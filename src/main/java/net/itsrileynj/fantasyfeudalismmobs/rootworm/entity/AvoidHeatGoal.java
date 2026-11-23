package net.itsrileynj.fantasyfeudalismmobs.rootworm.entity;

import java.util.EnumSet;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.RootWormConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Scan nearby heat at a bounded interval and flee to a reachable point farther from it. */
public final class AvoidHeatGoal extends Goal {
    private final RootWorm worm;
    private Path escape;
    private BlockPos heat;
    private int nextScan;
    private int fleeUntil;

    public AvoidHeatGoal(RootWorm worm) {
        this.worm = worm;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean canUse() {
        if (!RootWormConfig.WORM_FEAR_HEAT.get() || worm.tickCount < nextScan) return false;
        nextScan = worm.tickCount + adjustedTickDelay(10);
        heat = nearestHeat();
        if (heat == null) return false;
        double current = worm.distanceToSqr(heat.getCenter());
        Vec3 away = worm.position().subtract(heat.getCenter()).multiply(1, 0, 1).normalize();
        if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
        // Bias searches away from the hazard rather than accepting an arbitrary random panic path.
        for (int i = 0; i < 12; i++) {
            Vec3 target = worm.position().add(away.scale(6 + worm.getRandom().nextInt(5)))
                    .add(worm.getRandom().nextInt(7) - 3, 0, worm.getRandom().nextInt(7) - 3);
            BlockPos candidate = BlockPos.containing(target);
            if (candidate.distSqr(heat) <= current + 4 || RootWorm.isHeat(worm.level(), candidate)
                    || RootWorm.isHeat(worm.level(), candidate.below())) continue;
            escape = worm.getNavigation().createPath(candidate, 0);
            if (escape != null && escape.canReach()) return true;
        }
        return false;
    }

    private BlockPos nearestHeat() {
        BlockPos nearest = null;
        double distance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(worm.blockPosition().offset(-6, -2, -6), worm.blockPosition().offset(6, 2, 6))) {
            if (!RootWorm.isHeat(worm.level(), pos)) continue;
            double value = worm.distanceToSqr(pos.getCenter());
            if (value < distance) { nearest = pos.immutable(); distance = value; }
        }
        return nearest;
    }

    @Override public void start() { fleeUntil = worm.tickCount + 80; worm.getNavigation().moveTo(escape, 1.65); }
    @Override public boolean canContinueToUse() {
        return RootWormConfig.WORM_FEAR_HEAT.get() && worm.tickCount < fleeUntil && !worm.getNavigation().isDone();
    }
    @Override public void stop() { worm.getNavigation().stop(); }
}
