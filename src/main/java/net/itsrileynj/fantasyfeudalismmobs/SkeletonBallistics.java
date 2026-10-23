package net.itsrileynj.fantasyfeudalismmobs;

import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Retain vanilla shot spread while correcting the gravity arc for the faster launch speed. */
public final class SkeletonBallistics {
    private static final double DRAG = (double)0.99F;
    // The solver only samples times in [0, 120]. Reuse the exact same powers
    // instead of evaluating Math.pow again for every trajectory-search step.
    private static final double[] DRAG_POWERS = dragPowers();

    private static double[] dragPowers() {
        double[] powers = new double[121];
        for (int tick = 0; tick < powers.length; tick++) powers[tick] = Math.pow(DRAG, tick);
        return powers;
    }

    public static void accelerate(AbstractArrow arrow, AbstractSkeleton shooter) {
        Vec3 original = arrow.getDeltaMovement();
        Vec3 accelerated = original.scale(1.4);
        var target = shooter.getTarget();
        ItemStack weapon = arrow.getWeaponItem();
        if (target != null && target.isAlive() && shooter.distanceToSqr(target) <= 35 * 35
                && weapon != null && (weapon.is(Items.BOW) || weapon.is(Items.CROSSBOW))
                && !arrow.isInWater() && original.lengthSqr() > 1.0E-8) {
            Vec3 offset = new Vec3(target.getX() - arrow.getX(), target.getY(1.0 / 3) - arrow.getY(), target.getZ() - arrow.getZ());
            double horizontal = offset.horizontalDistance();
            if (horizontal > 0.01) {
                Vec3 aim = launch(offset, accelerated.length(), arrow.getGravity());
                if (aim != null) {
                    Vec3 vanillaAim = new Vec3(offset.x, offset.y + horizontal * 0.2F, offset.z).normalize();
                    Vec3 spread = original.normalize().subtract(vanillaAim);
                    accelerated = aim.normalize().add(spread).normalize().scale(accelerated.length());
                }
            }
        }
        arrow.setDeltaMovement(accelerated);
    }

    private static Vec3 launch(Vec3 offset, double speed, double gravity) {
        double horizontal = offset.horizontalDistance();
        // Find the first (low-arc) solution of the discrete vanilla drag/gravity trajectory.
        for (int tick = 1; tick <= 120; tick++) {
            if (requiredSpeed(horizontal, offset.y, gravity, tick) > speed) continue;
            double low = tick - 1, high = tick;
            for (int step = 0; step < 36; step++) {
                double middle = (low + high) * 0.5;
                if (requiredSpeed(horizontal, offset.y, gravity, middle) > speed) low = middle;
                else high = middle;
            }
            double time = (low + high) * 0.5;
            double travel = travelFactor(time);
            double vertical = (offset.y + gravity / (1 - DRAG) * (time - travel)) / travel;
            return new Vec3(offset.x / travel, vertical, offset.z / travel);
        }
        return null;
    }

    private static double requiredSpeed(double horizontal, double height, double gravity, double time) {
        double travel = travelFactor(time);
        return Math.hypot(horizontal, height + gravity / (1 - DRAG) * (time - travel)) / travel;
    }

    private static double travelFactor(double time) {
        int whole = (int)time;
        double power = DRAG_POWERS[whole];
        return (1 - power) / (1 - DRAG) + (time - whole) * power;
    }
    private SkeletonBallistics() {}
}
