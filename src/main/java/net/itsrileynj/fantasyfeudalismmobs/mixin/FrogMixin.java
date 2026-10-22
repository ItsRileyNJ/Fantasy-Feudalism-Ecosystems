package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.FrogWaterEscape;
import net.minecraft.world.entity.animal.frog.Frog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Frog.class)
public abstract class FrogMixin {
    @Unique private final FrogWaterEscape ffm$waterEscape=new FrogWaterEscape();
    @Inject(method="customServerAiStep",at=@At("HEAD"),cancellable=true)
    private void ffm$escapeToWater(CallbackInfo ci){if(ffm$waterEscape.tick((Frog)(Object)this))ci.cancel();}
}
