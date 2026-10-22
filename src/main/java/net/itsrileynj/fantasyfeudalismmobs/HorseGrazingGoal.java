package net.itsrileynj.fantasyfeudalismmobs;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.EventHooks;

/** Sheep-style grazing with the horse's existing synchronized head-down pose. */
public final class HorseGrazingGoal extends Goal {
    private final Horse horse;
    private int remaining;
    private long nextGraze=Long.MIN_VALUE;
    private boolean scheduledReady;

    public HorseGrazingGoal(Horse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    private boolean grassAvailable() {
        return horse.level().getBlockState(horse.blockPosition()).is(Blocks.SHORT_GRASS)
                || horse.level().getBlockState(horse.blockPosition().below()).is(Blocks.GRASS_BLOCK);
    }

    private void schedule() {
        scheduledReady=HorseLife.ready(horse);
        int minimum=scheduledReady?100:horse.isBaby()?200:400;
        nextGraze=horse.level().getGameTime()+minimum+horse.getRandom().nextInt(minimum+1);
    }

    @Override public boolean canUse() {
        if (horse.isVehicle() || horse.isPassenger() || horse.isInLove() || horse.isStanding() || horse.isInWater()
                || horse.isPanicking() || !horse.onGround()) return false;
        if(nextGraze==Long.MIN_VALUE)schedule();
        else if(HorseLife.ready(horse)&&!scheduledReady) {
            // Becoming eligible must not wait for the remainder of an ordinary grazing interval.
            nextGraze=Math.min(nextGraze,horse.level().getGameTime()+100+horse.getRandom().nextInt(101));
            scheduledReady=true;
        }
        return horse.level().getGameTime()>=nextGraze&&grassAvailable();
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canContinueToUse() {
        return remaining > 0 && !horse.isVehicle() && !horse.isPassenger() && !horse.isInWater()
                && !horse.isStanding() && !horse.isPanicking() && !horse.isInLove();
    }
    @Override public void start() {
        remaining = 40;
        horse.getNavigation().stop();
        horse.setEating(true);
    }
    @Override public void stop() { remaining = 0; horse.setEating(false);schedule(); }

    @Override public void tick() {
        if (--remaining != 4) return;
        BlockPos pos = horse.blockPosition();
        boolean ate = false;
        if (horse.level().getBlockState(pos).is(Blocks.SHORT_GRASS)) {
            if (EventHooks.canEntityGrief(horse.level(), horse)) horse.level().destroyBlock(pos, false);
            ate = true;
        } else if (horse.level().getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
            if (EventHooks.canEntityGrief(horse.level(), horse)) {
                horse.level().levelEvent(2001, pos.below(), Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));
                horse.level().setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 2);
            }
            ate = true;
        }
        if (ate) HorseLife.grazed(horse);
    }
}
