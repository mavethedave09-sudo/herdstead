package com.mavethedave.herdstead.mixin;

import com.mavethedave.herdstead.AnimalRegistry;
import com.mavethedave.herdstead.HerdManager;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobFinalizeMixin {

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void herdstead$tagVanillaSpawns(ServerLevelAccessor level, DifficultyInstance difficulty,
                                            EntitySpawnReason spawnReason, SpawnGroupData groupData,
                                            CallbackInfoReturnable<SpawnGroupData> cir) {
        if (spawnReason != EntitySpawnReason.NATURAL && spawnReason != EntitySpawnReason.CHUNK_GENERATION) {
            return;
        }
        if ((Object) this instanceof Animal animal && AnimalRegistry.getForEntity(animal) != null) {
            animal.addTag(HerdManager.BLOCKED_TAG);
        }
    }
}