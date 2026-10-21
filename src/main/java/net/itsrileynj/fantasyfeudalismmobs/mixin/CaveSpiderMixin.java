package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.SpiderCompanion;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.CaveSpider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(CaveSpider.class)
public abstract class CaveSpiderMixin {
    @Redirect(method = "doHurtTarget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean ffm$replaceTamedPoison(LivingEntity target, MobEffectInstance effect, Entity source) {
        return !SpiderCompanion.isTamed((Entity)(Object)this) && target.addEffect(effect, source);
    }
}
