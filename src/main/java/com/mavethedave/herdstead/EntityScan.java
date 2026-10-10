package com.mavethedave.herdstead;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class EntityScan {
    // Only look this far from each player. Entities farther away aren't being simulated anyway.
    public static final double RADIUS = 256.0;

    private EntityScan() {
    }

    // All loaded animals of exactly this species near any player (domestic ones included; callers filter).
    @SuppressWarnings("unchecked")
    public static <T extends Animal> List<T> animalsNearPlayers(ServerLevel level, AnimalSpecies species) {
        Class<T> cls = (Class<T>) species.entityClass;
        List<T> result = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();

        for (ServerPlayer player : level.players()) {
            AABB box = new AABB(player.getX() - RADIUS, level.getMinY(), player.getZ() - RADIUS,
                    player.getX() + RADIUS, level.getMaxY(), player.getZ() + RADIUS);
            for (T animal : level.getEntitiesOfClass(cls, box)) {
                if (animal.getType() == species.entityType && seen.add(animal.getUUID())) {
                    result.add(animal);
                }
            }
        }
        return result;
    }
}