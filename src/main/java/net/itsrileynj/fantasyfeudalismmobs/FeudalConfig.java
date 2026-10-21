package net.itsrileynj.fantasyfeudalismmobs;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Only the three configuration switches requested by the specification. */
public final class FeudalConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CREEPER_BLOCK_CONVERSION;
    public static final ModConfigSpec.BooleanValue ENDERMAN_BLOCK_PICKUP;
    public static final ModConfigSpec.BooleanValue BLAZE_DASHING;

    static {
        var builder = new ModConfigSpec.Builder();
        CREEPER_BLOCK_CONVERSION = builder.define("creeperBlockConversion", true);
        ENDERMAN_BLOCK_PICKUP = builder.define("endermanBlockPickup", false);
        BLAZE_DASHING = builder.define("blazeAerialDashing", true);
        SPEC = builder.build();
    }

    private FeudalConfig() {}
}
