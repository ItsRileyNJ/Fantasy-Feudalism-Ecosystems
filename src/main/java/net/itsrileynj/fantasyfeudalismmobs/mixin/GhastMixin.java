package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.minecraft.world.entity.monster.Ghast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Ghast.class)
public abstract class GhastMixin {
    @Inject(method = "getMaxSpawnClusterSize", at = @At("HEAD"), cancellable = true)
    private void ffm$threeGhasts(CallbackInfoReturnable<Integer> cir) { cir.setReturnValue(3); }
}
