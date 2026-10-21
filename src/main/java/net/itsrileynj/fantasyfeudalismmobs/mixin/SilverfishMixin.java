package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.SilverfishBurrowing;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Silverfish.class)
public abstract class SilverfishMixin extends Monster implements SilverfishBurrowing.EffectTicker {
    protected SilverfishMixin(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Override
    public void ffm$tickBurrowEffects() { tickEffects(); }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ffm$goals(CallbackInfo ci) {
        Silverfish fish = (Silverfish)(Object)this;
        goalSelector.addGoal(2, new AvoidEntityGoal<>(fish, Spider.class, 8.0F, 1.0, 1.2));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(fish, Zombie.class, true));
    }
}
