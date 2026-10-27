package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.List;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PigBehaviorTests {
    private static void floor(GameTestHelper h) { for(int x=0;x<38;x++)for(int z=0;z<25;z++)h.setBlock(x,1,z,Blocks.PODZOL); }
    private static Pig pig(GameTestHelper h,int x,int z) { var p=h.spawn(EntityType.PIG,x,2,z);p.setAge(0);p.setNoAi(true);p.setOnGround(true);return p; }
    private static ItemEntity food(GameTestHelper h,Pig p,int amount) {
        var food=new ItemEntity(h.getLevel(),p.getX(),p.getY(),p.getZ(),new ItemStack(Items.BROWN_MUSHROOM,amount));
        food.setNoPickUpDelay();h.getLevel().addFreshEntity(food);return food;
    }
    private static List<ItemEntity> drops(Pig p) { return p.level().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(2),e->e.isAlive()&&PigLife.mushroom(e.getItem())); }
    private static void dig(Pig p,long seed) { var g=new PigGoals.Dig(p);g.start();p.getRandom().setSeed(seed);for(int i=0;i<90;i++)g.tick();g.stop(); }

    @GameTest(template="test_empty")
    public static void soilOddsAndMushroomTypesMatchActualSingleItemDrops(GameTestHelper h) {
        floor(h);var p=pig(h,8,8);
        var soils=List.of(Blocks.PODZOL,Blocks.ROOTED_DIRT,Blocks.COARSE_DIRT,Blocks.DIRT);
        double[] chances={.8,.5,.15,0};
        for(int soil=0;soil<soils.size();soil++) {
            h.setBlock(8,1,8,soils.get(soil));
            h.assertTrue(PigLife.mushroomChance(soils.get(soil).defaultBlockState())==chances[soil],"Each soil has exactly the requested success chance");
            for(int seed=0;seed<100;seed++) {
                long rngSeed=seed*0x9E3779B97F4A7C15L;var expected=RandomSource.create(rngSeed);boolean found=expected.nextDouble()<chances[soil];
                var type=found?(expected.nextFloat()<.8F?Items.BROWN_MUSHROOM:Items.RED_MUSHROOM):Items.AIR;
                dig(p,rngSeed);var drops=drops(p);
                h.assertTrue(drops.size()==(found?1:0),"Exactly one mushroom per successful dig, including extra ticks after completion");
                if(found)h.assertTrue(drops.getFirst().getItem().is(type)&&drops.getFirst().getItem().getCount()==1,"Successful finds use 80% brown / 20% red");
                drops.forEach(Entity::discard);
            }
            h.assertTrue(h.getBlockState(new BlockPos(8,1,8)).is(soils.get(soil)),"Digging leaves soil intact");
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void cooldownAndInterruptedDigsSurviveSavingWithoutRewards(GameTestHelper h) {
        floor(h);var p=pig(h,8,8);p.getPersistentData().putLong(PigLife.NEXT_DIG,0);
        var goal=new PigGoals.Dig(p);h.assertTrue(goal.canUse(),"Due adult on podzol can dig");goal.start();
        h.assertTrue(((PigState)p).ffm$digging(),"Starting digging synchronizes the animation flag");
        long next=PigLife.nextDig(p)-h.getLevel().getGameTime();h.assertTrue(next>=2200&&next<=2600,"Dig attempts average two minutes with staggered timing");
        goal.tick();goal.stop();for(int i=0;i<80;i++)goal.tick();
        h.assertTrue(drops(p).isEmpty()&&!((PigState)p).ffm$digging(),"Interrupted digging has no reward or stuck pose");
        var tag=p.saveWithoutId(new CompoundTag());p.discard();var loaded=EntityType.PIG.create(h.getLevel());loaded.load(tag);h.getLevel().addFreshEntity(loaded);
        h.assertTrue(PigLife.nextDig(loaded)==tag.getCompound("NeoForgeData").getLong(PigLife.NEXT_DIG),"The dig cooldown survives native save/load");
        h.assertFalse(new PigGoals.Dig(loaded).canUse(),"Reload does not grant a fresh dig");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void changedSoilAndPigletsCannotProduceMushrooms(GameTestHelper h) {
        floor(h);var p=pig(h,8,8);var goal=new PigGoals.Dig(p);goal.start();h.setBlock(8,1,8,Blocks.STONE);
        for(int i=0;i<60;i++)goal.tick();goal.stop();h.assertTrue(drops(p).isEmpty(),"Replacing soil before the reward prevents the find");
        h.setBlock(8,1,8,Blocks.PODZOL);p.setBaby(true);p.getPersistentData().putLong(PigLife.NEXT_DIG,0);
        h.assertFalse(goal.canUse(),"Piglets do not dig");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void eatingEachMushroomRemovesTwentyPercentOfRemainingGrowth(GameTestHelper h) {
        floor(h);var p=pig(h,8,8);p.setAge(-24000);var food=food(h,p,2);var goal=new PigGoals.EatMushroom(p);
        h.assertTrue(goal.canUse(),"Piglets find dropped mushrooms");goal.start();goal.tick();
        h.assertTrue(p.getAge()==-19200&&food.getItem().getCount()==1,"One mushroom removes twenty percent and consumes one item");
        h.assertTrue(PigLife.grow(p)&&p.getAge()==-15360,"Subsequent mushrooms affect remaining growth");
        p.setAge(-1);PigLife.grow(p);h.assertTrue(p.getAge()==0&&!p.isBaby(),"Near-adult piglets grow without overflow");
        h.assertFalse(PigLife.grow(p),"Adults get no mushroom growth or breeding benefit");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void twoPigletsCannotConsumeTheSameMushroomTwice(GameTestHelper h) {
        floor(h);var a=pig(h,8,8);var b=pig(h,8,8);a.setAge(-24000);b.setAge(-24000);var item=food(h,a,1);
        var first=new PigGoals.EatMushroom(a);var second=new PigGoals.EatMushroom(b);
        h.assertTrue(first.canUse()&&second.canUse(),"Both piglets can discover the same item");first.start();second.start();first.tick();second.tick();
        h.assertTrue(!item.isAlive()&&a.getAge()==-19200&&b.getAge()==-24000,"Only one piglet consumes the one mushroom");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void delayedAndPrivateDropsAreNotEatenAndAdultsIgnoreMushrooms(GameTestHelper h) {
        floor(h);var p=pig(h,8,8);var item=food(h,p,1);var goal=new PigGoals.EatMushroom(p);
        h.assertFalse(goal.canUse(),"Adults do not eat mushrooms");p.setBaby(true);item.setNeverPickUp();
        h.assertFalse(new PigGoals.EatMushroom(p).canUse(),"Unpickable items remain untouched");item.setNoPickUpDelay();item.setTarget(java.util.UUID.randomUUID());
        h.assertFalse(new PigGoals.EatMushroom(p).canUse(),"Player-reserved drops remain untouched");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void playersFeedBothColorsWithoutChangingAdultBreedingFoods(GameTestHelper h) {
        floor(h);var p=pig(h,8,8);p.setAge(-24000);var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(var mushroom:List.of(Items.BROWN_MUSHROOM,Items.RED_MUSHROOM)) {
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(mushroom,2));
            h.assertTrue(p.mobInteract(player,InteractionHand.MAIN_HAND).consumesAction(),"Both mushroom colors can feed a piglet");
            h.assertTrue(player.getMainHandItem().getCount()==1,"Survival feeding consumes exactly one mushroom");
        }
        h.assertTrue(p.getAge()==-15360,"Manual feeding applies the same twenty-percent growth");
        h.assertFalse(p.isFood(new ItemStack(Items.BROWN_MUSHROOM)),"Mushrooms are not adult breeding food");
        h.assertTrue(p.isFood(new ItemStack(Items.CARROT)),"Native carrots still work");
        var creative=h.makeMockPlayer(GameType.CREATIVE);creative.getAbilities().instabuild=true;creative.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.RED_MUSHROOM));
        p.mobInteract(creative,InteractionHand.MAIN_HAND);h.assertTrue(creative.getMainHandItem().getCount()==1,"Creative feeding preserves the item");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void looseHerdAllowsFifteenBlocksAndNeverInterferesWithRiding(GameTestHelper h) {
        floor(h);var a=pig(h,5,8);var b=pig(h,19,8);
        Pig leader=a.getUUID().compareTo(b.getUUID())<0?a:b;Pig follower=leader==a?b:a;
        var goal=new PigGoals.LooseHerd(follower);h.assertFalse(goal.canUse(),"Adults fourteen blocks apart may wander freely");
        follower.moveTo(leader.position().add(16,0,0));
        // A fresh goal avoids the intentional five-second rescan delay.
        goal=new PigGoals.LooseHerd(follower);h.assertTrue(goal.canUse(),"Separated pigs regroup beyond fifteen blocks");
        goal.start();goal.tick();h.assertFalse(follower.getNavigation().isDone(),"Regrouping uses native navigation");
        var rider=h.makeMockPlayer(GameType.SURVIVAL);rider.startRiding(follower,true);
        h.assertFalse(goal.canContinueToUse(),"Riding immediately preempts herd navigation");
        h.assertFalse(new PigGoals.Dig(follower).canUse(),"Ridden pigs cannot begin digging");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void bredPigletsRememberBothParentsAndStopFollowingAsAdults(GameTestHelper h) {
        floor(h);var first=pig(h,5,8);var second=pig(h,6,8);first.setInLove(null);second.setInLove(null);
        first.spawnChildFromBreeding(h.getLevel(),second);
        var child=h.getLevel().getEntitiesOfClass(Pig.class,first.getBoundingBox().inflate(3),Pig::isBaby).getFirst();child.setNoAi(true);child.setOnGround(true);
        h.assertTrue(child.getPersistentData().getUUID(PigLife.PARENT).equals(first.getUUID())&&child.getPersistentData().getUUID(PigLife.OTHER_PARENT).equals(second.getUUID()),"Actual native breeding records both parents");
        child.moveTo(first.position().add(10,0,0));var follow=new PigGoals.FollowFamily(child);
        h.assertTrue(follow.canUse(),"A player-bred piglet follows a remembered parent");follow.start();follow.tick();
        h.assertFalse(child.getNavigation().isDone(),"Piglets navigate toward their family");child.setAge(0);
        h.assertFalse(follow.canContinueToUse(),"Growing up immediately ends parent following");h.succeed();
    }

    @GameTest(template="test_empty",batch="pig_native_dig",timeoutTicks=120)
    public static void realPigAiStartsAndFinishesDiggingFromSpawnEgg(GameTestHelper h) {
        floor(h);var p=EntityType.PIG.spawn(h.getLevel(),h.absolutePos(new BlockPos(8,2,8)),MobSpawnType.SPAWN_EGG);
        p.setAge(0);p.getRandom().setSeed(123);
        // Native spawn placement starts above the floor; let it land before making the fixture due.
        h.runAfterDelay(10,()->p.getPersistentData().putLong(PigLife.NEXT_DIG,0));
        h.runAfterDelay(30,()->h.assertTrue(((PigState)p).ffm$digging(),"Native spawn-egg pigs run the digging goal: pos="+p.position()+" ground="+p.onGround()+" next="+PigLife.nextDig(p)+" now="+p.level().getGameTime()+" goals="+p.goalSelector.getAvailableGoals().stream().filter(g->g.isRunning()).map(g->g.getGoal().getClass().getSimpleName()).toList()));
        h.runAfterDelay(105,()->{
            h.assertFalse(((PigState)p).ffm$digging(),"The native goal completes and clears its synchronized pose");
            h.assertTrue(PigLife.nextDig(p)>h.getLevel().getGameTime(),"A finished dig enters its saved cooldown");h.succeed();
        });
    }
}
