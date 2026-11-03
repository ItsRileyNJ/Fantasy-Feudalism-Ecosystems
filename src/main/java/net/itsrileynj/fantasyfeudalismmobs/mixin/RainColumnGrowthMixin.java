package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.CropGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SugarCaneBlock.class,CactusBlock.class})
public abstract class RainColumnGrowthMixin {
    @Unique private static final ThreadLocal<Boolean> FFM_RAIN_BONUS=ThreadLocal.withInitial(()->false);

    @Inject(method="randomTick",at=@At("TAIL"))
    private void ffm$extraRainTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random,CallbackInfo ci) {
        if(FFM_RAIN_BONUS.get())return;
        // Re-run native growth checks, including height, space, survival and other mods' permission hooks.
        BlockPos target=pos;
        BlockState now=level.getBlockState(pos);
        if(!now.is(state.getBlock()))return;
        if(level.getBlockState(pos.above()).is(state.getBlock())) {
            // A successful native height increase puts the active growing tip one cell higher.
            if(state.hasProperty(SugarCaneBlock.AGE)&&state.getValue(SugarCaneBlock.AGE)==15&&now.getValue(SugarCaneBlock.AGE)==0)target=pos.above();
            else if(state.hasProperty(CactusBlock.AGE)&&state.getValue(CactusBlock.AGE)==15&&now.getValue(CactusBlock.AGE)==0)target=pos.above();
        }
        if(!CropGrowth.rainedOn(level,target)||random.nextFloat()>=.15F)return;
        FFM_RAIN_BONUS.set(true);
        try { level.getBlockState(target).randomTick(level,target,random); }
        finally { FFM_RAIN_BONUS.remove(); }
    }
}
