package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.SheepState;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SheepModel.class)
public abstract class SheepModelMixin extends QuadrupedModel<Sheep> {
    protected SheepModelMixin(ModelPart root) { super(root,false,8,4,2,2,24); }

    @Inject(method="setupAnim(Lnet/minecraft/world/entity/animal/Sheep;FFFFF)V",at=@At("HEAD"))
    private void ffm$resetShiver(Sheep sheep,float walk,float amplitude,float age,float yaw,float pitch,CallbackInfo ci) {
        // Renderers share one model: a cold sheep must never leave an offset on a warm one.
        head.x=head.getInitialPose().x;body.x=body.getInitialPose().x;
        head.zRot=head.getInitialPose().zRot;body.zRot=body.getInitialPose().zRot;
        rightFrontLeg.zRot=rightFrontLeg.getInitialPose().zRot;leftFrontLeg.zRot=leftFrontLeg.getInitialPose().zRot;
        rightHindLeg.zRot=rightHindLeg.getInitialPose().zRot;leftHindLeg.zRot=leftHindLeg.getInitialPose().zRot;
    }

    @Inject(method="setupAnim(Lnet/minecraft/world/entity/animal/Sheep;FFFFF)V",at=@At("TAIL"))
    private void ffm$visibleShiver(Sheep sheep,float walk,float amplitude,float age,float yaw,float pitch,CallbackInfo ci) {
        if(!((SheepState)sheep).ffm$isCold()||!sheep.isAlive())return;
        // Around three trembles per second, interpolated by the native render age.
        float shake=Mth.sin(age*.95F);
        head.x+=.35F*shake;head.yRot+=.09F*shake;head.zRot+=.04F*shake;
        body.x+=.2F*shake;body.zRot+=.045F*shake;
        rightFrontLeg.zRot+=.025F*shake;leftFrontLeg.zRot+=.025F*shake;
        rightHindLeg.zRot+=.025F*shake;leftHindLeg.zRot+=.025F*shake;
    }
}
