package net.itsrileynj.fantasyfeudalismmobs.rootworm;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Independent behavior settings; no biome IDs, habitat tags, or custom block lists. */
public final class RootWormConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue WORM_FIRE_WEAKNESS, WORM_FEAR_HEAT;
    public static final ModConfigSpec.IntValue HEARING_RADIUS, SOUND_MEMORY_TICKS;
    public static final ModConfigSpec.DoubleValue WORM_FIRE_MULTIPLIER, WORM_LAVA_MULTIPLIER;
    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("rootWorm");
        HEARING_RADIUS = builder.defineInRange("hearingRadius", 16, 4, 16);
        SOUND_MEMORY_TICKS = builder.defineInRange("soundMemoryTicks", 80, 20, 400);
        WORM_FIRE_WEAKNESS = builder.define("fireWeaknessEnabled", true);
        WORM_FIRE_MULTIPLIER = builder.defineInRange("fireDamageMultiplier", 2.0, 1.0, 8.0);
        WORM_LAVA_MULTIPLIER = builder.defineInRange("lavaDamageMultiplier", 2.0, 1.0, 8.0);
        WORM_FEAR_HEAT = builder.comment("Avoid vanilla fire/lava and panic while burning.").define("avoidHeat", true);
        builder.pop();
        SPEC = builder.build();
    }
    private RootWormConfig() {}
}
