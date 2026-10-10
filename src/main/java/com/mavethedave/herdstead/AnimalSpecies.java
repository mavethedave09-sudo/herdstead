package com.mavethedave.herdstead;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import  net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;

public class AnimalSpecies {
    public final String key;
    public final String domesticTag;
    public final Class<? extends Animal> entityClass;
    public final EntityType<? extends Animal> entityType;
    public final List<Item> breedingItems;
    public final Map<String, Integer> biomePopulations;

    public AnimalSpecies(String key, Class<? extends Animal> entityClass, EntityType<? extends Animal> entityType,
                         List<Item> breedingItems, Map<String, Integer> biomePopulations) {
        this.key = key;
        this.domesticTag = "domestic_" + key.substring(key.indexOf(':') + 1);
        this.entityClass = entityClass;
        this.entityType = entityType;
        this.breedingItems = breedingItems;
        this.biomePopulations = biomePopulations;
    }
}
