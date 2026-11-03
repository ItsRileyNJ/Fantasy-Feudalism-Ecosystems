package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.CropGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({CropBlock.class,StemBlock.class,PitcherCropBlock.class,NetherWartBlock.class,CocoaBlock.class,SweetBerryBushBlock.class})
public abstract class RainCropGrowthMixin {
    @Redirect(method="randomTick",at=@At(value="INVOKE",target="Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int ffm$rainGrowthChance(RandomSource random,int bound,BlockState state,ServerLevel level,BlockPos pos,RandomSource tickRandom) {
        return CropGrowth.rainRoll(random,bound,level,pos);
    }
}
