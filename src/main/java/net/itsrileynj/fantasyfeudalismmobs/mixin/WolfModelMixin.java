package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.WolfState;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WolfModel.class)
public abstract class WolfModelMixin {
    @Shadow @Final private ModelPart head;
    @Shadow @Final private ModelPart body;
    @Shadow @Final private ModelPart upperBody;
    @Shadow @Final private ModelPart tail;
    @Shadow @Final private ModelPart rightHindLeg;
    @Shadow @Final private ModelPart leftHindLeg;
    @Shadow @Final private ModelPart rightFrontLeg;
    @Shadow @Final private ModelPart leftFrontLeg;
    @Unique private float ffm$crouch;

    @Inject(method="prepareMobModel(Lnet/minecraft/world/entity/animal/Wolf;FFF)V",at=@At("HEAD"))
    private void ffm$reset(Wolf wolf,float walk,float amplitude,float partial,CallbackInfo ci){
        head.y=head.getInitialPose().y;
        rightHindLeg.yScale=leftHindLeg.yScale=rightFrontLeg.yScale=leftFrontLeg.yScale=1;
        ffm$crouch=wolf.isTame()||wolf.isInSittingPose()?0:((WolfState)wolf).ffm$crouch(partial);
    }
    @Inject(method="prepareMobModel(Lnet/minecraft/world/entity/animal/Wolf;FFF)V",at=@At("TAIL"))
    private void ffm$lowWalk(Wolf wolf,float walk,float amplitude,float partial,CallbackInfo ci){
        if(ffm$crouch<=0)return;
        float bob=Mth.sin(walk*1.3324F)*Math.min(amplitude,1)*0.15F;
        body.y+=(2.5F+bob)*ffm$crouch;upperBody.y+=(2.5F+bob)*ffm$crouch;
        head.y+=(3.5F+bob)*ffm$crouch;tail.y+=2.5F*ffm$crouch;
        for(var leg:new ModelPart[]{rightHindLeg,leftHindLeg,rightFrontLeg,leftFrontLeg}){
            leg.y+=2*ffm$crouch;leg.yScale=1-0.25F*ffm$crouch;
            leg.xRot*=1-0.4F*ffm$crouch;
        }
    }
    @Inject(method="setupAnim(Lnet/minecraft/world/entity/animal/Wolf;FFFFF)V",at=@At("TAIL"))
    private void ffm$headAndTail(Wolf wolf,float walk,float amplitude,float age,float yaw,float pitch,CallbackInfo ci){
        head.xRot=Mth.lerp(ffm$crouch,head.xRot,0.35F);
        tail.xRot=Mth.lerp(ffm$crouch,tail.xRot,0.15F);
    }
}
