package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.VariantSlime;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlock.class)
public abstract class FlameSlimeLavaMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void ffm$flameSlimeSurface(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context,
                                       CallbackInfoReturnable<VoxelShape> result) {
        var fluid = state.getFluidState();
        if (fluid.is(FluidTags.LAVA) && context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof VariantSlime slime && slime.isFlame()
                && !level.getFluidState(pos.above()).is(FluidTags.LAVA)) {
            var surface = Shapes.box(0, 0, 0, 1, fluid.getHeight(level, pos), 1);
            result.setReturnValue(context.isAbove(surface, pos, true) ? surface : Shapes.empty());
        }
    }
}
