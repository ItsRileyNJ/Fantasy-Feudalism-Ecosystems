package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.*;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreeperSurfaceTests {
    static boolean plant(BlockState s) { return s.is(Blocks.MOSS_CARPET)||s.is(Blocks.FERN)||s.is(Blocks.SHORT_GRASS); }
    static void safeAir(GameTestHelper h,BlockPos pos,BlockState before) {
        var after=h.getBlockState(pos);
        if(after==before)return;
        var absolute=h.absolutePos(pos);
        h.assertTrue(before.is(Blocks.AIR)&&plant(after)&&CreeperVegetation.suitableGround(h.getLevel(),absolute.below())&&after.canSurvive(h.getLevel(),absolute),"Only normal air directly above suitable ground may become a surviving single-block plant: "+pos);
    }
    private static void floor(GameTestHelper h) { for(int x=1;x<17;x++)for(int z=1;z<17;z++)h.setBlock(x,1,z,Blocks.GRASS_BLOCK); }
    private static void explode(GameTestHelper h,float radius) {
        var c=h.spawn(EntityType.CREEPER,8,2,8);c.setNoAi(true);h.getLevel().explode(c,c.getX(),c.getY(),c.getZ(),radius,Level.ExplosionInteraction.MOB);c.discard();
    }
    @GameTest(template="test_empty",batch="creeper_surface")
    public static void repeatedNormalAndChargedBlastsOnlyDecorateTheFirstAirBlock(GameTestHelper h) {
        floor(h);h.setBlock(8,2,8,Blocks.CAVE_AIR);h.setBlock(9,2,8,Blocks.VOID_AIR);h.setBlock(7,1,8,Blocks.AIR);h.setBlock(8,2,9,Blocks.POPPY);
        var air=new HashMap<BlockPos,BlockState>();
        for(var pos:BlockPos.betweenClosed(new BlockPos(1,1,1),new BlockPos(16,7,16)))if(h.getBlockState(pos).isAir())air.put(pos.immutable(),h.getBlockState(pos));
        for(float radius:new float[]{3,3,6,6}) {
            explode(h,radius);air.forEach((pos,state)->safeAir(h,pos,state));
            for(var pos:BlockPos.betweenClosed(new BlockPos(1,3,1),new BlockPos(16,7,16)))h.assertTrue(!plant(h.getBlockState(pos)),"No stacked or floating growth above the first surface air block");
        }
        int count=0;for(var pos:BlockPos.betweenClosed(new BlockPos(2,2,2),new BlockPos(14,2,14)))if(plant(h.getBlockState(pos)))count++;
        h.assertTrue(count>0,"Real creeper explosions restore surface vegetation");
        h.assertBlockPresent(Blocks.POPPY,new BlockPos(8,2,9));h.assertBlockPresent(Blocks.AIR,new BlockPos(7,1,8));h.succeed();
    }
    @GameTest(template="test_empty",batch="creeper_surface")
    public static void explosionsLeaveWaterFarmlandAndCropSpacesFreeOfVegetation(GameTestHelper h) {
        floor(h);h.setBlock(6,1,8,Blocks.WATER);h.setBlock(7,1,8,Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true));
        h.setBlock(8,2,9,Blocks.WATER);h.setBlock(9,1,8,Blocks.FARMLAND);h.setBlock(9,2,8,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,1));h.setBlock(10,1,8,Blocks.FARMLAND);
        h.setBlock(10,1,7,Blocks.OAK_LEAVES);h.setBlock(6,1,7,Blocks.DIAMOND_BLOCK);
        explode(h,6);
        h.assertBlockPresent(Blocks.WATER,new BlockPos(6,1,8));h.assertBlockPresent(Blocks.WATER,new BlockPos(8,2,9));
        for(var pos:List.of(new BlockPos(6,2,8),new BlockPos(7,2,8),new BlockPos(9,3,8),new BlockPos(10,2,8),new BlockPos(10,2,7),new BlockPos(6,2,7)))h.assertTrue(h.getBlockState(pos).isAir(),"No vegetation on water, waterlogged blocks, crops, farmland, leaves or player valuables: "+pos);
        h.assertTrue(h.getBlockState(new BlockPos(9,2,8)).is(Blocks.WHEAT)&&h.getBlockState(new BlockPos(9,2,8)).getValue(CropBlock.AGE)==3,"Existing crop still gains two stages without replacement");h.succeed();
    }
}
