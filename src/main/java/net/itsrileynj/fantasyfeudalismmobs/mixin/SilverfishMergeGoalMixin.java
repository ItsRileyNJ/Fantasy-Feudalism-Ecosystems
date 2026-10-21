package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.SilverfishBurrowing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.block.InfestedBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.entity.monster.Silverfish$SilverfishMergeWithStoneGoal")
public abstract class SilverfishMergeGoalMixin extends RandomStrollGoal {
    @Shadow private boolean doMerge;
    @Shadow private Direction selectedDirection;

    protected SilverfishMergeGoalMixin(PathfinderMob mob, double speed) { super(mob, speed); }

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void ffm$burrowOnlyAfterOwnKill(CallbackInfoReturnable<Boolean> cir) {
        doMerge = false;
        Silverfish fish = (Silverfish) mob;
        if (SilverfishBurrowing.isBurrowed(fish) || mob.getTarget() != null) {
            cir.setReturnValue(false);
            return;
        }
        if (SilverfishBurrowing.hasKill(fish)) {
            // Hiding changes no blocks, so it also works with mobGriefing disabled.
            BlockPos origin = BlockPos.containing(mob.getX(), mob.getY() + 0.5, mob.getZ());
            for (Direction direction : Direction.values()) {
                if (InfestedBlock.isCompatibleHostBlock(mob.level().getBlockState(origin.relative(direction)))) {
                    selectedDirection = direction;
                    doMerge = true;
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
        cir.setReturnValue(mob.getNavigation().isDone() && super.canUse());
    }

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private void ffm$temporaryBurrow(CallbackInfo ci) {
        if (!doMerge) return;
        BlockPos block = BlockPos.containing(mob.getX(), mob.getY() + 0.5, mob.getZ()).relative(selectedDirection);
        SilverfishBurrowing.enter((Silverfish) mob, block);
        ci.cancel(); // Never create a permanent infested block or discard the timer entity early.
    }
}
