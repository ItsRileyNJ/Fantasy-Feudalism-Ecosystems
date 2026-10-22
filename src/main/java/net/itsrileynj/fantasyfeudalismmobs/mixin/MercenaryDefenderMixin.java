package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.SpiderCompanion;
import net.itsrileynj.fantasyfeudalismmobs.PhantomPact;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional Mercenaries integration: vanilla spider entities represent our tamed pets. */
@Pseudo
@Mixin(targets="dev.villagerscontract.Defender", remap=false)
public abstract class MercenaryDefenderMixin {
    @org.spongepowered.asm.mixin.Shadow(remap=false) public abstract java.util.UUID redeemer();
    @Inject(method="isHostile", at=@At("HEAD"), cancellable=true, remap=false)
    private static void ffm$companionIsNotHostile(LivingEntity target, CallbackInfoReturnable<Boolean> result) {
        if ((SpiderCompanion.isTamed(target)||PhantomPact.bound(target))) result.setReturnValue(false);
    }

    @Inject(method="isAlliedTo", at=@At("HEAD"), cancellable=true, remap=false)
    private void ffm$companionIsAnAlly(Entity target, CallbackInfoReturnable<Boolean> result) {
        if (SpiderCompanion.isTamed(target)) result.setReturnValue(true);
        else if(target instanceof net.minecraft.world.entity.monster.Phantom phantom&&PhantomPact.bound(phantom)
                &&java.util.Objects.equals(redeemer(),PhantomPact.ownerId(phantom)))result.setReturnValue(true);
    }
}
