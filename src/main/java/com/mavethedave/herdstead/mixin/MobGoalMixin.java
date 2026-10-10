package com.mavethedave.herdstead.mixin;

import com.mavethedave.herdstead.AnimalRegistry;
import com.mavethedave.herdstead.AnimalSpecies;
import com.mavethedave.herdstead.SpeciesRules;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.function.Predicate;

@Mixin(Mob.class)
public abstract class MobGoalMixin {

    @Shadow
    @Final
    protected GoalSelector goalSelector;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void herdstead$addPredatorAvoidance(EntityType<? extends Mob> entityType, Level level, CallbackInfo ci) {
        if (!((Object) this instanceof PathfinderMob pathfinder)) {
            return;
        }

        for (AnimalSpecies species : AnimalRegistry.SPECIES.values()) {
            if (species.entityType != entityType) {
                continue;
            }
            Set<String> predators = SpeciesRules.predatorsOf(species.key);
            if (predators.contains("wolf")) {
                herdstead$avoid(pathfinder, Wolf.class, other -> other instanceof Wolf wolf && !wolf.isTame());
            }
            if (predators.contains("fox")) {
                herdstead$avoid(pathfinder, Fox.class, other -> true);
            }
            if (predators.contains("ocelot")) {
                herdstead$avoid(pathfinder, Ocelot.class, other -> true);
            }
            return;
        }
    }

    private <T extends LivingEntity> void herdstead$avoid(PathfinderMob mob, Class<T> predator,
                                                          Predicate<LivingEntity> filter) {
        this.goalSelector.addGoal(3,
                new AvoidEntityGoal<>(mob, predator, filter, 16.0F, 1.3, 1.6, other -> true));
    }
}