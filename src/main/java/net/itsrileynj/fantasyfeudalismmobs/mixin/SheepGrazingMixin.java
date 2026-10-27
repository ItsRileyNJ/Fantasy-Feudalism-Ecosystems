package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.SheepState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.animal.Sheep;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EatBlockGoal.class)
public abstract class SheepGrazingMixin {
    @Shadow @Final private Mob mob;

    @Inject(method={"canUse","canContinueToUse"}, at=@At("HEAD"), cancellable=true)
    private void ffm$tooColdToGraze(CallbackInfoReturnable<Boolean> cir) {
        if (mob instanceof SheepState sheep && sheep.ffm$isCold()) cir.setReturnValue(false);
    }

    @Inject(method="tick", at=@At("HEAD"), cancellable=true)
    private void ffm$interruptColdMeal(CallbackInfo ci) {
        if (mob instanceof SheepState sheep && sheep.ffm$isCold()) {
            ((EatBlockGoal)(Object)this).stop();
            ci.cancel();
        }
    }

    @Redirect(method="canUse", at=@At(value="INVOKE", target="Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int ffm$grazeThreeTimesAsOften(RandomSource random, int bound) {
        int roll=random.nextInt(bound);
        return mob instanceof Sheep ? (roll<3?0:1) : roll;
    }
}
