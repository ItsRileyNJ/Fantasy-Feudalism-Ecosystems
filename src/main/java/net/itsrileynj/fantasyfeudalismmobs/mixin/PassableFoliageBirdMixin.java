package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.ForestBird;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Use Passable Foliage's native collision and pathfinding support only while grounded. */
@Pseudo
@Mixin(targets="snownee.passablefoliage.PassableFoliage",remap=false)
public abstract class PassableFoliageBirdMixin {
    @Inject(method="hasLeafWalker",at=@At("HEAD"),cancellable=true,remap=false)
    private static void ffm$perchingBird(LivingEntity entity,CallbackInfoReturnable<Boolean> result) {
        if(entity instanceof ForestBird bird)result.setReturnValue(!bird.flightActive());
    }
}
