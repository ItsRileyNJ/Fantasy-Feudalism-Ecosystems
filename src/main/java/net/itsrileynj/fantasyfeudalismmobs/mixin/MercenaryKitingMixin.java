package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** CrossbowAttackMob is a capability; classify skeleton threats by their equipped weapon. */
@Pseudo
@Mixin(targets="dev.villagerscontract.KitingCrossbowGoal", remap=false)
public abstract class MercenaryKitingMixin {
    @Inject(method="preferredDistance", at=@At("HEAD"), cancellable=true, remap=false)
    private static void ffm$actualSkeletonWeapon(LivingEntity target, CallbackInfoReturnable<Double> result) {
        if (target instanceof AbstractSkeleton skeleton) {
            if (skeleton.isHolding(stack -> stack.getItem() instanceof CrossbowItem)) result.setReturnValue(12.0);
            else if (skeleton.isHolding(stack -> stack.getItem() instanceof BowItem)) result.setReturnValue(20.0);
            else result.setReturnValue(7.0);
        }
    }
}
