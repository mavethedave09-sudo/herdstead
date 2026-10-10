package com.mavethedave.herdstead;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SpeciesRules {
    private SpeciesRules() {
    }

    public record Rule(int herdMin, int herdMax, Set<Block> ground) {
    }

    private static final Map<String, Rule> RULES = new HashMap<>();
    private static final Rule DEFAULT_RULE = new Rule(2, 5,
            blocks(Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT));

    private static Set<Block> blocks(Block... blocks) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(blocks)));
    }

    private static void rule(String key, int herdMin, int herdMax, Block... ground) {
        RULES.put(key, new Rule(herdMin, herdMax, blocks(ground)));
    }

    static {
        // Livestock: biggest herds.
        rule("minecraft:sheep", 4, 10, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.SNOW_BLOCK);
        rule("minecraft:cow", 3, 8, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.SNOW_BLOCK);
        rule("minecraft:chicken", 3, 8, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.SNOW_BLOCK);
        rule("minecraft:pig", 3, 7, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.SNOW_BLOCK);
        rule("minecraft:mooshroom", 3, 6, Blocks.MYCELIUM);

        // Grazers and others.
        rule("minecraft:horse", 3, 7, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.SNOW_BLOCK);
        rule("minecraft:donkey", 2, 4, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.SNOW_BLOCK);
        rule("minecraft:llama", 3, 6, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.SNOW_BLOCK);
        rule("minecraft:rabbit", 2, 5, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT,
                Blocks.SAND, Blocks.RED_SAND, Blocks.SNOW_BLOCK);
        rule("minecraft:panda", 1, 3, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT);
        rule("minecraft:goat", 2, 5, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.STONE, Blocks.SNOW_BLOCK,
                Blocks.PACKED_ICE, Blocks.GRAVEL);
        rule("minecraft:camel", 2, 5, Blocks.SAND, Blocks.RED_SAND, Blocks.COARSE_DIRT);
        rule("minecraft:armadillo", 1, 3, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.SAND, Blocks.RED_SAND,
                Blocks.TERRACOTTA, Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA,
                Blocks.YELLOW_TERRACOTTA, Blocks.BROWN_TERRACOTTA, Blocks.RED_TERRACOTTA,
                Blocks.LIGHT_GRAY_TERRACOTTA);
    }

    // Used by SpeciesConfig to replace the built-in rule and tier with the values from herdstead-species.json.
    public static void set(String key, int herdMin, int herdMax, Set<Block> ground, int tier) {
        RULES.put(key, new Rule(herdMin, herdMax, Collections.unmodifiableSet(new HashSet<>(ground))));
        TIERS.put(key, tier);
    }

    private static Rule of(String key) {
        return RULES.getOrDefault(key, DEFAULT_RULE);
    }

    public static int herdMin(String key) {
        return of(key).herdMin();
    }

    public static int herdMax(String key) {
        return of(key).herdMax();
    }

    public static Set<Block> groundFor(String key) {
        return of(key).ground();
    }

    // Spawn order for pending herd sites: lower tier spawns first.
    // 0 = produce animals, 1 = common grazers (the default), 2 = specialist species.
    private static final Map<String, Integer> TIERS = new HashMap<>();

    static {
        for (String key : new String[]{"minecraft:cow", "minecraft:sheep", "minecraft:pig", "minecraft:chicken"}) {
            TIERS.put(key, 0);
        }
        for (String key : new String[]{"minecraft:mooshroom", "minecraft:panda", "minecraft:camel", "minecraft:armadillo"}) {
            TIERS.put(key, 2);
        }
    }

    public static int tier(String key) {
        return TIERS.getOrDefault(key, 1);
    }

    // Which predators each species runs away from. Names: "wolf", "fox", "ocelot".
    private static final Map<String, Set<String>> FLEES = Map.of(
            "minecraft:sheep", Set.of("wolf"),
            "minecraft:chicken", Set.of("fox", "ocelot"),
            "minecraft:rabbit", Set.of("fox")
    );

    public static Set<String> predatorsOf(String key) {
        return FLEES.getOrDefault(key, Set.of());
    }
}