package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.SpiderCompanion;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Spider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpiderModel.class)
public abstract class SpiderModelMixin {
    @Shadow @Final private ModelPart root;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V", at = @At("TAIL"))
    private void ffm$tiltSittingAbdomen(Entity entity, float walk, float amplitude, float age, float yaw, float pitch, CallbackInfo ci) {
        ModelPart abdomen = root.getChild("body1");
        // The abdomen's front center is six model units ahead of its native center pivot.
        // Compensate the rotated pivot so that attachment stays fixed to the thorax.
        var initial=abdomen.getInitialPose();
        abdomen.y=initial.y;abdomen.z=initial.z;abdomen.xRot=initial.xRot;
        if(entity instanceof Spider spider&&SpiderCompanion.isTamed(spider)&&SpiderCompanion.isSitting(spider)) {
            float angle=initial.xRot-.25F;
            abdomen.y-=6F*((float)Math.sin(angle)-(float)Math.sin(initial.xRot));
            abdomen.z+=6F*((float)Math.cos(angle)-(float)Math.cos(initial.xRot));
            abdomen.xRot=angle;
        }
    }
}
