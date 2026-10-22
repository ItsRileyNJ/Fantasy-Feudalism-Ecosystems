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

    @Inject(method="setupAnim(Lnet/minecraft/world/entity/Mob;FFFFF)V",at=@At("HEAD"))
    private void ffm$crouchedBody(Mob entity,float walk,float amplitude,float age,float yaw,float pitch,CallbackInfo ci){
        crouching=entity.isCrouching();
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/Mob;FFFFF)V", at = @At("TAIL"))
    private void ffm$crossbowPose(Mob entity, float walk, float amplitude, float age, float yaw, float pitch, CallbackInfo ci) {
        if(entity instanceof net.minecraft.world.entity.monster.WitherSkeleton&&entity.isBlocking()){
            boolean right=entity.getMainArm()!=HumanoidArm.RIGHT;
            ModelPart shield=right?rightArm:leftArm, sword=right?leftArm:rightArm;
            shield.xRot=-.9424779F+net.minecraft.util.Mth.clamp(head.xRot,-1.396F,.436F);
            shield.yRot=(right?-.5235988F:.5235988F)+head.yRot;shield.zRot=0;
            sword.xRot=-.15F+net.minecraft.util.Mth.cos(walk*.6662F)*amplitude*.5F;return;
        }
        boolean mainHand = entity.getMainHandItem().getItem() instanceof CrossbowItem;
        if (!mainHand && !(entity.getOffhandItem().getItem() instanceof CrossbowItem)) return;
        boolean right = (entity.getMainArm() == HumanoidArm.RIGHT) == mainHand;
        rightArm.zRot = 0;
        leftArm.zRot = 0;
        if (entity.isUsingItem() && entity.getUseItem().getItem() instanceof CrossbowItem)
            AnimationUtils.animateCrossbowCharge(rightArm, leftArm, entity, right);
        else AnimationUtils.animateCrossbowHold(rightArm, leftArm, head, right);
    }
}
