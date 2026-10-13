package com.mavethedave.herdstead;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Comparator;

public class ZoneStorage extends SavedData {
    // ----- zone size and population scaling -----
    private static final int ZONE_SAFETY_MAX_CHUNKS = 4096;
    // Base populations in AnimalRegistry are for a zone of this many chunks; bigger or smaller zones scale linearly.
    private static final int CHUNKS_PER_BASE_POPULATION = HerdsteadConfig.get().chunksPerBasePopulation;

    // ----- seeding (herd sites) -----
    private static final float SEED_FRACTION_MIN = 0.25f;
    private static final float SEED_FRACTION_MAX = 0.60f;
    // A species with a max population BELOW this has a chance to be absent from a new zone.
    private static final int ABSENT_CHANCE_BELOW_MAX_POP = 5;
    private static final float ABSENT_CHANCE = 0.10f;
    // Herd size = max population * this ratio * a random 0.7-1.3 factor, clamped to the species' min/max.
    private static final float HERD_SIZE_RATIO = HerdsteadConfig.get().herdSizeRatio;
    private static final int HERD_SPREAD = 8;
    private static final int MAX_HERDS_PER_PASS = 2;
    private static final int SITE_FAIL_LIMIT = 20;
    private static final int MAX_SITE_RELOCATIONS = 5;

    // ----- placement rules -----
    private static final double MIN_PLAYER_DISTANCE = 24.0;
    // Seeded herds stay this many chunks from the zone (biome) edge: 3 chunks = 48 blocks.
    // Zones too small for it fall back to 2, 1, then 0.
    private static final int PREFERRED_EDGE_BUFFER_CHUNKS = 3;

    // ----- recruitment -----
    private static final int RECRUIT_RADIUS = 6;
    private static final int RECRUIT_ANCHOR_ATTEMPTS = 5;
    private static final int RECRUIT_RETRY_TICKS = 600;
    private static final int MAX_DEPLETION = 20;
    private static final int MAX_RECRUIT_ROLLS = HerdsteadConfig.get().maxRecruitRolls;
    private static final int RECRUITMENT_INTERVAL_TICKS = HerdsteadConfig.get().recruitmentIntervalTicks;

    private static final Random RANDOM = new Random();

    public List<Zone> zones = new ArrayList<>();

    // Lookup caches. Not saved; rebuilt automatically whenever the zone list changes.
    private final Map<Long, Zone> chunkIndex = new HashMap<>();
    private final Map<String, Zone> tagIndex = new HashMap<>();
    private int indexedZoneCount = -1;

    public ZoneStorage() {
    }

    public ZoneStorage(List<Zone> zones) {
        this.zones = new ArrayList<>(zones);
    }

    private static final Codec<ZoneStorage> CODEC = Zone.CODEC.listOf().xmap(
            ZoneStorage::new,
            zoneStorage -> zoneStorage.zones
    );

    private static final SavedDataType<ZoneStorage> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(HerdStead.MOD_ID, "zones"),
            ZoneStorage::new,
            CODEC,
            null
    );

    public static ZoneStorage get(MinecraftServer server) {
        ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
        if (level == null) {
            return new ZoneStorage();
        }
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ---------- lookup index ----------

    private void ensureIndex() {
        if (indexedZoneCount == zones.size()) {
            return;
        }
        chunkIndex.clear();
        tagIndex.clear();
        for (Zone zone : zones) {
            indexZone(zone);
        }
        indexedZoneCount = zones.size();
    }

    private void indexZone(Zone zone) {
        tagIndex.put(zone.zoneTag(), zone);
        for (long key : zone.chunks) {
            chunkIndex.put(key, zone);
        }
    }

    public void addZone(Zone zone) {
        ensureIndex();
        zones.add(zone);
        indexZone(zone);
        indexedZoneCount = zones.size();
        setDirty();
    }

    private boolean isChunkClaimed(long chunkKey) {
        ensureIndex();
        return chunkIndex.containsKey(chunkKey);
    }

    private static String zoneTagOf(Animal animal) {
        for (String tag : animal.entityTags()) {
            if (tag.startsWith(Zone.TAG_PREFIX)) {
                return tag;
            }
        }
        return null;
    }

    // ---------- zone creation ----------

    private static final long GROW_BUDGET_NANOS = 8_000_000L; // at most 8 ms of flood-fill per tick

    // A zone that is still being grown. In memory only; never saved.
    private static class GrowJob {
        final Zone zone;
        final Holder<Biome> originBiome;
        final Set<Long> visited = new HashSet<>();
        final Deque<int[]> frontier = new ArrayDeque<>();
        final long startNanos = System.nanoTime();
        int ticks = 0;

        GrowJob(Zone zone, Holder<Biome> originBiome) {
            this.zone = zone;
            this.originBiome = originBiome;
        }
    }

    private GrowJob growJob = null;

    // Called every 200 ticks per player: starts growing a zone if the player is standing in unclaimed land.
    public void checkAndCreateZoneForPlayer(ServerPlayer player, ServerLevel level) {
        if (growJob != null) {
            return; // one zone at a time; the player's chunk is rechecked next time
        }

        int playerChunkX = player.blockPosition().getX() >> 4;
        int playerChunkZ = player.blockPosition().getZ() >> 4;
        long playerChunkKey = new ChunkPos(playerChunkX, playerChunkZ).pack();

        if (isChunkClaimed(playerChunkKey)) {
            return;
        }

        Zone newZone = new Zone("Zone (" + playerChunkX + "," + playerChunkZ + ")", playerChunkX, playerChunkZ);
        Holder<Biome> originBiome = sampleChunkBiome(level, playerChunkX, playerChunkZ);

        GrowJob job = new GrowJob(newZone, originBiome);
        job.frontier.add(new int[]{playerChunkX, playerChunkZ});
        job.visited.add(playerChunkKey);
        growJob = job;
    }

    // Called every tick: advances the zone being grown, if any, within the time budget.
    public void stepZoneGrowth(ServerLevel level) {
        GrowJob job = growJob;
        if (job == null) {
            return;
        }
        job.ticks++;
        Zone zone = job.zone;
        long deadline = System.nanoTime() + GROW_BUDGET_NANOS;

        while (!job.frontier.isEmpty() && zone.chunks.size() < ZONE_SAFETY_MAX_CHUNKS) {
            if (System.nanoTime() >= deadline) {
                return; // out of time this tick; carry on next tick
            }

            int[] current = job.frontier.poll();
            int chunkX = current[0];
            int chunkZ = current[1];
            long chunkKey = new ChunkPos(chunkX, chunkZ).pack();

            if (isChunkClaimed(chunkKey)) {
                continue;
            }

            if (sampleChunkBiome(level, chunkX, chunkZ) != job.originBiome) {
                continue;
            }

            zone.chunks.add(chunkKey);

            int[][] neighbors = {
                    {chunkX + 1, chunkZ},
                    {chunkX - 1, chunkZ},
                    {chunkX, chunkZ + 1},
                    {chunkX, chunkZ - 1}
            };
            for (int[] neighbor : neighbors) {
                long neighborKey = new ChunkPos(neighbor[0], neighbor[1]).pack();
                if (job.visited.add(neighborKey)) {
                    job.frontier.add(neighbor);
                }
            }
        }

        finishZone(job, level);
    }

    private void finishZone(GrowJob job, ServerLevel level) {
        growJob = null;
        Zone newZone = job.zone;

        if (newZone.chunks.size() >= ZONE_SAFETY_MAX_CHUNKS) {
            HerdStead.LOGGER.info("Zone at chunk (" + newZone.originChunkX + "," + newZone.originChunkZ
                    + ") hit the safety ceiling of " + ZONE_SAFETY_MAX_CHUNKS
                    + " chunks; the rest of this biome will become a separate zone.");
        }

        chooseEdgeBuffer(newZone);
        newZone.tablesHash = tablesFingerprint();

        for (AnimalSpecies species : AnimalRegistry.SPECIES.values()) {
            int maxPop = calculateMaxPopulation(newZone, level, species);
            newZone.populations.put(species.key, new AnimalPopulation(maxPop));
        }
        int herdsPlanned = planHerdSites(newZone);

        addZone(newZone);
        long totalMillis = (System.nanoTime() - job.startNanos) / 1_000_000;
        HerdStead.LOGGER.info("Created " + newZone.name + " with " + newZone.chunks.size()
                + " chunks (grown over " + job.ticks + " ticks, " + totalMillis + " ms), edge buffer "
                + (newZone.edgeBufferChunks * 16) + " blocks, " + herdsPlanned + " herd sites planned.");
    }

    public int calculateMaxPopulation(Zone zone, ServerLevel level, AnimalSpecies species) {
        if (!HerdsteadConfig.get().isEnabled(species.key)) {
            return 0;
        }
        Holder<Biome> biome = sampleChunkBiome(level, zone.originChunkX, zone.originChunkZ);
        String biomeId = biome.unwrapKey().map(key -> key.identifier().toString()).orElse("");
        int baseValue = species.biomePopulations.getOrDefault(biomeId, 0);
        if (baseValue <= 0) {
            return 0;
        }

        int scaled = Math.round(baseValue * (zone.chunks.size() / (float) CHUNKS_PER_BASE_POPULATION));
        return Math.max(2, scaled);
    }

    // ---------- edge buffer and interior chunks ----------

    // Picks the biggest edge buffer (in chunks) that still leaves at least one usable interior chunk.
    private void chooseEdgeBuffer(Zone zone) {
        Set<Long> own = new HashSet<>(zone.chunks);
        for (int radius = PREFERRED_EDGE_BUFFER_CHUNKS; radius > 0; radius--) {
            for (long key : zone.chunks) {
                if (isInterior(own, key, radius)) {
                    zone.edgeBufferChunks = radius;
                    zone.interiorChunksCache = null;
                    return;
                }
            }
        }
        zone.edgeBufferChunks = 0;
        zone.interiorChunksCache = null;
    }

    // A chunk is interior when every chunk within `radius` of it (a square) belongs to the zone.
    private static boolean isInterior(Set<Long> own, long key, int radius) {
        int cx = ChunkPos.getX(key);
        int cz = ChunkPos.getZ(key);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!own.contains(new ChunkPos(cx + dx, cz + dz).pack())) {
                    return false;
                }
            }
        }
        return true;
    }

    private List<Long> interiorChunks(Zone zone) {
        if (zone.interiorChunksCache != null) {
            return zone.interiorChunksCache;
        }
        Set<Long> own = new HashSet<>(zone.chunks);
        List<Long> result = new ArrayList<>();
        for (long key : zone.chunks) {
            if (isInterior(own, key, zone.edgeBufferChunks)) {
                result.add(key);
            }
        }
        zone.interiorChunksCache = result;
        return result;
    }

    // True if the block position is at least bufferBlocks from any chunk that isn't part of this zone.
    // Only looks up the zone index; it never loads chunks. Needs the zone to be added already.
    private boolean isFarFromZoneEdge(Zone zone, int x, int z, int bufferBlocks) {
        ensureIndex();
        int minCx = (x - bufferBlocks) >> 4;
        int maxCx = (x + bufferBlocks) >> 4;
        int minCz = (z - bufferBlocks) >> 4;
        int maxCz = (z + bufferBlocks) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (chunkIndex.get(new ChunkPos(cx, cz).pack()) != zone) {
                    return false;
                }
            }
        }
        return true;
    }

    // ---------- seeding: plan herd sites at zone creation ----------

    private int planHerdSites(Zone zone) {
        List<Long> interior = interiorChunks(zone);
        if (interior.isEmpty()) {
            return 0;
        }

        // Each site gets a random spawn score; the list is sorted by it at the end.
        Map<HerdSite, Double> order = new HashMap<>();
        int planned = 0;
        for (Map.Entry<String, AnimalPopulation> entry : zone.populations.entrySet()) {
            AnimalSpecies species = AnimalRegistry.SPECIES.get(entry.getKey());
            if (species == null) {
                continue;
            }
            planned += planSitesForSpecies(zone, species, entry.getValue().maxPopulation, interior, order);
        }

        zone.sites.sort(Comparator.comparingDouble(order::get));
        return planned;
    }

    // Plans herd sites for ONE species and returns how many were added.
    // If `order` is not null, each new site's random spawn score is recorded in it.
    private int planSitesForSpecies(Zone zone, AnimalSpecies species, int maxPop,
                                    List<Long> interior, Map<HerdSite, Double> order) {
        if (maxPop <= 0 || interior.isEmpty()) {
            return 0;
        }

        // Rare species (small max population) sometimes just aren't in a zone.
        if (maxPop < ABSENT_CHANCE_BELOW_MAX_POP && RANDOM.nextFloat() < ABSENT_CHANCE) {
            return 0;
        }

        float fraction = SEED_FRACTION_MIN + RANDOM.nextFloat() * (SEED_FRACTION_MAX - SEED_FRACTION_MIN);
        int budget = Math.min(maxPop, Math.max(2, Math.round(maxPop * fraction)));
        int herdMin = SpeciesRules.herdMin(species.key);
        int herdMax = SpeciesRules.herdMax(species.key);

        int planned = 0;
        while (budget > 0) {
            float variation = 0.7f + RANDOM.nextFloat() * 0.6f;
            int size = Math.round(maxPop * HERD_SIZE_RATIO * variation);
            size = Math.max(herdMin, Math.min(herdMax, size));
            size = Math.min(size, budget);
            if (budget - size < herdMin) {
                size = budget; // don't leave a tiny leftover herd
            }

            long chunk = interior.get(RANDOM.nextInt(interior.size()));
            HerdSite site = new HerdSite(species.key, chunk, size);
            zone.sites.add(site);
            if (order != null) {
                order.put(site, spawnScore(species.key));
            }
            budget -= size;
            planned++;
        }
        return planned;
    }

    // Shrinks or removes pending herd sites for one species so present + pending never exceeds the maximum.
    // Animals already in the world are never touched. Returns how many animals were trimmed from pending herds.
    private int trimPendingToMax(Zone zone, String speciesKey) {
        AnimalPopulation pop = zone.populations.get(speciesKey);
        if (pop == null) {
            return 0;
        }

        int pending = 0;
        for (HerdSite site : zone.sites) {
            if (site.speciesKey.equals(speciesKey)) {
                pending += site.size;
            }
        }

        int over = pending + pop.currentPopulation - pop.maxPopulation;
        if (over <= 0) {
            return 0;
        }

        int herdMin = SpeciesRules.herdMin(speciesKey);
        int trimmed = 0;
        for (int i = zone.sites.size() - 1; i >= 0 && over > 0; i--) {
            HerdSite site = zone.sites.get(i);
            if (!site.speciesKey.equals(speciesKey)) {
                continue;
            }

            int remaining = site.size - over;
            if (remaining > 0 && remaining >= herdMin) {
                // Shrink this herd by exactly what's left to trim. Same site object, so its retry state is kept.
                site.size = remaining;
                trimmed += over;
                over = 0;
            } else {
                // Shrinking would leave a tiny herd: drop it entirely.
                zone.sites.remove(i);
                trimmed += site.size;
                over -= site.size;
            }
        }

        if (trimmed > 0) {
            setDirty();
        }
        return trimmed;
    }

    // Lower score spawns sooner. Produce (tier 0) always lands in 0-0.9, ahead of everything else.
    // Tiers 1 and 2 overlap a little, so their order differs from zone to zone.
    private double spawnScore(String speciesKey) {
        int tier = SpeciesRules.tier(speciesKey);
        double spread = (tier == 0) ? 0.9 : 1.5;
        return tier + RANDOM.nextDouble() * spread;
    }

    // ---------- spawning pending herd sites (called every 10 ticks) ----------

    public void processPendingSites(ServerLevel level) {
        ensureIndex();
        int budget = MAX_HERDS_PER_PASS;

        for (Zone zone : zones) {
            if (zone.sites.isEmpty()) {
                continue;
            }

            // Trim first, while nothing is iterating zone.sites.
            for (String key : new ArrayList<>(zone.populations.keySet())) {
                trimPendingToMax(zone, key);
            }
            if (zone.sites.isEmpty()) {
                continue;
            }

            Iterator<HerdSite> iterator = zone.sites.iterator();
            while (iterator.hasNext()) {
                HerdSite site = iterator.next();

                AnimalSpecies species = AnimalRegistry.SPECIES.get(site.speciesKey);
                if (species == null || !zone.populations.containsKey(site.speciesKey)) {
                    iterator.remove();
                    setDirty();
                    continue;
                }

                if (!isChunkReady(level, ChunkPos.getX(site.chunk), ChunkPos.getZ(site.chunk))) {
                    continue; // chunk not loaded yet: the site just waits
                }
                if (budget <= 0) {
                    return;
                }

                if (spawnHerdAtSite(zone, site, level, species)) {
                    budget--;
                    iterator.remove();
                    setDirty();
                } else if (++site.failedPasses >= SITE_FAIL_LIMIT) {
                    // This spot keeps failing (wrong ground, water, player standing there...). Move it, or give up.
                    site.failedPasses = 0;
                    site.relocations++;
                    List<Long> interior = interiorChunks(zone);
                    if (site.relocations > MAX_SITE_RELOCATIONS || interior.isEmpty()) {
                        iterator.remove();
                    } else {
                        site.chunk = interior.get(RANDOM.nextInt(interior.size()));
                    }
                    setDirty();
                }
            }
        }
    }

    private boolean spawnHerdAtSite(Zone zone, HerdSite site, ServerLevel level, AnimalSpecies species) {
        int edgeBufferBlocks = zone.edgeBufferChunks * 16;
        int chunkX = ChunkPos.getX(site.chunk);
        int chunkZ = ChunkPos.getZ(site.chunk);

        Animal leader = null;
        for (int attempt = 0; attempt < 8 && leader == null; attempt++) {
            int x = chunkX * 16 + RANDOM.nextInt(16);
            int z = chunkZ * 16 + RANDOM.nextInt(16);
            leader = spawnOne(level, zone, species, x, z, false, true, edgeBufferBlocks);
        }
        if (leader == null) {
            return false;
        }

        BlockPos centre = leader.blockPosition();
        for (int i = 1; i < site.size; i++) {
            for (int attempt = 0; attempt < 4; attempt++) {
                int x = centre.getX() + RANDOM.nextInt(2 * HERD_SPREAD + 1) - HERD_SPREAD;
                int z = centre.getZ() + RANDOM.nextInt(2 * HERD_SPREAD + 1) - HERD_SPREAD;
                if (spawnOne(level, zone, species, x, z, false, false, edgeBufferBlocks) != null) {
                    break;
                }
            }
        }
        return true;
    }

    private Animal spawnOne(ServerLevel level, Zone zone, AnimalSpecies species, int x, int z,
                            boolean baby, boolean leader, int edgeBufferBlocks) {
        if (!isFarFromZoneEdge(zone, x, z, edgeBufferBlocks)) {
            return null;
        }
        if (!isChunkReady(level, x >> 4, z >> 4)) {
            return null;
        }

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);

        if (!SpeciesRules.groundFor(species.key).contains(level.getBlockState(pos.below()).getBlock())) {
            return null;
        }
        if (isNearPlayer(level, x + 0.5, y, z + 0.5)) {
            return null;
        }

        Animal animal = species.entityType.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (animal == null) {
            return null;
        }
        animal.setBaby(baby);
        animal.setPos(x + 0.5, y, z + 0.5);
        if (!animal.checkSpawnObstruction(level)) {
            return null;
        }

        animal.addTag(zone.zoneTag());
        animal.setPersistenceRequired();
        if (leader) {
            animal.addTag("herd_leader");
        }
        level.addFreshEntity(animal);

        AnimalPopulation pop = zone.populations.get(species.key);
        if (pop != null) {
            pop.currentPopulation++;
        }
        setDirty();
        return animal;
    }

    private boolean isNearPlayer(ServerLevel level, double x, double y, double z) {
        double minSqr = MIN_PLAYER_DISTANCE * MIN_PLAYER_DISTANCE;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) < minSqr) {
                return true;
            }
        }
        return false;
    }

    private static boolean isChunkReady(ServerLevel level, int chunkX, int chunkZ) {
        return level.getChunkSource().getChunkNow(chunkX, chunkZ) != null;
    }

    private List<Long> loadedChunks(Zone zone, ServerLevel level) {
        List<Long> loaded = new ArrayList<>();
        for (long key : zone.chunks) {
            if (isChunkReady(level, ChunkPos.getX(key), ChunkPos.getZ(key))) {
                loaded.add(key);
            }
        }
        return loaded;
    }

    // ---------- migration: bring old zones up to date with the current tables ----------

    // A fingerprint of everything that decides maximum populations. If it changes, saved zones are migrated.
    private static int tablesFingerprint() {
        int hash = HerdsteadConfig.get().fingerprint();
        for (AnimalSpecies species : AnimalRegistry.SPECIES.values()) {
            hash = 31 * hash + species.key.hashCode();
            hash = 31 * hash + species.biomePopulations.hashCode();
        }
        return hash == 0 ? 1 : hash; // 0 is reserved for "never migrated"
    }

    public void migrateZones(ServerLevel level) {
        int fingerprint = tablesFingerprint();
        int migrated = 0;

        for (Zone zone : zones) {
            if (zone.tablesHash == fingerprint) {
                continue;
            }

            List<Long> interior = interiorChunks(zone);
            Map<String, Integer> pending = zone.pendingBySpecies();
            int speciesAdded = 0;
            int sitesPlanned = 0;
            int animalsTrimmed = 0;

            for (AnimalSpecies species : AnimalRegistry.SPECIES.values()) {
                int newMax = calculateMaxPopulation(zone, level, species);
                AnimalPopulation pop = zone.populations.get(species.key);

                if (pop == null) {
                    zone.populations.put(species.key, new AnimalPopulation(newMax));
                    speciesAdded++;
                    sitesPlanned += planSitesForSpecies(zone, species, newMax, interior, null);
                } else {
                    boolean wasZero = pop.maxPopulation <= 0;
                    boolean empty = pop.currentPopulation == 0 && pending.getOrDefault(species.key, 0) == 0;
                    pop.maxPopulation = newMax;
                    if (wasZero && empty) {
                        sitesPlanned += planSitesForSpecies(zone, species, newMax, interior, null);
                    } else {
                        animalsTrimmed += trimPendingToMax(zone, species.key);
                    }
                }
            }

            zone.sites.sort(Comparator.comparingInt((HerdSite site) -> SpeciesRules.tier(site.speciesKey)));

            zone.tablesHash = fingerprint;
            migrated++;
            HerdStead.LOGGER.info("Migrated " + zone.name + ": " + speciesAdded + " species added, "
                    + sitesPlanned + " herd sites planned, " + animalsTrimmed + " pending animals trimmed.");
        }

        if (migrated > 0) {
            setDirty();
        }
    }

    // ---------- timers and recruitment ----------

    public void updateDepletionAndRecovery() {
        for (Zone zone : zones) {
            Map<String, Integer> pending = zone.pendingBySpecies();
            for (Map.Entry<String, AnimalPopulation> entry : zone.populations.entrySet()) {
                AnimalPopulation pop = entry.getValue();
                int pendingCount = pending.getOrDefault(entry.getKey(), 0);

                if (pop.currentPopulation + pendingCount >= pop.maxPopulation) {
                    pop.recoveryTimer = 0;
                } else {
                    pop.recoveryTimer += 20;
                }

                pop.depletionDecayTimer += 20;
                if (pop.depletionDecayTimer >= pop.depletionDecayTarget) {
                    if (pop.depletion > 0) {
                        pop.depletion--;
                    }
                    pop.depletionDecayTarget = AnimalPopulation.rollDecayTarget();
                    pop.depletionDecayTimer = 0;
                }
            }
        }
        if (!zones.isEmpty()) {
            setDirty();
        }
    }

    public void attemptRecruitment(ServerLevel level) {
        ensureIndex();
        for (Zone zone : zones) {
            List<Long> loaded = null;
            Map<String, Integer> pending = null;

            for (Map.Entry<String, AnimalPopulation> entry : zone.populations.entrySet()) {
                AnimalSpecies species = AnimalRegistry.SPECIES.get(entry.getKey());
                AnimalPopulation pop = entry.getValue();
                if (species == null || pop.recoveryTimer < RECRUITMENT_INTERVAL_TICKS) {
                    continue;
                }

                if (loaded == null) {
                    loaded = loadedChunks(zone, level);
                    pending = zone.pendingBySpecies();
                }
                if (loaded.isEmpty()) {
                    break;
                }

                int rolls = Math.max(1, Math.min(MAX_RECRUIT_ROLLS,
                        Math.round(loaded.size() / (float) CHUNKS_PER_BASE_POPULATION)));
                int pendingCount = pending.getOrDefault(entry.getKey(), 0);
                int recruitedCount = 0;
                boolean noSpot = false;

                for (int i = 0; i < rolls; i++) {
                    if (pop.currentPopulation + pendingCount >= pop.maxPopulation) {
                        break;
                    }
                    if (!rollRecruitSuccess(pop.depletion)) {
                        continue;
                    }

                    if (recruitBabyNearHerd(zone, level, species, loaded)) {
                        recruitedCount++;
                        if (pop.depletion > 0) {
                            pop.depletion--;
                        }
                    } else if (pop.currentPopulation == 0 && pendingCount == 0 && pop.depletion > 0
                            && foundNewHerd(zone, species, pop)) {
                        // Wiped out here: a small founding herd is the whole response this interval.
                        recruitedCount++;
                        pop.depletion--;
                        break;
                    } else {
                        noSpot = true;
                        break;
                    }
                }

                if (recruitedCount == 0 && noSpot) {
                    // Rolled a success but found no valid spot: retry soon instead of waiting a full interval.
                    pop.recoveryTimer = Math.max(0, RECRUITMENT_INTERVAL_TICKS - RECRUIT_RETRY_TICKS);
                } else {
                    pop.recoveryTimer = 0;
                }
                setDirty();
            }
        }
    }

    private boolean rollRecruitSuccess(int depletion) {
        int roll = RANDOM.nextInt(100) + 1;
        if (depletion <= 2) return roll <= 50;
        if (depletion <= 5) return roll <= 35;
        if (depletion <= 9) return roll <= 20;
        if (depletion <= 14) return roll <= 5;
        return roll <= 2;
    }

    // Spawns one baby within RECRUIT_RADIUS of a wild adult of this species in this zone.
    @SuppressWarnings("unchecked")
    private <T extends Animal> boolean recruitBabyNearHerd(Zone zone, ServerLevel level,
                                                           AnimalSpecies species, List<Long> loaded) {
        int minCx = Integer.MAX_VALUE;
        int maxCx = Integer.MIN_VALUE;
        int minCz = Integer.MAX_VALUE;
        int maxCz = Integer.MIN_VALUE;
        for (long key : loaded) {
            minCx = Math.min(minCx, ChunkPos.getX(key));
            maxCx = Math.max(maxCx, ChunkPos.getX(key));
            minCz = Math.min(minCz, ChunkPos.getZ(key));
            maxCz = Math.max(maxCz, ChunkPos.getZ(key));
        }
        AABB box = new AABB(minCx * 16.0, level.getMinY(), minCz * 16.0,
                (maxCx + 1) * 16.0, level.getMaxY(), (maxCz + 1) * 16.0);

        Class<T> cls = (Class<T>) species.entityClass;
        List<T> adults = new ArrayList<>();
        List<T> leaders = new ArrayList<>();
        String zoneTag = zone.zoneTag();

        for (T candidate : level.getEntitiesOfClass(cls, box)) {
            if (candidate.getType() != species.entityType || candidate.isBaby() || !candidate.isAlive()) {
                continue;
            }
            if (candidate.entityTags().contains(species.domesticTag)) {
                continue;
            }
            if (!candidate.entityTags().contains(zoneTag)) {
                continue;
            }
            adults.add(candidate);
            if (candidate.entityTags().contains("herd_leader")) {
                leaders.add(candidate);
            }
        }

        if (adults.isEmpty()) {
            return false;
        }

        for (int attempt = 0; attempt < RECRUIT_ANCHOR_ATTEMPTS; attempt++) {
            List<T> pool = (!leaders.isEmpty() && RANDOM.nextFloat() < 0.6f) ? leaders : adults;
            T anchor = pool.get(RANDOM.nextInt(pool.size()));
            BlockPos anchorPos = anchor.blockPosition();

            for (int tries = 0; tries < 4; tries++) {
                int x = anchorPos.getX() + RANDOM.nextInt(2 * RECRUIT_RADIUS + 1) - RECRUIT_RADIUS;
                int z = anchorPos.getZ() + RANDOM.nextInt(2 * RECRUIT_RADIUS + 1) - RECRUIT_RADIUS;
                // Buffer 0: no biome-edge buffer for babies, but the spot must still be inside this zone.
                if (spawnOne(level, zone, species, x, z, true, false, 0) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean foundNewHerd(Zone zone, AnimalSpecies species, AnimalPopulation pop) {
        List<Long> interior = interiorChunks(zone);
        if (interior.isEmpty()) {
            return false;
        }
        int size = Math.max(2, SpeciesRules.herdMin(species.key));
        size = Math.min(size, Math.max(1, pop.maxPopulation));
        zone.sites.add(new HerdSite(species.key, interior.get(RANDOM.nextInt(interior.size())), size));
        return true;
    }

    // ---------- population events ----------

    public void onTrackedAnimalDied(Animal animal, boolean countsAsDepletion) {
        String tag = zoneTagOf(animal);
        if (tag == null) {
            return;
        }
        animal.removeTag(tag);

        ensureIndex();
        Zone zone = tagIndex.get(tag);
        AnimalSpecies species = AnimalRegistry.getForEntity(animal);
        if (zone == null || species == null) {
            return;
        }
        AnimalPopulation pop = zone.populations.get(species.key);
        if (pop == null) {
            return;
        }

        pop.currentPopulation = Math.max(0, pop.currentPopulation - 1);
        if (countsAsDepletion) {
            pop.depletion = Math.min(MAX_DEPLETION, pop.depletion + 1);
        }
        setDirty();
    }

    // Domestication takes the animal out of the wild population but does NOT add depletion.
    public void onAnimalDomesticated(Animal animal) {
        String tag = zoneTagOf(animal);
        if (tag == null) {
            return;
        }
        animal.removeTag(tag);

        ensureIndex();
        Zone zone = tagIndex.get(tag);
        AnimalSpecies species = AnimalRegistry.getForEntity(animal);
        if (zone == null || species == null) {
            return;
        }
        AnimalPopulation pop = zone.populations.get(species.key);
        if (pop == null) {
            return;
        }

        pop.currentPopulation = Math.max(0, pop.currentPopulation - 1);
        setDirty();
    }

    // ---------- reconciliation: transfers and adoption (never a recount) ----------

    public void reconcileLoadedAnimals(ServerLevel level) {
        ensureIndex();
        for (AnimalSpecies species : AnimalRegistry.SPECIES.values()) {
            reconcileSpecies(species, level);
        }
    }

    private <T extends Animal> void reconcileSpecies(AnimalSpecies species, ServerLevel level) {
        List<T> animals = EntityScan.animalsNearPlayers(level, species);

        for (T animal : animals) {
            if (animal.entityTags().contains(species.domesticTag)) {
                continue;
            }

            if (animal.entityTags().contains(HerdManager.BLOCKED_TAG)) {
                continue;
            }

            long chunkKey = new ChunkPos(animal.blockPosition().getX() >> 4, animal.blockPosition().getZ() >> 4).pack();
            Zone actualZone = chunkIndex.get(chunkKey);
            if (actualZone == null) {
                continue; // unclaimed land: stays attributed to its home zone
            }

            AnimalPopulation newPop = actualZone.populations.get(species.key);
            if (newPop == null) {
                continue;
            }

            String actualTag = actualZone.zoneTag();
            String currentTag = zoneTagOf(animal);
            if (actualTag.equals(currentTag)) {
                continue;
            }

            if (currentTag != null) {
                Zone oldZone = tagIndex.get(currentTag);
                if (oldZone != null) {
                    AnimalPopulation oldPop = oldZone.populations.get(species.key);
                    if (oldPop != null) {
                        oldPop.currentPopulation = Math.max(0, oldPop.currentPopulation - 1);
                    }
                }
                animal.removeTag(currentTag);
            }

            animal.addTag(actualTag);
            newPop.currentPopulation++;
            setDirty();
        }
    }

    private static final Set<String> PASS_THROUGH_BIOMES = Set.of("streamsreflowing:stream");
    private static final Set<String> CAVE_BIOMES = Set.of(
            "minecraft:lush_caves", "minecraft:dripstone_caves", "minecraft:deep_dark");
    private static final int[][] SAMPLE_OFFSETS = {{8, 8}, {2, 2}, {14, 2}, {2, 14}, {14, 14}};
    private static final int[] SURFACE_PROBE_HEIGHTS_ABOVE_SEA = {0, 40, 80, 140};

    private static String biomeId(Holder<Biome> biome) {
        return biome.unwrapKey().map(k -> k.identifier().toString()).orElse("");
    }

    // The biome on the surface at x/z: climbs past cave biomes. Never loads chunks.
    private static Holder<Biome> surfaceBiomeAt(ServerLevel level, int x, int z) {
        Holder<Biome> biome = null;
        for (int dy : SURFACE_PROBE_HEIGHTS_ABOVE_SEA) {
            biome = level.getBiome(new BlockPos(x, level.getSeaLevel() + dy, z));
            if (!CAVE_BIOMES.contains(biomeId(biome))) {
                return biome;
            }
        }
        return biome;
    }

    // The most common surface biome among 5 points in the chunk, so a thin stream crossing it doesn't decide.
    static Holder<Biome> sampleChunkBiome(ServerLevel level, int chunkX, int chunkZ) {
        Map<Holder<Biome>, Integer> counts = new HashMap<>();
        Holder<Biome> best = null;
        int bestCount = 0;
        for (int[] o : SAMPLE_OFFSETS) {
            Holder<Biome> biome = surfaceBiomeAt(level, chunkX * 16 + o[0], chunkZ * 16 + o[1]);
            if (PASS_THROUGH_BIOMES.contains(biomeId(biome))) {
                continue;
            }
            int count = counts.merge(biome, 1, Integer::sum);
            if (count > bestCount) {
                best = biome;
                bestCount = count;
            }
        }
        if (best == null) {
            best = surfaceBiomeAt(level, chunkX * 16 + 8, chunkZ * 16 + 8);
        }
        return best;
    }

}