package com.mavethedave.herdstead;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class HerdsteadConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static HerdsteadConfig instance;

    // Field names are exactly what appears in the JSON file.
    public int chunksPerBasePopulation = 256;
    public int maxRecruitRolls = 40;
    public int recruitmentIntervalTicks = 24000;
    public float herdSizeRatio = 0.15f;
    public boolean debugLogging = false;
    public Map<String, Boolean> species = new LinkedHashMap<>();

    // Loads on first use, so it doesn't matter which class asks first.
    public static synchronized HerdsteadConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static HerdsteadConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("herdstead.json");
        HerdsteadConfig config = new HerdsteadConfig();
        boolean safeToWrite = true;

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                HerdsteadConfig loaded = GSON.fromJson(reader, HerdsteadConfig.class);
                if (loaded != null) {
                    config = loaded;
                }
            } catch (Exception e) {
                // A typo in the file must never cause it to be overwritten with defaults.
                safeToWrite = false;
                HerdStead.LOGGER.warn("Could not read herdstead.json (" + e.getMessage()
                        + "). Using defaults this run and leaving the file untouched.");
            }
        }

        config.sanitize();
        if (safeToWrite) {
            config.save(path);
        }
        return config;
    }

    private void sanitize() {
        chunksPerBasePopulation = Math.max(16, chunksPerBasePopulation);
        maxRecruitRolls = Math.max(1, maxRecruitRolls);
        recruitmentIntervalTicks = Math.max(20, recruitmentIntervalTicks);
        herdSizeRatio = Math.max(0.02f, Math.min(1.0f, herdSizeRatio));

        Map<String, Boolean> saved = species == null ? new LinkedHashMap<>() : species;

        // Every registered species gets an entry, so the file always lists them all.
        Map<String, Boolean> complete = new LinkedHashMap<>();
        for (String key : AnimalRegistry.SPECIES.keySet()) {
            String name = shortName(key);
            Boolean value = saved.get(name);
            complete.put(name, value == null || value);
        }
        for (String name : saved.keySet()) {
            if (!complete.containsKey(name)) {
                HerdStead.LOGGER.warn("herdstead.json: unknown species '" + name + "' ignored.");
            }
        }
        species = complete;
    }

    private void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            HerdStead.LOGGER.warn("Could not write herdstead.json: " + e.getMessage());
        }
    }

    private static String shortName(String speciesKey) {
        return speciesKey.substring(speciesKey.indexOf(':') + 1);
    }

    public boolean isEnabled(String speciesKey) {
        Boolean enabled = species.get(shortName(speciesKey));
        return enabled == null || enabled;
    }

    // Only the settings that change saved maximums feed the migration fingerprint.
    public int fingerprint() {
        int hash = chunksPerBasePopulation;
        for (Map.Entry<String, Boolean> entry : new TreeMap<>(species).entrySet()) {
            if (!entry.getValue()) {
                hash = 31 * hash + entry.getKey().hashCode();
            }
        }
        return hash;
    }
}