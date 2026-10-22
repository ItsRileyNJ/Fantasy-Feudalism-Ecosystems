package net.itsrileynj.fantasyfeudalismmobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Deterministic maximum growth; no fertilizer is spent on a mature crop or obstructed pitcher. */
public final class FertilizerItem extends Item {
    public FertilizerItem(Properties properties){super(properties);}
    public static boolean supports(BlockState state){
        return state.getBlock() instanceof CropBlock||state.getBlock() instanceof PitcherCropBlock
                ||state.getBlock() instanceof StemBlock||state.getBlock() instanceof NetherWartBlock
                ||state.getBlock() instanceof CocoaBlock||state.getBlock() instanceof SweetBerryBushBlock
                ||state.getBlock() instanceof SugarCaneBlock||state.getBlock() instanceof CactusBlock||state.is(BlockTags.CROPS);
    }
    @Override public InteractionResult useOn(UseOnContext context){
        var level=context.getLevel();var player=context.getPlayer();var pos=context.getClickedPos();
        if(player==null||player.isSpectator()||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
        if(!supports(level.getBlockState(pos)))return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(!grow((ServerLevel)level,pos))return InteractionResult.FAIL;
        context.getItemInHand().consume(1,player);level.levelEvent(1505,pos,0);return InteractionResult.CONSUME;
    }
    public static boolean grow(ServerLevel level,BlockPos pos){
        BlockState state=level.getBlockState(pos);Block block=state.getBlock();
        if(block instanceof PitcherCropBlock pitcher){
            if(state.getValue(PitcherCropBlock.HALF)==DoubleBlockHalf.UPPER){pos=pos.below();state=level.getBlockState(pos);}
            if(!state.is(Blocks.PITCHER_CROP)||state.getValue(PitcherCropBlock.AGE)>=4)return false;
            BlockState above=level.getBlockState(pos.above());if(!above.isAir()&&!above.is(Blocks.PITCHER_CROP))return false;
            for(int n=0;n<4;n++){
                BlockState before=level.getBlockState(pos);pitcher.performBonemeal(level,level.random,pos,before);
                if(level.getBlockState(pos).equals(before))break;
            }
            return !level.getBlockState(pos).equals(state);
        }
        if(block instanceof CropBlock crop){
            if(crop.isMaxAge(state))return false;
            level.setBlock(pos,crop.getStateForAge(crop.getMaxAge()),3);return true;
        }
        if(block instanceof SugarCaneBlock||block instanceof CactusBlock)return growColumn(level,pos,state);
        if(block instanceof StemBlock){
            boolean young=state.getValue(StemBlock.AGE)<7;level.setBlock(pos,state.setValue(StemBlock.AGE,7),3);
            Block fruit=state.is(Blocks.MELON_STEM)?Blocks.MELON:state.is(Blocks.PUMPKIN_STEM)?Blocks.PUMPKIN:null;
            Block attached=state.is(Blocks.MELON_STEM)?Blocks.ATTACHED_MELON_STEM:Blocks.ATTACHED_PUMPKIN_STEM;
            if(fruit!=null)for(Direction side:Direction.Plane.HORIZONTAL){
                BlockPos at=pos.relative(side);BlockState soil=level.getBlockState(at.below());
                if(level.isEmptyBlock(at)&&(soil.is(Blocks.FARMLAND)||soil.is(BlockTags.DIRT))){
                    level.setBlock(at,fruit.defaultBlockState(),3);level.setBlock(pos,attached.defaultBlockState().setValue(AttachedStemBlock.FACING,side),3);return true;
                }
            }
            return young;
        }
        if(!supports(state))return false;
        for(var property:state.getProperties())if(property instanceof IntegerProperty age&&property.getName().equals("age")){
            int maximum=age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
            if(state.getValue(age)>=maximum)return false;
            level.setBlock(pos,state.setValue(age,maximum),3);return true;
        }
        return false;
    }
    private static boolean growColumn(ServerLevel level,BlockPos clicked,BlockState state){
        Block block=state.getBlock();BlockPos base=clicked;
        while(base.getY()>level.getMinBuildHeight()&&level.getBlockState(base.below()).is(block))base=base.below();
        int height=1;while(height<3&&level.getBlockState(base.above(height)).is(block))height++;
        if(height>=3)return false;
        for(int n=height;n<3;n++){
            BlockPos at=base.above(n);if(!level.isEmptyBlock(at))return false;
            if(block instanceof CactusBlock)for(Direction side:Direction.Plane.HORIZONTAL)
                if(level.getBlockState(at.relative(side)).isSolid()||level.getFluidState(at.relative(side)).is(net.minecraft.tags.FluidTags.LAVA))return false;
        }
        for(int n=height;n<3;n++)level.setBlock(base.above(n),block.defaultBlockState(),3);
        return true;
    }
}
