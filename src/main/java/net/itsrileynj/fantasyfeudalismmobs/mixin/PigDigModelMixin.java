package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.PigState;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Pig;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(QuadrupedModel.class)
public abstract class PigDigModelMixin {
    @Shadow @Final protected ModelPart head;
    @Shadow @Final protected ModelPart body;
    @Shadow @Final protected ModelPart rightFrontLeg;
    @Shadow @Final protected ModelPart leftFrontLeg;

    @Inject(method="setupAnim",at=@At("TAIL"))
    private void ffm$rootInTheSoil(Entity entity,float walk,float amplitude,float age,float yaw,float pitch,CallbackInfo ci) {
        if(!(entity instanceof Pig pig))return;
        float dig=((PigState)pig).ffm$digAmount(Mth.clamp(age-pig.tickCount,0,1));
        // Reset offsets on every render: models are shared by digging and idle pigs.
        head.y=head.getInitialPose().y;
        body.y=body.getInitialPose().y;body.xRot=body.getInitialPose().xRot;
        if(dig<=0)return;
        float paw=Mth.sin(age*1.2F),snuffle=Mth.sin(age*.8F);
        head.y+=(3.5F+.35F*snuffle)*dig;
        head.xRot=Mth.lerp(dig,head.xRot,.65F+.12F*snuffle);head.yRot*=1-dig;
        body.y+=.25F*snuffle*dig;body.xRot+=.08F*dig;
        rightFrontLeg.xRot=Mth.lerp(dig,rightFrontLeg.xRot,-.25F+.4F*paw);
        leftFrontLeg.xRot=Mth.lerp(dig,leftFrontLeg.xRot,-.25F-.4F*paw);
    }
}
