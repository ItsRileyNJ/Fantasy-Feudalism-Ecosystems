package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.SheepState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class SheepRendererMixin {
    // Hook the erased LivingEntity signature actually called by setupRotations.
    // A new isShaking(Sheep) overload on SheepRenderer is not dispatched by that call.
    @Inject(method="isShaking",at=@At("HEAD"),cancellable=true)
    private void ffm$coldSheepShiver(LivingEntity entity,CallbackInfoReturnable<Boolean> cir) {
        if(entity instanceof Sheep sheep&&((SheepState)sheep).ffm$isCold())cir.setReturnValue(true);
    }
}
