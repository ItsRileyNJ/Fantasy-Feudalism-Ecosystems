package net.itsrileynj.fantasyfeudalismmobs.gametest;

import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SheepCareTests {
    private static Sheep sheep(GameTestHelper h) {
        // Native GameTests encase structures in a barrier roof. Open the test column
        // so outdoor weather cases genuinely see the sky.
        for(int y=6;y<20;y++)h.setBlock(8,y,8,Blocks.AIR);
        h.setBlock(8,1,8,Blocks.GRASS_BLOCK);
        h.setBlock(8,5,8,Blocks.GLASS);
        var sheep=h.spawn(EntityType.SHEEP,8,2,8);
        sheep.setNoAi(true);sheep.setOnGround(true);sheep.setAge(0);sheep.setSheared(true);
        return sheep;
    }
    private static SheepState state(Sheep sheep) { return (SheepState)sheep; }
    private static void meal(GameTestHelper h,Sheep sheep) {
        h.setBlock(8,1,8,Blocks.GRASS_BLOCK);
        var goal=new EatBlockGoal(sheep);goal.start();
        for(int i=0;i<40;i++)goal.tick();goal.stop();
    }
    private static void refresh(Sheep sheep) {
        // Exercise the production staggered weather update, including NoAI sheep.
        while((sheep.tickCount+sheep.getId())%20!=0)sheep.tickCount++;
        sheep.aiStep();
    }

    @GameTest(template="test_empty")
    public static void threeRealGrassMealsRestoreTheOriginalWoolColor(GameTestHelper h) {
        var sheep=sheep(h);sheep.setColor(DyeColor.BLUE);
        for(int i=1;i<=3;i++) {
            meal(h,sheep);
            h.assertTrue(sheep.isSheared()==(i<3),"Wool only returns after the third completed native graze");
            h.assertTrue(h.getBlockState(new BlockPos(8,1,8)).is(Blocks.DIRT),"Each successful graze consumes native grass");
        }
        h.assertTrue(sheep.getColor()==DyeColor.BLUE&&state(sheep).ffm$grazes()==0,"Color survives and the next cycle starts fresh");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void shortGrassCountsButInterruptedOrMissingGrassDoesNot(GameTestHelper h) {
        var sheep=sheep(h);h.setBlock(8,2,8,Blocks.SHORT_GRASS);meal(h,sheep);
        h.assertTrue(state(sheep).ffm$grazes()==1&&h.getBlockState(new BlockPos(8,2,8)).isAir(),"Native short grass supplies one meal");
        var goal=new EatBlockGoal(sheep);goal.start();goal.tick();goal.stop();
        h.setBlock(8,1,8,Blocks.DIRT);goal.start();for(int i=0;i<40;i++)goal.tick();goal.stop();
        h.assertTrue(state(sheep).ffm$grazes()==1,"Interrupted animations and absent grass award no wool progress");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void woolProgressSurvivesSaveAndReloadAndNextShearingResetsIt(GameTestHelper h) {
        var sheep=sheep(h);meal(h,sheep);meal(h,sheep);
        var tag=sheep.saveWithoutId(new CompoundTag());sheep.discard();
        var restored=EntityType.SHEEP.create(h.getLevel());restored.load(tag);h.getLevel().addFreshEntity(restored);
        h.assertTrue(restored.isSheared()&&state(restored).ffm$grazes()==2,"Two saved grazes survive reload");
        meal(h,restored);h.assertFalse(restored.isSheared(),"One more meal restores saved wool");
        restored.setSheared(true);meal(h,restored);
        h.assertTrue(restored.isSheared()&&state(restored).ffm$grazes()==1,"Newly sheared wool needs a fresh three meals");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void oldSavesAndInvalidProgressAreSafe(GameTestHelper h) {
        var sheep=sheep(h);var tag=sheep.saveWithoutId(new CompoundTag());tag.remove("ffm_wool_grazes");sheep.load(tag);
        h.assertTrue(state(sheep).ffm$grazes()==0,"Old saves begin at zero");
        tag.putInt("ffm_wool_grazes",999);sheep.load(tag);h.assertTrue(state(sheep).ffm$grazes()==2,"Invalid progress is bounded");
        tag.putBoolean("Sheared",false);sheep.load(tag);h.assertTrue(state(sheep).ffm$grazes()==0,"Wooled sheep carry no progress");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void babyGrowthAndUnrelatedAnimalsKeepNativeBehavior(GameTestHelper h) {
        var sheep=sheep(h);sheep.setSheared(false);sheep.setAge(-24000);sheep.ate();
        h.assertTrue(sheep.getAge()==-22800&&!sheep.isSheared(),"Native baby grazing growth remains sixty seconds");
        var pig=h.spawn(EntityType.PIG,8,2,8);pig.setNoAi(true);var goal=new EatBlockGoal(pig);
        pig.getRandom().setSeed(42);var expected=RandomSource.create(42);
        for(int i=0;i<3000;i++)h.assertTrue(goal.canUse()==(expected.nextInt(1000)==0),"Other species retain the native roll");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void adultsGetThirtyWinningRollsAndLambsKeepThree(GameTestHelper h) {
        var sheep=sheep(h);var goal=new EatBlockGoal(sheep);
        for(boolean baby:new boolean[]{false,true}) {
            sheep.setBaby(baby);sheep.getRandom().setSeed(12345);var expected=RandomSource.create(12345);
            for(int i=0;i<6000;i++)h.assertTrue(goal.canUse()==(expected.nextInt(baby?50:1000)<(baby?3:30)),"Adults use 30 in 1000; lambs retain 3 in 50");
        }
        h.succeed();
    }

    @GameTest(template="test_empty",batch="sheep_no_grief")
    public static void noGriefingStillCountsMealsWithoutDestroyingGrass(GameTestHelper h) {
        boolean original=h.getLevel().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        h.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false,h.getLevel().getServer());
        try {
            var sheep=sheep(h);for(int i=0;i<3;i++)meal(h,sheep);
            h.assertFalse(sheep.isSheared(),"Native no-griefing feeding still restores wool after three meals");
            h.assertTrue(h.getBlockState(new BlockPos(8,1,8)).is(Blocks.GRASS_BLOCK),"No-griefing grass remains intact");h.succeed();
        } finally { h.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(original,h.getLevel().getServer()); }
    }

    @GameTest(template="test_empty",batch="sheep_weather")
    public static void exposureRainAndShelterControlColdWithoutCausingDamage(GameTestHelper h) {
        var level=h.getLevel();boolean raining=level.getLevelData().isRaining();boolean thunder=level.getLevelData().isThundering();float rain=level.getRainLevel(1);
        try {
            level.setWeatherParameters(6000,0,false,false);level.setRainLevel(0);
            h.setBiome(Biomes.SNOWY_PLAINS);var sheep=sheep(h);float health=sheep.getHealth();
            h.assertFalse(state(sheep).ffm$isCold(),"A glass roof shelters even snowy biomes");
            h.setBlock(8,5,8,Blocks.AIR);refresh(sheep);
            h.assertTrue(state(sheep).ffm$isCold(),"Exposed sheared sheep get cold in snowy biomes: pos="+sheep.blockPosition()+" biome="+level.getBiome(sheep.blockPosition()).unwrapKey()+" roof="+level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,sheep.getBlockX(),sheep.getBlockZ())+" exposure="+SheepCare.exposedToCold(sheep)+" sheared="+sheep.isSheared());
            double normal=sheep.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
            h.assertTrue(Math.abs(sheep.getAttributeValue(Attributes.MOVEMENT_SPEED)-normal*.75)<.000001,"Cold slows movement by one quarter");
            for(int i=0;i<4;i++)refresh(sheep);
            h.assertTrue(Math.abs(sheep.getAttributeValue(Attributes.MOVEMENT_SPEED)-normal*.75)<.000001,"Repeated weather checks do not stack the slowdown");
            var goal=new EatBlockGoal(sheep);h.assertFalse(goal.canUse(),"Cold sheep cannot start grazing");
            goal.start();h.assertFalse(goal.canContinueToUse(),"Cold interrupts an active graze");for(int i=0;i<40;i++)goal.tick();
            h.assertTrue(state(sheep).ffm$grazes()==0&&h.getBlockState(new BlockPos(8,1,8)).is(Blocks.GRASS_BLOCK),"Cold grazing consumes neither grass nor wool progress");
            h.setBlock(8,5,8,Blocks.OAK_SLAB);refresh(sheep);
            h.assertFalse(SheepCare.exposedToCold(sheep),"A partial roof provides shelter too");
            h.assertTrue(state(sheep).ffm$isCold(),"Previously cold sheep need time to warm up");
            h.assertTrue(Math.abs(sheep.getAttributeValue(Attributes.MOVEMENT_SPEED)-normal*.75)<.000001,"The speed penalty remains during warm-up");
            h.setBlock(8,5,8,Blocks.AIR);h.setBiome(Biomes.PLAINS);refresh(sheep);
            h.assertFalse(SheepCare.exposedToCold(sheep),"Dry plains count as warm");
            level.setWeatherParameters(0,6000,true,false);level.setRainLevel(1);refresh(sheep);
            h.assertTrue(level.isRainingAt(sheep.blockPosition())&&state(sheep).ffm$isCold(),"Actual warm-biome rain makes exposed sheep cold");
            h.setBlock(8,5,8,Blocks.GLASS);refresh(sheep);h.assertFalse(SheepCare.exposedToCold(sheep),"A roof stops rain exposure");
            h.setBlock(8,5,8,Blocks.AIR);h.setBiome(Biomes.DESERT);refresh(sheep);
            h.assertFalse(SheepCare.exposedToCold(sheep),"Global rainy weather cannot wet a rainless desert");
            h.setBiome(Biomes.SNOWY_PLAINS);refresh(sheep);sheep.setSheared(false);
            h.assertFalse(state(sheep).ffm$isCold(),"Regrown wool immediately insulates sheep");
            h.assertTrue(sheep.getHealth()==health&&sheep.getTicksFrozen()==0,"Cold adds no damage or powder-snow freezing");h.succeed();
        } finally { level.setWeatherParameters(6000,0,raining,thunder);level.setRainLevel(rain); }
    }

    @GameTest(template="test_empty",batch="sheep_taiga_snow")
    public static void snowUnderfootDoesNotShelterShearedSheepInSnowyTaiga(GameTestHelper h) {
        h.setBiome(Biomes.SNOWY_TAIGA);var sheep=sheep(h);h.setBlock(8,5,8,Blocks.AIR);
        var base=h.absolutePos(new BlockPos(8,2,8));
        for(int layers=1;layers<=8;layers++) {
            h.setBlock(8,2,8,Blocks.SNOW.defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS,layers));
            sheep.moveTo(base.getX()+.5,base.getY()+(layers-1)/8.0,base.getZ()+.5);
            h.assertTrue(SheepCare.exposedToCold(sheep),"Snow underfoot must not count as a roof: layers="+layers+" height="+h.getLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,base.getX(),base.getZ()));
        }
        h.setBlock(8,5,8,Blocks.SPRUCE_LEAVES);
        h.assertFalse(SheepCare.exposedToCold(sheep),"Spruce leaves overhead still provide actual shelter");h.succeed();
    }

    @GameTest(template="test_empty",batch="sheep_shelter_transition",timeoutTicks=680)
    public static void shelterNeedsThirtySecondsAndRecoverySurvivesReload(GameTestHelper h) {
        h.setBiome(Biomes.SNOWY_PLAINS);var sheep=sheep(h);meal(h,sheep);meal(h,sheep);
        h.setBlock(8,5,8,Blocks.AIR);refresh(sheep);
        h.assertTrue(state(sheep).ffm$isCold()&&state(sheep).ffm$grazes()==2,"Becoming cold preserves completed meals");
        h.setBlock(8,5,8,Blocks.GLASS);refresh(sheep);
        Sheep[] current={sheep};
        h.runAfterDelay(300,()->{
            h.assertTrue(state(current[0]).ffm$isCold(),"Fifteen seconds of shelter is not enough");
            var tag=current[0].saveWithoutId(new CompoundTag());current[0].discard();
            var loaded=EntityType.SHEEP.create(h.getLevel());loaded.load(tag);h.getLevel().addFreshEntity(loaded);current[0]=loaded;
            h.assertTrue(state(loaded).ffm$isCold()&&state(loaded).ffm$grazes()==2,"Reload retains cold, progress and the original recovery deadline");
        });
        h.runAfterDelay(599,()->h.assertTrue(state(current[0]).ffm$isCold(),"Sheep must remain cold for the full thirty seconds"));
        h.runAfterDelay(623,()->{
            var warm=current[0];h.assertFalse(state(warm).ffm$isCold(),"Actual world ticks warm sheltered NoAI sheep after thirty seconds");
            h.assertFalse(warm.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(SheepCare.COLD_SLOWDOWN),"Recovery removes the speed penalty");
            meal(h,warm);h.assertFalse(warm.isSheared(),"The third meal resumes saved progress once warm");h.succeed();
        });
    }

    @GameTest(template="test_empty",batch="sheep_shelter_transition",timeoutTicks=980)
    public static void renewedExposureRestartsTheFullWarmup(GameTestHelper h) {
        h.setBiome(Biomes.SNOWY_PLAINS);var sheep=sheep(h);
        h.setBlock(8,5,8,Blocks.AIR);refresh(sheep);h.setBlock(8,5,8,Blocks.GLASS);refresh(sheep);
        h.runAfterDelay(300,()->{
            h.setBlock(8,5,8,Blocks.AIR);refresh(sheep);h.setBlock(8,5,8,Blocks.GLASS);refresh(sheep);
        });
        h.runAfterDelay(623,()->h.assertTrue(state(sheep).ffm$isCold(),"The old deadline cannot warm a sheep that was exposed again"));
        h.runAfterDelay(899,()->h.assertTrue(state(sheep).ffm$isCold(),"New shelter must last the full thirty seconds"));
        h.runAfterDelay(923,()->{h.assertFalse(state(sheep).ffm$isCold(),"Thirty uninterrupted seconds in new shelter complete recovery");h.succeed();});
    }
}
