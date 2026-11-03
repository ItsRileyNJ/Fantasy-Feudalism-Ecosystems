package net.itsrileynj.fantasyfeudalismmobs;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class CropGrowth {
    private CropGrowth() {}

    public static boolean rainedOn(ServerLevel level, BlockPos pos) {
        return level.isRaining() && level.isRainingAt(pos.above());
    }

    /** Increase the native winning probability from 1/bound to 1.15/bound. */
    public static int rainRoll(RandomSource random, int bound, ServerLevel level, BlockPos pos) {
        int roll=random.nextInt(bound);
        if(roll!=0 && bound>1 && rainedOn(level,pos) && random.nextDouble()<.15/(bound-1))return 0;
        return roll;
    }

    /** Advance only already-occupied crop cells; never create a block in air. */
    public static boolean advanceExisting(Level level, BlockPos pos, int stages) {
        if(level.isClientSide || stages<=0)return false;
        BlockState state=level.getBlockState(pos);Block block=state.getBlock();
        if(block instanceof PitcherCropBlock) {
            if(state.getValue(PitcherCropBlock.HALF)==DoubleBlockHalf.UPPER)return false;
            int age=state.getValue(PitcherCropBlock.AGE);
            BlockState above=level.getBlockState(pos.above());
            boolean hasUpper=above.is(block)&&above.getValue(PitcherCropBlock.HALF)==DoubleBlockHalf.UPPER;
            // Forming a new upper half would fill air, so young single-block pitchers stop at age two.
            int next=Math.min(age+stages,hasUpper?4:2);
            if(next<=age)return false;
            level.setBlock(pos,state.setValue(PitcherCropBlock.AGE,next),Block.UPDATE_ALL);
            if(hasUpper)level.setBlock(pos.above(),above.setValue(PitcherCropBlock.AGE,next),Block.UPDATE_ALL);
            return true;
        }
        if(block instanceof CropBlock crop) {
            int age=crop.getAge(state),next=Math.min(age+stages,crop.getMaxAge());
            if(next<=age)return false;
            level.setBlock(pos,crop.getStateForAge(next),Block.UPDATE_ALL);return true;
        }
        if(!(block instanceof StemBlock || block instanceof NetherWartBlock || block instanceof CocoaBlock
                || block instanceof SweetBerryBushBlock || block instanceof SugarCaneBlock
                || block instanceof CactusBlock || state.is(BlockTags.CROPS)))return false;
        for(var property:state.getProperties())if(property instanceof IntegerProperty age&&property.getName().equals("age")) {
            int maximum=age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
            int before=state.getValue(age),next=Math.min(before+stages,maximum);
            if(next<=before)return false;
            level.setBlock(pos,state.setValue(age,next),Block.UPDATE_ALL);return true;
        }
        return false;
    }
}
