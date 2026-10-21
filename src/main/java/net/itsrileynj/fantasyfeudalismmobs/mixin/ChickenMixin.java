package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chicken.class)
public abstract class ChickenMixin extends Animal {
    protected ChickenMixin(EntityType<? extends Animal> type, Level level) { super(type, level); }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ffm$fleePlayers(CallbackInfo ci) {
        // Mob movement uses speed both as acceleration and forward input. Players use
        // unit forward input, so the square root is required to match ground velocity.
        double speedModifier = Math.sqrt(0.1 * 1.3 * 0.95) / 0.25;
        goalSelector.addGoal(1, new AvoidEntityGoal<>((Chicken)(Object)this,
                Player.class, 3.0F, speedModifier, speedModifier));
    }
}
