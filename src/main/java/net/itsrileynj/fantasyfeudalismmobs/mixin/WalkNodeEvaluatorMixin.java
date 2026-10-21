package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Advertise three-block edges to the search without giving the entity three-block auto-step. */
@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin extends NodeEvaluator {
    @Redirect(method = "getNeighbors", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;maxUpStep()F"))
    private float ffm$searchJump(Mob mob) { return mob instanceof Spider ? 3.0F : mob.maxUpStep(); }

    @Inject(method = "getMobJumpHeight", at = @At("HEAD"), cancellable = true)
    private void ffm$jumpHeight(CallbackInfoReturnable<Double> cir) {
        if (mob instanceof Spider) cir.setReturnValue(3.02D);
    }
}
