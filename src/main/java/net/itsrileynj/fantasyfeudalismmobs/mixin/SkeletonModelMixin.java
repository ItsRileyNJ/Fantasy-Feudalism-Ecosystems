package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reuse vanilla crossbow poses instead of the skeleton model's default sword swing. */
@Mixin(SkeletonModel.class)
public abstract class SkeletonModelMixin extends HumanoidModel<Mob> {
    protected SkeletonModelMixin(ModelPart root) { super(root); }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/Mob;FFFFF)V", at = @At("TAIL"))
    private void ffm$crossbowPose(Mob entity, float walk, float amplitude, float age, float yaw, float pitch, CallbackInfo ci) {
        if (!(entity.getMainHandItem().getItem() instanceof CrossbowItem)) return;
        boolean right = entity.getMainArm() == HumanoidArm.RIGHT;
        if (entity.isUsingItem()) AnimationUtils.animateCrossbowCharge(rightArm, leftArm, entity, right);
        else if (entity.isAggressive()) AnimationUtils.animateCrossbowHold(rightArm, leftArm, head, right);
    }
}
