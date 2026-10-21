package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.FeudalConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal")
public abstract class EndermanPickupMixin {
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void ffm$configurablePickup(CallbackInfoReturnable<Boolean> cir) {
        if (!FeudalConfig.ENDERMAN_BLOCK_PICKUP.get()) cir.setReturnValue(false);
    }
}
