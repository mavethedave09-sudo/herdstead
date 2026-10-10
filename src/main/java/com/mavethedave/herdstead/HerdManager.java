package com.mavethedave.herdstead;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HerdManager {
    private static final double HERD_SEARCH_RANGE = 48.0;
    private static final double FOLLOW_TRIGGER_DISTANCE = 8.0;

    public static void updateHerds(ServerLevel level) {
        for (AnimalSpecies species : AnimalRegistry.SPECIES.values()) {
            updateHerdsForSpecies(species, level);
        }
    }

    private static <T extends Animal> void updateHerdsForSpecies(AnimalSpecies species, ServerLevel level) {
        List<T> found = EntityScan.animalsNearPlayers(level, species);

        List<T> animals = new ArrayList<>();
        for (T candidate : found) {
            if (candidate.entityTags().contains(BLOCKED_TAG)) {
                candidate.discard(); // vanilla worldgen or natural spawn of a managed species
                continue;
            }
            if (!candidate.entityTags().contains(species.domesticTag)) {
                animals.add(candidate);
            }
        }

        List<T> leaders = new ArrayList<>();
        for (T animal : animals) {
            if (animal.entityTags().contains("herd_leader")) {
                leaders.add(animal);
            }
        }

        for (T animal : animals) {
            if (animal.isBaby() || animal.entityTags().contains("herd_leader")) {
                continue;
            }

            T nearestLeader = null;
            double nearestDistSqr = HERD_SEARCH_RANGE * HERD_SEARCH_RANGE;

            for (T leader : leaders) {
                double distSqr = leader.distanceToSqr(animal);
                if (distSqr < nearestDistSqr) {
                    nearestDistSqr = distSqr;
                    nearestLeader = leader;
                }
            }

            if (nearestLeader == null) {
                animal.addTag("herd_leader");
                continue;
            }

            if (animal.distanceTo(nearestLeader) > FOLLOW_TRIGGER_DISTANCE
                    && !predatorNearby(animal, species.key)) {
                animal.getNavigation().moveTo(nearestLeader, 1.0);
            }
        }
    }

    private static boolean predatorNearby(Animal animal, String speciesKey) {
        Set<String> predators = SpeciesRules.predatorsOf(speciesKey);
        if (predators.isEmpty()) {
            return false;
        }
        Level level = animal.level();
        AABB box = animal.getBoundingBox().inflate(16.0);

        if (predators.contains("wolf")
                && !level.getEntitiesOfClass(Wolf.class, box, wolf -> !wolf.isTame()).isEmpty()) {
            return true;
        }
        if (predators.contains("fox") && !level.getEntitiesOfClass(Fox.class, box).isEmpty()) {
            return true;
        }
        return predators.contains("ocelot") && !level.getEntitiesOfClass(Ocelot.class, box).isEmpty();
    }

    public static final String BLOCKED_TAG = "herdstead_vanilla_spawn";
}
