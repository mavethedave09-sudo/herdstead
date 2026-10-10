package com.mavethedave.herdstead.mixin;

import com.mavethedave.herdstead.AnimalRegistry;
import com.mavethedave.herdstead.AnimalSpecies;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public class AnimalMixin {
    @Inject(method = "finalizeSpawnChildFromBreeding", at = @At("HEAD"))
    private void herdstead$onFinalizeSpawnChildFromBreeding(ServerLevel level, Animal partner, AgeableMob offspring, CallbackInfo ci) {
        Animal self = (Animal) (Object) this;

        if (!(offspring instanceof Animal offspringAnimal)) {
            return;
        }

        AnimalSpecies selfSpecies = AnimalRegistry.getForEntity(self);
        AnimalSpecies partnerSpecies = AnimalRegistry.getForEntity(partner);
        AnimalSpecies offspringSpecies = AnimalRegistry.getForEntity(offspringAnimal);

        if (selfSpecies == null || partnerSpecies == null || offspringSpecies == null) {
            return;
        }

        if (!selfSpecies.key.equals(partnerSpecies.key) || !selfSpecies.key.equals(offspringSpecies.key)) {
            return;
        }

        boolean selfDomestic = self.entityTags().contains(selfSpecies.domesticTag);
        boolean partnerDomestic = partner.entityTags().contains(partnerSpecies.domesticTag);

       if (selfDomestic && partnerDomestic) {
           offspringAnimal.addTag(offspringSpecies.domesticTag);
       }
    }
}





























