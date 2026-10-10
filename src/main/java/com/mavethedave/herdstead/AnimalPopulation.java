package com.mavethedave.herdstead;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class AnimalPopulation {
    public int maxPopulation;
    public int currentPopulation;
    public int depletion;
    public int recoveryTimer;
    public int depletionDecayTimer;
    public int depletionDecayTarget;

    public AnimalPopulation(int maxPopulation) {
        this.maxPopulation = maxPopulation;
        this.currentPopulation = 0;
        this.depletion = 0;
        this.recoveryTimer = 0;
        this.depletionDecayTimer = 0;
        this.depletionDecayTarget = rollDecayTarget();
    }

    // Parameter order MUST match the Codec field order below (binding is positional).
    public AnimalPopulation(int maxPopulation, int currentPopulation, int depletion,
                            int recoveryTimer, int depletionDecayTimer, int depletionDecayTarget) {
        this.maxPopulation = maxPopulation;
        this.currentPopulation = currentPopulation;
        this.depletion = depletion;
        this.recoveryTimer = recoveryTimer;
        this.depletionDecayTimer = depletionDecayTimer;
        this.depletionDecayTarget = depletionDecayTarget;
    }

    public static int rollDecayTarget() {
        return 168000 + (int) (Math.random() * (336000 - 168000 + 1));
    }

    public static final Codec<AnimalPopulation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("maxPopulation").forGetter(a -> a.maxPopulation),
            Codec.INT.fieldOf("currentPopulation").forGetter(a -> a.currentPopulation),
            Codec.INT.fieldOf("depletion").forGetter(a -> a.depletion),
            Codec.INT.fieldOf("recoveryTimer").forGetter(a -> a.recoveryTimer),
            Codec.INT.fieldOf("depletionDecayTimer").forGetter(a -> a.depletionDecayTimer),
            Codec.INT.fieldOf("depletionDecayTarget").forGetter(a -> a.depletionDecayTarget)
    ).apply(instance, AnimalPopulation::new));
}