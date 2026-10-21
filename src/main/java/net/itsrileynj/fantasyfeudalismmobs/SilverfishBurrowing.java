package net.itsrileynj.fantasyfeudalismmobs;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.phys.Vec3;

public final class SilverfishBurrowing {
    public interface EffectTicker { void ffm$tickBurrowEffects(); }
    private static final String KILL = "ffm_zombie_kill";
    private static final String UNTIL = "ffm_burrow_until";
    public static final int DURATION = 600;

    public static boolean hasKill(Silverfish fish) { return fish.getPersistentData().getBoolean(KILL); }
    public static boolean isBurrowed(Silverfish fish) { return fish.getPersistentData().contains(UNTIL); }
    public static long deadline(Silverfish fish) { return fish.getPersistentData().getLong(UNTIL); }

    public static void creditKill(Silverfish fish) {
        if (!fish.isAlive() || isBurrowed(fish)) return;
        fish.getPersistentData().putBoolean(KILL, true);
        fish.setTarget(null);
        fish.getNavigation().stop();
        fish.setAggressive(false);
    }

    public static void enter(Silverfish fish, BlockPos block) {
        if (fish.level().isClientSide || !hasKill(fish) || isBurrowed(fish)
                || !InfestedBlock.isCompatibleHostBlock(fish.level().getBlockState(block))) return;
        fish.spawnAnim();
        fish.getPersistentData().putLong(UNTIL, fish.level().getGameTime() + DURATION);
        fish.moveTo(block.getX() + 0.5, block.getY() + 0.25, block.getZ() + 0.5, fish.getYRot(), 0);
        restore(fish);
    }

    public static void restore(Silverfish fish) {
        if (!isBurrowed(fish)) return;
        fish.setTarget(null);
        fish.getNavigation().stop();
        fish.setPersistenceRequired();
        fish.setNoAi(true);
        fish.setNoGravity(true);
        fish.setInvisible(true);
        fish.clearFire();
        fish.setSharedFlagOnFire(false);
        fish.setSilent(true);
        fish.setInvulnerable(true);
        fish.noPhysics = true;
        fish.setDeltaMovement(Vec3.ZERO);
    }

    public static void tick(Silverfish fish) {
        if (fish.level().getGameTime() >= deadline(fish)) fish.discard();
        else {
            ((EffectTicker) fish).ffm$tickBurrowEffects();
            restore(fish);
        }
    }

    private SilverfishBurrowing() {}
}
