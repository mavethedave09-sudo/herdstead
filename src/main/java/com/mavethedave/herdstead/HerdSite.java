package com.mavethedave.herdstead;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// A planned herd that hasn't spawned yet. Removed from the zone once it spawns.
public class HerdSite {
    public String speciesKey;
    public long chunk;
    public int size;

    // Not saved: bookkeeping for sites that keep failing to find a valid spot.
    public int failedPasses = 0;
    public int relocations = 0;

    // Parameter order MUST match the Codec field order below.
    public HerdSite(String speciesKey, long chunk, int size) {
        this.speciesKey = speciesKey;
        this.chunk = chunk;
        this.size = size;
    }

    public static final Codec<HerdSite> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("speciesKey").forGetter(s -> s.speciesKey),
            Codec.LONG.fieldOf("chunk").forGetter(s -> s.chunk),
            Codec.INT.fieldOf("size").forGetter(s -> s.size)
    ).apply(instance, HerdSite::new));
}