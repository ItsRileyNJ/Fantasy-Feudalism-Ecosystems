package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.HorseLife;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.Horse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Horse.class)
public abstract class HorseMatingMixin {
    @Inject(method="canMate", at=@At("HEAD"), cancellable=true)
    private void ffm$wildBreeding(Animal other, CallbackInfoReturnable<Boolean> ci) {
        if (other instanceof Horse mate && HorseLife.automaticPair((Horse)(Object)this, mate))
            ci.setReturnValue(true);
    }
}
