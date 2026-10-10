package com.mavethedave.herdstead;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

// Reads config/herdstead-species.json: per-species biome populations, ground blocks, herd sizes and spawn tier.
// The built-in values in AnimalRegistry and SpeciesRules are the defaults, and the file is written from them
// the first time. A broken file never crashes the game and is never overwritten, and neither is a file with
// a mistake in it (unknown block, bad number, unknown species): it is left exactly as the player wrote it,
// so the warnings repeat every launch until the mistake is fixed.
public final class SpeciesConfig {
    private SpeciesConfig() {
    }

    // Set by warn(). When true, the file is not rewritten this run.
    private static boolean problemFound = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type FILE_TYPE = new TypeToken<LinkedHashMap<String, Entry>>() {
    }.getType();

    // Field names are exactly what appears in the JSON file.
    private static class Entry {
        Integer herdMin;
        Integer herdMax;
        Integer tier;
        List<String> ground;
        Map<String, Integer> biomes;
    }

    // Takes the registry as a parameter (rather than reading AnimalRegistry.SPECIES) because it runs
    // while AnimalRegistry is still being set up.
    public static void load(Map<String, AnimalSpecies> registry) {
        try {
            loadInternal(registry);
        } catch (Exception e) {
            HerdStead.LOGGER.warn("herdstead-species.json: unexpected problem (" + e
                    + "). Built-in species settings are used this run.");
        }
    }

    private static void loadInternal(Map<String, AnimalSpecies> registry) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("herdstead-species.json");
        Map<String, Entry> saved = new LinkedHashMap<>();
        boolean safeToWrite = true;
        problemFound = false;

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                Map<String, Entry> loaded = GSON.fromJson(reader, FILE_TYPE);
                if (loaded != null) {
                    for (Map.Entry<String, Entry> item : loaded.entrySet()) {
                        if (item.getKey() != null && item.getValue() != null) {
                            saved.put(speciesName(item.getKey()), item.getValue());
                        }
                    }
                }
            } catch (Exception e) {
                // A typo in the file must never cause it to be overwritten with defaults.
                safeToWrite = false;
                HerdStead.LOGGER.warn("Could not read herdstead-species.json (" + e.getMessage()
                        + "). Using built-in species settings this run and leaving the file untouched.");
            }
        }

        // Every registered species gets an entry, so the file always lists them all.
        Map<String, Entry> complete = new LinkedHashMap<>();
        for (AnimalSpecies species : registry.values()) {
            String name = speciesName(species.key);
            Entry in = saved.get(name);
            complete.put(name, resolve(species, name, in == null ? new Entry() : in));
        }
        for (String name : saved.keySet()) {
            if (!complete.containsKey(name)) {
                warn("unknown species '" + name + "' ignored.");
            }
        }

        if (safeToWrite && problemFound) {
            // Rewriting now would silently delete the mistake along with its warning.
            safeToWrite = false;
            HerdStead.LOGGER.warn("herdstead-species.json has problems (see above). Built-in values are used for"
                    + " those entries this run, and the file is left as you wrote it until they are fixed.");
        }

        if (safeToWrite) {
            save(path, complete);
        }
    }

    // Fills in anything missing from the built-in values, checks the rest, applies it to the game,
    // and returns the complete entry that gets written back to the file.
    private static Entry resolve(AnimalSpecies species, String name, Entry in) {
        String key = species.key;

        int herdMin = in.herdMin != null ? in.herdMin : SpeciesRules.herdMin(key);
        int herdMax = in.herdMax != null ? in.herdMax : SpeciesRules.herdMax(key);
        if (herdMin < 1) {
            warn(name + ": herdMin must be at least 1, using 1.");
            herdMin = 1;
        }
        if (herdMax < herdMin) {
            warn(name + ": herdMax is below herdMin, using " + herdMin + ".");
            herdMax = herdMin;
        }
        int tier = in.tier != null ? in.tier : SpeciesRules.tier(key);
        if (tier < 0) {
            warn(name + ": tier cannot be negative, using 0.");
            tier = 0;
        }

        Set<Block> ground = new LinkedHashSet<>();
        if (in.ground != null) {
            for (String id : in.ground) {
                if (id == null) {
                    continue;
                }
                Block block = findBlock(id);
                if (block == null) {
                    warn(name + ": unknown block '" + id + "' in ground list, ignored.");
                } else {
                    ground.add(block);
                }
            }
            if (ground.isEmpty()) {
                warn(name + ": no valid ground blocks listed, using the built-in list.");
            }
        }
        if (ground.isEmpty()) {
            ground.addAll(SpeciesRules.groundFor(key));
        }

        // Biome populations. Zero entries are dropped: a biome that isn't listed means none of this species.
        Map<String, Integer> applied = new LinkedHashMap<>(); // full ids, e.g. minecraft:plains
        if (in.biomes == null) {
            for (Map.Entry<String, Integer> item : species.biomePopulations.entrySet()) {
                if (item.getValue() != null && item.getValue() > 0) {
                    applied.put(item.getKey(), item.getValue());
                }
            }
        } else {
            for (Map.Entry<String, Integer> item : in.biomes.entrySet()) {
                Integer value = item.getValue();
                if (item.getKey() == null || value == null) {
                    continue;
                }
                if (value < 0) {
                    warn(name + ": negative population for '" + item.getKey() + "', ignored.");
                    continue;
                }
                if (value > 0) {
                    applied.put(fullId(item.getKey()), value);
                }
            }
        }

        Entry out = new Entry();
        out.herdMin = herdMin;
        out.herdMax = herdMax;
        out.tier = tier;
        out.ground = new ArrayList<>();
        for (Block block : ground) {
            out.ground.add(shortId(String.valueOf(BuiltInRegistries.BLOCK.getKey(block))));
        }
        Collections.sort(out.ground);
        out.biomes = new TreeMap<>();
        for (Map.Entry<String, Integer> item : applied.entrySet()) {
            out.biomes.put(shortId(item.getKey()), item.getValue());
        }

        species.biomePopulations.clear();
        species.biomePopulations.putAll(applied);
        SpeciesRules.set(key, herdMin, herdMax, ground, tier);
        return out;
    }

    private static Block findBlock(String id) {
        Identifier identifier = Identifier.tryParse(fullId(id));
        if (identifier == null) {
            return null;
        }
        return BuiltInRegistries.BLOCK.getOptional(identifier).orElse(null);
    }

    private static void save(Path path, Map<String, Entry> complete) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(complete, FILE_TYPE, writer);
            }
        } catch (IOException e) {
            HerdStead.LOGGER.warn("Could not write herdstead-species.json: " + e.getMessage());
        }
    }

    private static void warn(String message) {
        problemFound = true;
        HerdStead.LOGGER.warn("herdstead-species.json: " + message);
    }

    // "minecraft:cow" -> "cow"
    private static String speciesName(String key) {
        return key.substring(key.indexOf(':') + 1).trim().toLowerCase(Locale.ROOT);
    }

    // "plains" or "minecraft:plains" -> "minecraft:plains". Other namespaces (modded biomes) are kept as written.
    private static String fullId(String id) {
        String clean = id.trim().toLowerCase(Locale.ROOT);
        return clean.contains(":") ? clean : "minecraft:" + clean;
    }

    // "minecraft:plains" -> "plains"; other namespaces are kept in full.
    private static String shortId(String id) {
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }
}