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
    private void ffm$lowerSittingAbdomen(Entity entity, float walk, float amplitude, float age, float yaw, float pitch, CallbackInfo ci) {
        ModelPart abdomen = root.getChild("body1");
        // Reset on every entity: this model is shared by wild and tamed spiders.
        abdomen.y = abdomen.getInitialPose().y + (entity instanceof Spider spider
                && SpiderCompanion.isTamed(spider) && SpiderCompanion.isSitting(spider) ? 2.0F : 0.0F);
    }
}
