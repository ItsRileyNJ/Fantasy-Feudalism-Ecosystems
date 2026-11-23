package net.itsrileynj.fantasyfeudalismmobs.rootworm.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** A neutral worm investigates the last sound location without acquiring an attack target. */
final class InvestigateSoundGoal extends Goal {
    private final RootWorm worm;
    private Vec3 destination;
    private int repath, listenUntil;
    InvestigateSoundGoal(RootWorm worm) {
        this.worm = worm;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    @Override public boolean canUse() { return worm.hasRecentSound() && !worm.isOnFire(); }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { destination = null; repath = 0; listenUntil = worm.tickCount + 52; }
    @Override public void tick() {
        Vec3 sound = worm.lastHeardPosition();
        worm.faceSound(sound);
        worm.getLookControl().setLookAt(sound.x, sound.y, sound.z);
        boolean arrived = worm.distanceToSqr(sound) < 1.0;
        if (worm.tickCount < listenUntil || arrived) {
            worm.getNavigation().stop();
            worm.setSearching(true);
            if (worm.isSearching() || arrived) return;
        }
        worm.setSearching(false);
        if (--repath <= 0 && (destination == null || destination.distanceToSqr(sound) >= 1 || worm.getNavigation().isDone())) {
            destination = sound;
            repath = 10;
            worm.getNavigation().moveTo(sound.x, sound.y, sound.z, 0.85);
        }
    }
    @Override public void stop() { worm.setSearching(false); worm.getNavigation().stop(); }
}
