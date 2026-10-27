package net.itsrileynj.fantasyfeudalismmobs;

import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;

public final class SheepCare {
    public static final ResourceLocation COLD_SLOWDOWN=ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID,"cold_sheep");
    private SheepCare() {}

    public static void updateSpeed(Sheep sheep, boolean cold) {
        var speed=sheep.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed==null) return;
        if (cold && !speed.hasModifier(COLD_SLOWDOWN))
            speed.addTransientModifier(new AttributeModifier(COLD_SLOWDOWN,-0.25,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        else if (!cold) speed.removeModifier(COLD_SLOWDOWN);
    }

    public static boolean exposedToCold(Sheep sheep) {
        if (!sheep.isSheared() || !sheep.isAlive()) return false;
        var level=sheep.level();
        var pos=sheep.blockPosition();
        // The rain-blocking heightmap also recognizes glass, leaves and partial roofs.
        // A roof must cover the sheep itself, rather than merely being nearby.
        if (level.getHeight(Heightmap.Types.MOTION_BLOCKING,pos.getX(),pos.getZ())>pos.getY()) return false;
        return level.getBiome(pos).value().getBaseTemperature()<0.8F || level.isRainingAt(pos);
    }
}
