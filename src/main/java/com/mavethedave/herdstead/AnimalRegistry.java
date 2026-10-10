package com.mavethedave.herdstead;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnimalRegistry {
    public static final Map<String, AnimalSpecies> SPECIES = new LinkedHashMap<>();

    // Must stay a mutable map: SpeciesConfig replaces its contents with the values from herdstead-species.json.
    private static Map<String, Integer> biomeMap(Object... pairs) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put("minecraft:" + pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }

    static {

        SPECIES.put("minecraft:cow", new AnimalSpecies(
                "minecraft:cow", Cow.class, EntityType.COW,
                List.of(Items.WHEAT),
                biomeMap(
                        "plains", 25, "sunflower_plains", 20, "snowy_plains", 12, "ice_spikes", 5,
                        "forest", 18, "flower_forest", 20, "birch_forest", 18, "old_growth_birch_forest", 15,
                        "dark_forest", 8, "windswept_forest", 10, "taiga", 12, "snowy_taiga", 12,
                        "old_growth_pine_taiga", 10, "old_growth_spruce_taiga", 10, "jungle", 5, "bamboo_jungle", 0,
                        "sparse_jungle", 20, "savanna", 22, "savanna_plateau", 18, "windswept_savanna", 10,
                        "badlands", 0, "eroded_badlands", 0, "wooded_badlands", 5, "windswept_hills", 8,
                        "windswept_gravelly_hills", 5, "meadow", 0, "grove", 8, "snowy_slopes", 8,
                        "frozen_peaks", 0, "jagged_peaks", 0, "stony_peaks", 0, "desert", 0,
                        "swamp", 8, "mangrove_swamp", 0, "ocean", 0, "deep_ocean", 0,
                        "warm_ocean", 0, "lukewarm_ocean", 0, "deep_lukewarm_ocean", 0, "cold_ocean", 0,
                        "deep_cold_ocean", 0, "frozen_ocean", 0, "deep_frozen_ocean", 0, "river", 5,
                        "frozen_river", 3, "beach", 0, "snowy_beach", 0, "mushroom_fields", 0,
                        "cherry_grove", 5, "stony_shore", 3, "dripstone_caves", 0, "lush_caves", 0, "deep_dark", 0
                )));

        SPECIES.put("minecraft:sheep", new AnimalSpecies(
                "minecraft:sheep", Sheep.class, EntityType.SHEEP,
                List.of(Items.WHEAT),
                biomeMap(
                        "plains", 25, "sunflower_plains", 20, "snowy_plains", 15, "ice_spikes", 8,
                        "forest", 15, "flower_forest", 16, "birch_forest", 15, "old_growth_birch_forest", 12,
                        "dark_forest", 6, "windswept_forest", 12, "taiga", 15, "snowy_taiga", 15,
                        "old_growth_pine_taiga", 12, "old_growth_spruce_taiga", 12, "jungle", 2, "bamboo_jungle", 0,
                        "sparse_jungle", 15, "savanna", 20, "savanna_plateau", 16, "windswept_savanna", 8,
                        "badlands", 0, "eroded_badlands", 0, "wooded_badlands", 3, "windswept_hills", 15,
                        "windswept_gravelly_hills", 12, "meadow", 0, "grove", 10, "snowy_slopes", 10,
                        "frozen_peaks", 5, "jagged_peaks", 5, "stony_peaks", 5, "desert", 0,
                        "swamp", 4, "mangrove_swamp", 0, "ocean", 0, "deep_ocean", 0,
                        "warm_ocean", 0, "lukewarm_ocean", 0, "deep_lukewarm_ocean", 0, "cold_ocean", 0,
                        "deep_cold_ocean", 0, "frozen_ocean", 0, "deep_frozen_ocean", 0, "river", 4,
                        "frozen_river", 2, "beach", 0, "snowy_beach", 0, "mushroom_fields", 0,
                        "cherry_grove", 4, "stony_shore", 6, "dripstone_caves", 0, "lush_caves", 0, "deep_dark", 0
                )));

        SPECIES.put("minecraft:pig", new AnimalSpecies(
                "minecraft:pig", Pig.class, EntityType.PIG,
                List.of(Items.CARROT, Items.POTATO, Items.BEETROOT),
                biomeMap(
                        "plains", 15, "sunflower_plains", 15, "snowy_plains", 3, "ice_spikes", 0,
                        "forest", 25, "flower_forest", 22, "birch_forest", 25, "old_growth_birch_forest", 20,
                        "dark_forest", 22, "windswept_forest", 12, "taiga", 14, "snowy_taiga", 6,
                        "old_growth_pine_taiga", 12, "old_growth_spruce_taiga", 12, "jungle", 15, "bamboo_jungle", 10,
                        "sparse_jungle", 18, "savanna", 10, "savanna_plateau", 8, "windswept_savanna", 5,
                        "badlands", 0, "eroded_badlands", 0, "wooded_badlands", 8, "windswept_hills", 5,
                        "windswept_gravelly_hills", 3, "meadow", 0, "grove", 4, "snowy_slopes", 0,
                        "frozen_peaks", 0, "jagged_peaks", 0, "stony_peaks", 0, "desert", 0,
                        "swamp", 25, "mangrove_swamp", 20, "ocean", 0, "deep_ocean", 0,
                        "warm_ocean", 0, "lukewarm_ocean", 0, "deep_lukewarm_ocean", 0, "cold_ocean", 0,
                        "deep_cold_ocean", 0, "frozen_ocean", 0, "deep_frozen_ocean", 0, "river", 8,
                        "frozen_river", 0, "beach", 0, "snowy_beach", 0, "mushroom_fields", 0,
                        "cherry_grove", 10, "stony_shore", 2, "dripstone_caves", 0, "lush_caves", 0, "deep_dark", 0
                )));

        SPECIES.put("minecraft:chicken", new AnimalSpecies(
                "minecraft:chicken", Chicken.class, EntityType.CHICKEN,
                List.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS),
                biomeMap(
                        "plains", 22, "sunflower_plains", 25, "snowy_plains", 4, "ice_spikes", 0,
                        "forest", 20, "flower_forest", 22, "birch_forest", 20, "old_growth_birch_forest", 16,
                        "dark_forest", 14, "windswept_forest", 10, "taiga", 10, "snowy_taiga", 3,
                        "old_growth_pine_taiga", 8, "old_growth_spruce_taiga", 8, "jungle", 8, "bamboo_jungle", 6,
                        "sparse_jungle", 14, "savanna", 20, "savanna_plateau", 16, "windswept_savanna", 8,
                        "badlands", 0, "eroded_badlands", 0, "wooded_badlands", 6, "windswept_hills", 6,
                        "windswept_gravelly_hills", 3, "meadow", 0, "grove", 3, "snowy_slopes", 0,
                        "frozen_peaks", 0, "jagged_peaks", 0, "stony_peaks", 0, "desert", 0,
                        "swamp", 8, "mangrove_swamp", 4, "ocean", 0, "deep_ocean", 0,
                        "warm_ocean", 0, "lukewarm_ocean", 0, "deep_lukewarm_ocean", 0, "cold_ocean", 0,
                        "deep_cold_ocean", 0, "frozen_ocean", 0, "deep_frozen_ocean", 0, "river", 6,
                        "frozen_river", 0, "beach", 0, "snowy_beach", 0, "mushroom_fields", 0,
                        "cherry_grove", 14, "stony_shore", 2, "dripstone_caves", 0, "lush_caves", 0, "deep_dark", 0
                )));

        SPECIES.put("minecraft:rabbit", new AnimalSpecies(
                "minecraft:rabbit", Rabbit.class, EntityType.RABBIT,
                List.of(Items.CARROT, Items.GOLDEN_CARROT, Items.DANDELION),
                biomeMap(
                        "plains", 20, "sunflower_plains", 20, "snowy_plains", 18, "ice_spikes", 18,
                        "forest", 18, "flower_forest", 18, "birch_forest", 18, "old_growth_birch_forest", 18,
                        "dark_forest", 18, "windswept_forest", 18, "taiga", 15, "snowy_taiga", 15,
                        "old_growth_pine_taiga", 15, "old_growth_spruce_taiga", 15, "jungle", 8, "sparse_jungle", 8,
                        "bamboo_jungle", 5, "savanna", 15, "savanna_plateau", 15, "windswept_savanna", 15,
                        "badlands", 12, "eroded_badlands", 6, "wooded_badlands", 12, "windswept_hills", 15,
                        "windswept_gravelly_hills", 15, "meadow", 18, "grove", 10, "snowy_slopes", 10,
                        "frozen_peaks", 10, "jagged_peaks", 10, "stony_peaks", 10, "desert", 15,
                        "swamp", 8, "mangrove_swamp", 5, "ocean", 0, "deep_ocean", 0,
                        "lukewarm_ocean", 0, "deep_lukewarm_ocean", 0, "cold_ocean", 0, "deep_cold_ocean", 0,
                        "frozen_ocean", 0, "deep_frozen_ocean", 0, "warm_ocean", 0, "river", 10,
                        "frozen_river", 10, "beach", 5, "snowy_beach", 5, "mushroom_fields", 0,
                        "cherry_grove", 15, "stony_shore", 0, "dripstone_caves", 0, "lush_caves", 0, "deep_dark", 0
                )));

        SPECIES.put("minecraft:horse", new AnimalSpecies(
                "minecraft:horse", Horse.class, EntityType.HORSE,
                List.of(Items.GOLDEN_APPLE, Items.GOLDEN_CARROT),
                biomeMap(
                        "plains", 22, "sunflower_plains", 20, "snowy_plains", 8, "ice_spikes", 8,
                        "forest", 3, "flower_forest", 3, "birch_forest", 3, "old_growth_birch_forest", 3,
                        "dark_forest", 3, "windswept_forest", 3, "taiga", 5, "snowy_taiga", 5,
                        "old_growth_pine_taiga", 2, "old_growth_spruce_taiga", 2, "jungle", 0, "sparse_jungle", 0,
                        "bamboo_jungle", 0, "savanna", 20, "savanna_plateau", 20, "windswept_savanna", 20,
                        "badlands", 0, "eroded_badlands", 0, "wooded_badlands", 0, "windswept_hills", 8,
                        "windswept_gravelly_hills", 4, "meadow", 15, "grove", 3, "snowy_slopes", 0,
                        "frozen_peaks", 0, "jagged_peaks", 0, "stony_peaks", 0, "desert", 0,
                        "swamp", 0, "mangrove_swamp", 0, "ocean", 0, "deep_ocean", 0,
                        "lukewarm_ocean", 0, "deep_lukewarm_ocean", 0, "cold_ocean", 0, "deep_cold_ocean", 0,
                        "frozen_ocean", 0, "deep_frozen_ocean", 0, "warm_ocean", 0, "river", 5,
                        "frozen_river", 5, "beach", 0, "snowy_beach", 0, "mushroom_fields", 0,
                        "cherry_grove", 5, "stony_shore", 0, "dripstone_caves", 0, "lush_caves", 0, "deep_dark", 0
                )));

        SPECIES.put("minecraft:donkey", new AnimalSpecies(
                "minecraft:donkey", Donkey.class, EntityType.DONKEY,
                List.of(Items.GOLDEN_APPLE, Items.GOLDEN_CARROT),
                biomeMap(
                        "plains", 18, "sunflower_plains", 18, "snowy_plains", 5, "ice_spikes", 5,
                        "forest", 2, "flower_forest", 2, "birch_forest", 2, "old_growth_birch_forest", 2,
                        "dark_forest", 2, "windswept_forest", 2, "taiga", 3, "snowy_taiga", 3,
                        "old_growth_pine_taiga", 3, "old_growth_spruce_taiga", 3, "jungle", 0, "sparse_jungle", 0,
                        "bamboo_jungle", 0, "savanna", 18, "savanna_plateau", 18, "windswept_savanna", 18,
                        "badlands", 8, "eroded_badlands", 4, "wooded_badlands", 8, "windswept_hills", 10,
                        "windswept_gravelly_hills", 10, "meadow", 12, "grove", 4, "snowy_slopes", 4,
                        "frozen_peaks", 4, "jagged_peaks", 4, "stony_peaks", 4, "desert", 8,
                        "swamp", 0, "mangrove_swamp", 0, "ocean", 0, "deep_ocean", 0,
                        "lukewarm_ocean", 0, "deep_lukewarm_ocean", 0, "cold_ocean", 0, "deep_cold_ocean", 0,
                        "frozen_ocean", 0, "deep_frozen_ocean", 0, "warm_ocean", 0, "river", 4,
                        "frozen_river", 4, "beach", 0, "snowy_beach", 0, "mushroom_fields", 0,
                        "cherry_grove", 8, "stony_shore", 0, "dripstone_caves", 0, "lush_caves", 0, "deep_dark", 0
                )));

        SPECIES.put("minecraft:llama", new AnimalSpecies(
                "minecraft:llama", Llama.class, EntityType.LLAMA,
                List.of(Items.HAY_BLOCK),
                biomeMap(
                        "plains", 0, "sunflower_plains", 0, "snowy_plains", 0, "ice_spikes", 0,
                        "forest", 0, "flower_forest", 0, "birch_forest", 0, "old_growth_birch_forest", 0,
                        "dark_forest", 0, "windswept_forest", 0, "taiga", 5, "snowy_taiga", 5,
                        "old_growth_pine_taiga", 0, "old_growth_spruce_taiga", 0, "jungle", 0, "sparse_jungle", 0,
                        "bamboo_jungle", 0, "savanna", 18, "savanna_plateau", 18, "windswept_savanna", 18,
                        "badlands", 5, "eroded_badlands", 5, "wooded_badlands", 5, "windswept_hills", 20,
                        "windswept_gravelly_hills", 0, "meadow", 0, "grove", 8, "snowy_slopes", 8,
                        "frozen_peaks", 8, "jagged_peaks", 8, "stony_peaks", 8, "desert", 3,
                        "swamp", 0, "mangrove_swamp", 0, "ocean", 0, "deep_ocean", 0,
                        "lukewarm_ocean", 0, "deep_lukewarm_ocean", 0, "cold_ocean", 0, "deep_cold_ocean", 0,
                        "frozen_ocean", 0, "deep_frozen_ocean", 0, "warm_ocean", 0, "river", 2,
                        "frozen_river", 0, "beach", 0, "snowy_beach", 0, "mushroom_fields", 0,
                        "cherry_grove", 3, "stony_shore", 0, "dripstone_caves", 0, "lush_caves", 0, "deep_dark", 0
                )));

        SPECIES.put("minecraft:panda", new AnimalSpecies(
                "minecraft:panda", Panda.class, EntityType.PANDA,
                List.of(Items.BAMBOO),
                biomeMap(
                        "plains", 0, "sunflower_plains", 0, "snowy_plains", 0, "ice_spikes", 0,
                        "forest", 0, "flower_forest", 0, "birch_forest", 0, "old_growth_birch_forest", 0,
                        "dark_forest", 0, "windswept_forest", 0, "taiga", 0, "snowy_taiga", 0,
                        "old_growth_pine_taiga", 0, "old_growth_spruce_taiga", 0, "jungle", 8, "sparse_jungle", 8,
                        "bamboo_jungle", 18, "savanna", 0, "savanna_plateau", 0, "windswept_savanna", 0,
                        "badlands", 0, "eroded_badlands", 0, "wooded_badlands", 0, "windswept_hills", 0,
                        "windswept_gravelly_hills", 0, "meadow", 0, "grove", 0, "snowy_slopes", 0,
                        "frozen_peaks", 0, "jagged_peaks", 0, "stony_peaks", 0, "desert", 0,
                        "swamp", 0, "mangrove_swamp", 0, "ocean", 0, "deep_ocean", 0,
                        "lukewarm_ocean", 0, "deep_lukewarm_ocean", 0, "cold_ocean", 0, "deep_cold_ocean", 0,
                        "frozen_ocean", 0, "deep_frozen_ocean", 0, "warm_ocean", 0, "river", 0,
                        "frozen_river", 0, "beach", 0, "snowy_beach", 0, "mushroom_fields", 0,
                        "cherry_grove", 0, "stony_shore", 0, "dripstone_caves", 0, "lush_caves", 0, "deep_dark", 0
                )));

        SPECIES.put("minecraft:goat", new AnimalSpecies(
                "minecraft:goat", Goat.class, EntityType.GOAT,
                List.of(Items.WHEAT),
                biomeMap(
                        "plains", 0, "sunflower_plains", 0, "snowy_plains", 3, "ice_spikes", 5,
                        "forest", 3, "flower_forest", 2, "birch_forest", 2, "old_growth_birch_forest", 2,
                        "dark_forest", 2, "windswept_forest", 2, "taiga", 5, "snowy_taiga", 5,
                        "old_growth_pine_taiga", 5, "old_growth_spruce_taiga", 5, "jungle", 0, "sparse_jungle", 0,
                        "bamboo_jungle", 0, "savanna", 2, "savanna_plateau", 2, "windswept_savanna", 2,
                        "badlands", 3, "eroded_badlands", 3, "wooded_badlands", 3, "windswept_hills", 15,
                        "windswept_gravelly_hills", 15, "meadow", 18, "grove", 22, "snowy_slopes", 22,
                        "frozen_peaks", 22, "jagged_peaks", 22, "stony_peaks", 22, "desert", 2,
                        "swamp", 0, "mangrove_swamp", 0, "ocean", 0, "deep_ocean", 0,
                        "lukewarm_ocean", 0, "deep_lukewarm_ocean", 0, "cold_ocean", 0, "deep_cold_ocean", 0,
                        "frozen_ocean", 0, "deep_frozen_ocean", 0, "warm_ocean", 0, "river", 0,
                        "frozen_river", 0, "beach", 0, "snowy_beach", 0, "mushroom_fields", 0,
                        "cherry_grove", 3, "stony_shore", 0, "dripstone_caves", 0, "lush_caves", 0, "deep_dark", 0
                )));

        SPECIES.put("minecraft:camel", new AnimalSpecies(
                "minecraft:camel", Camel.class, EntityType.CAMEL,
                List.of(Items.CACTUS),
                biomeMap(
                        "savanna", 5, "savanna_plateau", 5, "windswept_savanna", 5,
                        "badlands", 8, "eroded_badlands", 0, "wooded_badlands", 8, "desert", 20
                )));

        SPECIES.put("minecraft:armadillo", new AnimalSpecies(
                "minecraft:armadillo", Armadillo.class, EntityType.ARMADILLO,
                List.of(Items.SPIDER_EYE),
                biomeMap(
                        "savanna", 8, "savanna_plateau", 8, "windswept_savanna", 8,
                        "badlands", 20, "eroded_badlands", 20, "wooded_badlands", 20,
                        "windswept_hills", 5, "windswept_gravelly_hills", 5, "desert", 8
                )));

        SPECIES.put("minecraft:mooshroom", new AnimalSpecies(
                "minecraft:mooshroom", MushroomCow.class, EntityType.MOOSHROOM,
                List.of(Items.WHEAT),
                biomeMap("mushroom_fields", 25)));

        // Everything above is the built-in default. This applies config/herdstead-species.json on top of it
        // (and creates the file the first time).
        SpeciesConfig.load(SPECIES);

    }

    public static AnimalSpecies getForEntity(Animal animal) {
        for (AnimalSpecies species : SPECIES.values()) {
            if (animal.getType() == species.entityType) {
                return species;
            }
        }
        return null;
    }
}