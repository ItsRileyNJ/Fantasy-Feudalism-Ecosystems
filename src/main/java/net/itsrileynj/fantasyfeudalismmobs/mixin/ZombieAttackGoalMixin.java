package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Keep pursuit active between short routes; vanilla target goals still enforce visibility and range. */
@Mixin(ZombieAttackGoal.class)
public abstract class ZombieAttackGoalMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/goal/MeleeAttackGoal;<init>(Lnet/minecraft/world/entity/PathfinderMob;DZ)V"), index = 2)
    private static boolean ffm$continuousPursuit(boolean original) {
        return true;
    }
}
