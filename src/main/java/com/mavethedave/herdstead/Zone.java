package com.mavethedave.herdstead;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Zone {
    public static final String TAG_PREFIX = "herdstead_zone_";

    public String name;
    public int originChunkX;
    public int originChunkZ;
    public List<Long> chunks;
    public Map<String, AnimalPopulation> populations;
    public List<HerdSite> sites;
    public int edgeBufferChunks;
    public int tablesHash;

    public List<Long> interiorChunksCache = null;

    public Zone(String name, int originChunkX, int originChunkZ) {
        this.name = name;
        this.originChunkX = originChunkX;
        this.originChunkZ = originChunkZ;
        this.chunks = new ArrayList<>();
        this.populations = new HashMap<>();
        this.sites = new ArrayList<>();
        this.edgeBufferChunks = 0;
        this.tablesHash = 0;
    }

    // Parameter order MUST match the Codec field order below.
    public Zone(String name, int originChunkX, int originChunkZ, List<Long> chunks,
                Map<String, AnimalPopulation> populations, List<HerdSite> sites,
                int edgeBufferChunks, int tablesHash) {
        this.name = name;
        this.originChunkX = originChunkX;
        this.originChunkZ = originChunkZ;
        this.chunks = new ArrayList<>(chunks);
        this.populations = new HashMap<>(populations);
        this.sites = new ArrayList<>(sites);
        this.edgeBufferChunks = edgeBufferChunks;
        this.tablesHash = tablesHash;
    }

    public String zoneTag() {
        return TAG_PREFIX + originChunkX + "_" + originChunkZ;
    }

    // Animals planned but not yet spawned, per species key.
    public Map<String, Integer> pendingBySpecies() {
        Map<String, Integer> result = new HashMap<>();
        for (HerdSite site : sites) {
            result.merge(site.speciesKey, site.size, Integer::sum);
        }
        return result;
    }

    public static final Codec<Zone> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(zone -> zone.name),
            Codec.INT.fieldOf("originChunkX").forGetter(zone -> zone.originChunkX),
            Codec.INT.fieldOf("originChunkZ").forGetter(zone -> zone.originChunkZ),
            Codec.LONG.listOf().fieldOf("chunks").forGetter(zone -> zone.chunks),
            Codec.unboundedMap(Codec.STRING, AnimalPopulation.CODEC).fieldOf("populations").forGetter(zone -> zone.populations),
            HerdSite.CODEC.listOf().fieldOf("sites").forGetter(zone -> zone.sites),
            Codec.INT.fieldOf("edgeBufferChunks").forGetter(zone -> zone.edgeBufferChunks),
            Codec.INT.optionalFieldOf("tablesHash", 0).forGetter(zone -> zone.tablesHash)
    ).apply(instance, Zone::new));
}
