package net.itsrileynj.fantasyfeudalismmobs.rootworm.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Pursue remembered sounds, then commit to a short bite; do not constantly repath around a retreating target. */
final class RootWormAttackGoal extends Goal {
    private final RootWorm worm;
    private int nextAttackAt, windup, repath;
    private Vec3 destination;
    RootWormAttackGoal(RootWorm worm) {
        this.worm = worm;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    private LivingEntity target() {
        var target = worm.getTarget();
        return target != null && target.isAlive() && !(target instanceof Player p && (p.isCreative() || p.isSpectator())) ? target : null;
    }
    @Override public boolean canUse() {
        var target = target();
        return target != null && (worm.hasRecentSoundFrom(target) || worm.sensesCloseTarget(target));
    }
    @Override public boolean canContinueToUse() { return windup > 0 && target() != null || canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { worm.setSearching(false); destination = null; repath = 0; }
    @Override public void tick() {
        var target = target();
        if (target == null) return;
        if (windup > 0) {
            if (--windup == 0 && worm.canBite(target)) worm.doHurtTarget(target);
            return;
        }
        boolean close = worm.sensesCloseTarget(target);
        if (close) {
            worm.faceSound(target.position());
            worm.getLookControl().setLookAt(target, 40, 30);
        }
        else if (worm.hasRecentSoundFrom(target)) {
            Vec3 sound = worm.lastHeardPosition();
            worm.faceSound(sound);
            worm.getLookControl().setLookAt(sound.x, sound.y, sound.z);
        }
        if (worm.tickCount >= nextAttackAt && worm.canBite(target)) {
            nextAttackAt = worm.tickCount + 20; // Same attack frequency as before; only the animation/windup are faster.
            windup = 3;
            worm.beginBite();
            worm.getNavigation().stop();
            Vec3 toward = target.position().subtract(worm.position()).multiply(1, 0, 1).normalize();
            worm.setDeltaMovement(worm.getDeltaMovement().add(toward.scale(0.28)));
            return;
        }
        if (worm.canBite(target)) { worm.getNavigation().stop(); return; }
        Vec3 sound = close ? target.position() : worm.hasRecentSoundFrom(target) ? worm.lastHeardPosition() : null;
        if (sound == null) { worm.getNavigation().stop(); return; }
        if (--repath <= 0 && (destination == null || destination.distanceToSqr(sound) >= 0.64 || worm.getNavigation().isDone())) {
            destination = sound;
            repath = 8;
            worm.getNavigation().moveTo(sound.x, sound.y, sound.z, 1.15);
        }
    }
    @Override public void stop() { windup = 0; worm.getNavigation().stop(); }
}
