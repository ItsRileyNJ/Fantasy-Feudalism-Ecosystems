package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.SpiderCompanion;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Spider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntitySoundMixin {
    @Inject(method = "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", at = @At("HEAD"), cancellable = true)
    private void ffm$quietCompanion(SoundEvent sound, float volume, float pitch, CallbackInfo ci) {
        if ((Object)this instanceof Spider spider && SpiderCompanion.isTamed(spider) && !SpiderCompanion.hostileNearby(spider)) ci.cancel();
    }
}
