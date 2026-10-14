package com.mavethedave.herdstead.mixin;

import com.mavethedave.herdstead.AnimalRegistry;
import com.mavethedave.herdstead.AnimalSpecies;
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

// Animals placed by structures (village pens etc.) are farm animals, not wildlife: tag them domestic.
@Mixin(Mob.class)
public class StructureSpawnMixin {
    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void herdstead$tagStructureAnimals(ServerLevelAccessor level, DifficultyInstance difficulty,
                                               EntitySpawnReason reason, SpawnGroupData groupData,
                                               CallbackInfoReturnable<SpawnGroupData> cir) {
        if (reason != EntitySpawnReason.STRUCTURE) {
            return;
        }
        if (!((Object) this instanceof Animal animal)) {
            return;
        }
        AnimalSpecies species = AnimalRegistry.getForEntity(animal);
        if (species != null) {
            animal.addTag(species.domesticTag);
        }
    }
}