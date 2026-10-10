package com.mavethedave.herdstead.mixin;

import com.mavethedave.herdstead.AnimalRegistry;
import com.mavethedave.herdstead.AnimalSpecies;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobSpawnMixin {

    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private void herdstead$blockNaturalSpawn(LevelAccessor level, EntitySpawnReason spawnReason, CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;

        if (!(self instanceof Animal animal)) {
            return;
        }

        AnimalSpecies species = AnimalRegistry.getForEntity(animal);
        if (species != null && (spawnReason == EntitySpawnReason.NATURAL || spawnReason == EntitySpawnReason.CHUNK_GENERATION)) {
            cir.setReturnValue(false);
        }
    }
}