package net.itsrileynj.fantasyfeudalismmobs.rootworm.mixin;

import net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWormPart;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Parts handle targeting; arrows apply their ordinary living-mob damage, fire and potion effects to the worm. */
@Mixin(value = AbstractArrow.class, remap = false)
public abstract class RootWormArrowMixin {
    @ModifyVariable(method = "onHitEntity", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1, remap = false)
    private EntityHitResult rootWorm$resolveWormSegment(EntityHitResult hit) {
        return hit.getEntity() instanceof RootWormPart part ? new EntityHitResult(part.getParent(), hit.getLocation()) : hit;
    }
}
