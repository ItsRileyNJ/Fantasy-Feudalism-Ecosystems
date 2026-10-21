package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Silverfish.class)
public abstract class SilverfishMixin extends Monster {
    protected SilverfishMixin(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ffm$goals(CallbackInfo ci) {
        Silverfish fish = (Silverfish)(Object)this;
        goalSelector.addGoal(2, new AvoidEntityGoal<>(fish, Spider.class, 8.0F, 1.0, 1.2));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(fish, Zombie.class, true));
    }
}
