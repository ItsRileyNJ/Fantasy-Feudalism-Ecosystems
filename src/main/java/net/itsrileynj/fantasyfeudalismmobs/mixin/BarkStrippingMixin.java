package net.itsrileynj.fantasyfeudalismmobs.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ItemAbilities;
import org.spongepowered.asm.mixin.Mixin;

/** Wait for NeoForge's placement/protection hooks to finish before rewarding a committed strip. */
@Mixin(ItemStack.class)
public abstract class BarkStrippingMixin {
    @WrapMethod(method="useOn")
    private InteractionResult ffm$barkAfterStripping(UseOnContext context, Operation<InteractionResult> original) {
        if(net.itsrileynj.fantasyfeudalismmobs.BarkCompatibility.usesFarmersDelight())return original.call(context);
        var level=context.getLevel();var stack=(ItemStack)(Object)this;
        var before=level.getBlockState(context.getClickedPos());
        var stripped=!level.isClientSide && stack.canPerformAction(ItemAbilities.AXE_STRIP)
                ? AxeItem.getAxeStrippingState(before) : null;
        var result=original.call(context);
        if(stripped!=null && !before.equals(stripped) && result.consumesAction()
                && level.getBlockState(context.getClickedPos()).equals(stripped)) {
            Block.popResource(level,context.getClickedPos(),new ItemStack(FantasyFeudalismMobs.BARK.get()));
        }
        return result;
    }
}
