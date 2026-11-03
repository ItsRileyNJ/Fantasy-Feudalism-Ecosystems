package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.*;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CropGrowthTests {
    private static void floor(GameTestHelper h) {
        for(int x=3;x<18;x++)for(int z=3;z<18;z++)h.setBlock(x,1,z,Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7));
    }
    private static BlockPos plot(GameTestHelper h) {
        floor(h);h.setBiome(Biomes.PLAINS);h.setDayTime(6000);
        for(int y=3;y<20;y++)h.setBlock(8,y,8,Blocks.AIR);
        return h.absolutePos(new BlockPos(8,2,8));
    }
    private static void weather(ServerLevel level,boolean wet) { level.setWeatherParameters(wet?0:6000,wet?6000:0,wet,false);level.setRainLevel(wet?1:0); }
    private static int grows(ServerLevel level,BlockPos pos,BlockState state,int count,long seed) {
        var random=RandomSource.create(seed);int grown=0;
        for(int i=0;i<count;i++) {
            level.setBlock(pos,state,2);state.randomTick(level,pos,random);
            if(!level.getBlockState(pos).equals(state))grown++;
        }
        return grown;
    }
    private static void explode(GameTestHelper h,boolean moss) {
        var creeper=h.spawn(EntityType.CREEPER,8,2,8);creeper.setNoAi(true);
        boolean before=FeudalConfig.CREEPER_BLOCK_CONVERSION.get();FeudalConfig.CREEPER_BLOCK_CONVERSION.set(moss);
        try { h.getLevel().explode(creeper,creeper.getX(),creeper.getY(),creeper.getZ(),3,Level.ExplosionInteraction.MOB); }
        finally { FeudalConfig.CREEPER_BLOCK_CONVERSION.set(before);creeper.discard(); }
    }

    @GameTest(template="test_empty",batch="rain_growth")
    public static void exposedWheatHasFifteenPercentMoreNativeGrowth(GameTestHelper h) {
        var pos=plot(h);var level=h.getLevel();var state=Blocks.WHEAT.defaultBlockState();
        try {
            weather(level,false);int dry=grows(level,pos,state,30000,82735);
            weather(level,true);h.assertTrue(CropGrowth.rainedOn(level,pos),"The test crop is actually exposed to rain");
            int wet=grows(level,pos,state,30000,82735);double ratio=(double)wet/dry;
            h.assertTrue(ratio>1.115&&ratio<1.185,"Native rainy growth should average 15% faster: dry="+dry+" rain="+wet+" ratio="+ratio);h.succeed();
        } finally { weather(level,false); }
    }

    @GameTest(template="test_empty",batch="rain_growth")
    public static void glassRoofAndRainlessDesertKeepExactDryGrowth(GameTestHelper h) {
        var pos=plot(h);var level=h.getLevel();var state=Blocks.CARROTS.defaultBlockState();
        try {
            weather(level,false);int dry=grows(level,pos,state,5000,61);
            weather(level,true);h.setBlock(8,5,8,Blocks.GLASS);
            h.assertFalse(CropGrowth.rainedOn(level,pos),"Glass intercepts rainfall");
            h.assertTrue(grows(level,pos,state,5000,61)==dry,"Covered crops retain the exact vanilla random sequence");
            h.setBlock(8,5,8,Blocks.AIR);h.setBiome(Biomes.DESERT);
            h.assertFalse(CropGrowth.rainedOn(level,pos),"Global rain does not water a rainless biome");
            h.assertTrue(grows(level,pos,state,5000,61)==dry,"Rainless biomes retain exact dry growth");h.succeed();
        } finally { weather(level,false); }
    }

    public static final class DenyGrowth {
        private final BlockPos pos;DenyGrowth(BlockPos pos){this.pos=pos;}
        @SubscribeEvent public void deny(CropGrowEvent.Pre event) {
            if(event.getPos().equals(pos))event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        }
    }
    @GameTest(template="test_empty",batch="rain_growth")
    public static void rainRespectsGrowthDenialAndMatureCrops(GameTestHelper h) {
        var pos=plot(h);var level=h.getLevel();var probe=new DenyGrowth(pos);NeoForge.EVENT_BUS.register(probe);
        try {
            weather(level,true);
            h.assertTrue(grows(level,pos,Blocks.WHEAT.defaultBlockState(),2000,42)==0,"Rain must not bypass another mod denying growth");
            h.assertTrue(grows(level,pos,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7),2000,42)==0,"Mature wheat cannot overgrow");h.succeed();
        } finally { NeoForge.EVENT_BUS.unregister(probe);weather(level,false); }
    }

    @GameTest(template="test_empty",batch="rain_growth")
    public static void nativeBeetrootAndTorchflowerGatesStillWorkInRain(GameTestHelper h) {
        var pos=plot(h);var level=h.getLevel();
        try {
            for(Block block:List.of(Blocks.BEETROOTS,Blocks.TORCHFLOWER_CROP,Blocks.PUMPKIN_STEM,Blocks.PITCHER_CROP)) {
                weather(level,false);int dry=grows(level,pos,block.defaultBlockState(),20000,1234);
                weather(level,true);int wet=grows(level,pos,block.defaultBlockState(),20000,1234);double ratio=(double)wet/dry;
                h.assertTrue(dry>0&&ratio>1.09&&ratio<1.21,"Rain preserves native subtype gates while boosting growth for "+block+": "+dry+" -> "+wet); 
            }
            h.succeed();
        } finally { weather(level,false); }
    }

    @GameTest(template="test_empty",batch="rain_growth")
    public static void rainySugarCaneAndCactiGetFifteenPercentExtraNativeAgeTicks(GameTestHelper h) {
        var pos=plot(h);var level=h.getLevel();
        try {
            weather(level,true);h.setBlock(8,1,8,Blocks.SAND);h.setBlock(9,1,8,Blocks.WATER);
            for(Block block:List.of(Blocks.SUGAR_CANE,Blocks.CACTUS)) {
                var state=block.defaultBlockState();var age=block==Blocks.SUGAR_CANE?SugarCaneBlock.AGE:CactusBlock.AGE;
                var random=RandomSource.create(719);int total=0;
                for(int i=0;i<10000;i++) {
                    level.setBlock(pos,state,2);state.randomTick(level,pos,random);total+=level.getBlockState(pos).getValue(age);
                }
                h.assertTrue(total>11300&&total<11700,"Rain adds 15% native age ticks without recursion for "+block+": "+total);
                h.setBlock(8,5,8,Blocks.GLASS);level.setBlock(pos,state,2);state.randomTick(level,pos,random);
                h.assertTrue(level.getBlockState(pos).getValue(age)==1,"Covered columns advance exactly once");h.setBlock(8,5,8,Blocks.AIR);
            }
            h.succeed();
        } finally { weather(level,false); }
    }

    @GameTest(template="test_empty",batch="creeper_crop_growth")
    public static void realCreeperExplosionAddsTwoStagesAndCapsAtMaturity(GameTestHelper h) {
        floor(h);h.setBlock(8,2,8,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,1));
        h.setBlock(9,2,8,Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE,6));
        h.setBlock(7,2,8,Blocks.BEETROOTS.defaultBlockState().setValue(BeetrootBlock.AGE,1));
        h.setBlock(8,2,9,Blocks.POTATOES.defaultBlockState());h.setBlock(8,2,7,Blocks.MELON_STEM.defaultBlockState().setValue(StemBlock.AGE,6));
        explode(h,true);
        h.assertTrue(h.getBlockState(new BlockPos(8,2,8)).getValue(CropBlock.AGE)==3,"Wheat gains exactly two stages");
        h.assertTrue(h.getBlockState(new BlockPos(9,2,8)).getValue(CropBlock.AGE)==7,"Carrots cap at maturity");
        h.assertTrue(h.getBlockState(new BlockPos(7,2,8)).getValue(BeetrootBlock.AGE)==3,"Beetroot gains two of its native stages");
        h.assertTrue(h.getBlockState(new BlockPos(8,2,9)).getValue(CropBlock.AGE)==2,"Potatoes gain two stages");
        h.assertTrue(h.getBlockState(new BlockPos(8,2,7)).getValue(StemBlock.AGE)==7,"Stems mature without creating fruit");h.succeed();
    }

    @GameTest(template="test_empty",batch="creeper_crop_growth")
    public static void creeperGrowthPreservesAirAndRespectsRadius(GameTestHelper h) {
        floor(h);h.setBlock(8,2,8,Blocks.WHEAT.defaultBlockState());h.setBlock(15,2,8,Blocks.WHEAT.defaultBlockState());
        h.setBlock(9,2,8,Blocks.PITCHER_CROP.defaultBlockState().setValue(PitcherCropBlock.AGE,1));
        var empty=new HashMap<BlockPos,BlockState>();
        for(BlockPos p:BlockPos.betweenClosed(new BlockPos(3,2,3),new BlockPos(16,6,13)))if(h.getBlockState(p).isAir())empty.put(p.immutable(),h.getBlockState(p));
        explode(h,true);empty.forEach((p,s)->h.assertTrue(h.getBlockState(p)==s,"Crop growth cannot replace any air at "+p));
        h.assertTrue(h.getBlockState(new BlockPos(15,2,8)).getValue(CropBlock.AGE)==0,"Crops outside the blast radius stay unchanged");
        h.assertTrue(h.getBlockState(new BlockPos(9,2,8)).getValue(PitcherCropBlock.AGE)==2,"A single-cell pitcher stops before it would need a new upper cell");h.succeed();
    }

    @GameTest(template="test_empty",batch="creeper_crop_growth")
    public static void disablingMossConversionStillAllowsCropGrowth(GameTestHelper h) {
        floor(h);h.setBlock(8,2,8,Blocks.WHEAT.defaultBlockState());h.setBlock(9,1,8,Blocks.COBBLESTONE);
        explode(h,false);
        h.assertTrue(h.getBlockState(new BlockPos(8,2,8)).getValue(CropBlock.AGE)==2,"The moss-only toggle does not disable crop growth");
        h.assertTrue(h.getBlockState(new BlockPos(9,1,8)).is(Blocks.COBBLESTONE),"Moss conversion remains disabled");h.succeed();
    }

    @GameTest(template="test_empty",batch="creeper_crop_no_grief")
    public static void mobGriefingPreventsCreeperCropGrowth(GameTestHelper h) {
        floor(h);h.setBlock(8,2,8,Blocks.WHEAT.defaultBlockState());var level=h.getLevel();boolean old=level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false,level.getServer());
        try { explode(h,true);h.assertTrue(h.getBlockState(new BlockPos(8,2,8)).getValue(CropBlock.AGE)==0,"No crop mutation when mob griefing is disabled");h.succeed(); }
        finally { level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(old,level.getServer()); }
    }

    @GameTest(template="test_empty",batch="creeper_crop_growth")
    public static void existingTallPitchersGrowBothHalvesOnceAndCocoaKeepsItsFacing(GameTestHelper h) {
        floor(h);h.setBlock(8,2,8,Blocks.PITCHER_CROP.defaultBlockState().setValue(PitcherCropBlock.AGE,3));
        h.setBlock(8,3,8,Blocks.PITCHER_CROP.defaultBlockState().setValue(PitcherCropBlock.AGE,3).setValue(PitcherCropBlock.HALF,DoubleBlockHalf.UPPER));
        h.setBlock(10,2,8,Blocks.JUNGLE_LOG);h.setBlock(9,2,8,Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING,Direction.EAST));
        explode(h,true);
        h.assertTrue(h.getBlockState(new BlockPos(8,2,8)).getValue(PitcherCropBlock.AGE)==4&&h.getBlockState(new BlockPos(8,3,8)).getValue(PitcherCropBlock.AGE)==4,"Existing pitcher halves mature consistently");
        var cocoa=h.getBlockState(new BlockPos(9,2,8));h.assertTrue(cocoa.getValue(CocoaBlock.AGE)==2&&cocoa.getValue(CocoaBlock.FACING)==Direction.EAST,"Cocoa matures without losing its support direction");h.succeed();
    }
}
