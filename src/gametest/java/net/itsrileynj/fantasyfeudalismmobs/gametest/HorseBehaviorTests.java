package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.Comparator;
import java.util.List;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HorseBehaviorTests {
    private static void floor(GameTestHelper h) {
        for(int x=0;x<18;x++) for(int z=0;z<18;z++) h.setBlock(x,1,z,Blocks.GRASS_BLOCK);
    }
    private static Horse horse(GameTestHelper h,int x,int z) {
        var horse=h.spawn(EntityType.HORSE,x,2,z);horse.setOnGround(true);return horse;
    }
    private static SpawnGroupData group(GameTestHelper h,Horse horse,SpawnGroupData group) {
        return horse.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(horse.blockPosition()),MobSpawnType.NATURAL,group);
    }
    private static void due(Horse horse) {
        horse.setAge(0);horse.getPersistentData().putLong("ffm_horse_next_breed",horse.level().getGameTime());
    }
    private static void graze(Horse horse) {
        var goal=new HorseGrazingGoal(horse);goal.start();for(int i=0;i<40;i++)goal.tick();goal.stop();
    }

    @GameTest(template="test_empty")
    public static void horseSpawnGroupsPersistAndKeepSeparateHerds(GameTestHelper h) {
        floor(h);var a=horse(h,4,8);var b=horse(h,6,8);var other=horse(h,8,8);
        var data=group(h,a,null);group(h,b,data);group(h,other,null);
        h.assertTrue(SpawnHerd.together(a,b),"A natural horse group shares one herd");
        h.assertFalse(SpawnHerd.together(a,other),"Different spawn groups remain separate");
        b.setEating(true);var saved=b.saveWithoutId(new CompoundTag());b.discard();
        var loaded=EntityType.HORSE.create(h.getLevel());loaded.load(saved);h.getLevel().addFreshEntity(loaded);
        h.assertTrue(SpawnHerd.together(a,loaded),"Horse herd membership survives save/load");
        h.assertFalse(loaded.isEating(),"Reloading an interrupted grazing pose must not freeze the horse");h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=240)
    public static void separatedHorseRejoinsItsHerd(GameTestHelper h) {
        floor(h);var a=horse(h,3,8);var b=horse(h,14,8);var data=group(h,a,null);group(h,b,data);
        a.setBaby(false);b.setBaby(false);
        var leader=List.of(a,b).stream().min(Comparator.comparing(Horse::getUUID)).orElseThrow();
        var follower=leader==a?b:a;
        leader.moveTo(h.absoluteVec(new Vec3(3,2,8)));leader.setNoAi(true);
        follower.moveTo(h.absoluteVec(new Vec3(14,2,8)));
        h.succeedWhen(()->h.assertTrue(follower.distanceToSqr(leader)<16,"The separated horse walks back to its herd"));
    }

    @GameTest(template="test_empty")
    public static void grazingConsumesGrassAndUsesNativeHorsePose(GameTestHelper h) {
        floor(h);var horse=horse(h,8,8);horse.setNoAi(true);
        var goal=new HorseGrazingGoal(horse);goal.start();
        h.assertTrue(horse.isEating(),"The native synchronized eating flag is set");
        for(int i=0;i<36;i++)goal.tick();
        h.assertBlockPresent(Blocks.DIRT,8,1,8);
        h.assertFalse(horse.isInLove(),"Grazing before the automatic deadline does not start breeding");goal.stop();
        h.assertFalse(horse.isEating(),"Stopping restores normal movement and pose");
        h.setBlock(8,2,8,Blocks.SHORT_GRASS);graze(horse);
        h.assertBlockPresent(Blocks.AIR,8,2,8);h.succeed();
    }

    @GameTest(template="test_empty")
    public static void foalCanInheritHerdFromEitherParent(GameTestHelper h) {
        floor(h);var ungrouped=horse(h,7,8);var grouped=horse(h,9,8);group(h,grouped,null);
        ungrouped.spawnChildFromBreeding(h.getLevel(),grouped);
        var foals=h.getLevel().getEntitiesOfClass(Horse.class,grouped.getBoundingBox().inflate(8),
                child->child.isBaby()&&SpawnHerd.together(grouped,child));
        h.assertTrue(foals.size()==1,"A foal joins the second parent's herd when the first parent has no herd");
        h.assertTrue(HorseLife.nextBreed(foals.getFirst())==h.getLevel().getGameTime()+180000,"A foal starts its own full automatic breeding cycle");
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=320)
    public static void worldTicksCompleteGrazingInsteadOfFreezingAi(GameTestHelper h) {
        floor(h);var horse=horse(h,8,8);due(horse);
        boolean[] seen={false};BlockPos[] bite={null};
        h.onEachTick(()->{if(horse.isEating()){seen[0]=true;bite[0]=horse.blockPosition().below();}});
        h.succeedWhen(()->{
            h.assertTrue(seen[0]&&!horse.isEating(),"Ordinary AI starts and finishes grazing without forced goal scans");
            h.assertTrue(h.getLevel().getBlockState(bite[0]).is(Blocks.DIRT),"World-ticked grazing consumes the actual grass block");
        });
    }

    @AfterBatch(batch="ffm_horse_temptation")
    public static void removeTemptingFixturePlayer(net.minecraft.server.level.ServerLevel level) {
        for(var player:List.copyOf(level.players()))if(player.getGameProfile().getName().equals("test-mock-player"))
            level.getServer().getPlayerList().remove(player);
    }

    @GameTest(template="test_empty",timeoutTicks=950,batch="ffm_horse_temptation")
    public static void ordinaryGrazingIsNotStarvedByAPlayerHoldingHorseFood(GameTestHelper h) {
        floor(h);var horse=horse(h,8,8);horse.setAge(0);
        var player=h.makeMockServerPlayerInLevel();player.moveTo(h.absoluteVec(new Vec3(8,2,11)));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.GOLDEN_CARROT));
        boolean[] seen={false};BlockPos[] bite={null};
        h.runAfterDelay(20,()->h.assertTrue(horse.goalSelector.getAvailableGoals().stream()
                .anyMatch(g->g.isRunning()&&g.getGoal() instanceof net.minecraft.world.entity.ai.goal.TemptGoal),
                "The fixture really starts normal food-following before the grazing timer"));
        h.onEachTick(()->{if(horse.isEating()){seen[0]=true;bite[0]=horse.blockPosition().below();}});
        h.succeedWhen(()->{
            h.assertTrue(seen[0]&&!horse.isEating(),"Normal timed grazing interrupts food-following and completes");
            h.assertTrue(h.getLevel().getBlockState(bite[0]).is(Blocks.DIRT),"The horse consumes real grass while the player continues holding a golden carrot");
            h.assertFalse(horse.isInLove(),"Grazing before the automatic deadline must not prematurely start breeding");
            h.assertTrue(HorseLife.nextBreed(horse)>h.getLevel().getGameTime()+174000,"The original two-and-a-half-hour deadline remains in force");
            h.getLevel().getServer().getPlayerList().remove(player);
        });
    }

    @GameTest(template="test_empty",timeoutTicks=25)
    public static void twoAndAHalfHourDeadlineSurvivesReloadAndHasExactBoundary(GameTestHelper h) {
        floor(h);var original=horse(h,8,8);original.setNoAi(true);
        h.assertTrue(HorseLife.nextBreed(original)==h.getLevel().getGameTime()+180000,"First automatic breeding is two and a half hours away");
        long deadline=h.getLevel().getGameTime()+6;
        original.getPersistentData().putLong("ffm_horse_next_breed",deadline);
        var saved=original.saveWithoutId(new CompoundTag());original.discard();
        var loaded=EntityType.HORSE.create(h.getLevel());loaded.load(saved);h.getLevel().addFreshEntity(loaded);
        h.assertTrue(HorseLife.nextBreed(loaded)==deadline,"Reload never resets the saved breeding deadline");
        h.runAfterDelay(4,()->h.assertFalse(HorseLife.ready(loaded),"The horse cannot breed before the deadline"));
        h.runAfterDelay(7,()->{h.assertTrue(HorseLife.ready(loaded),"The horse becomes eligible once the saved deadline passes");h.succeed();});
    }

    @GameTest(template="test_empty")
    public static void legacyHorseTimersShortenOnceAndRetainElapsedTime(GameTestHelper h) {
        floor(h);var horse=horse(h,8,8);horse.setNoAi(true);horse.setAge(0);
        long now=h.getLevel().getGameTime();var tag=horse.saveWithoutId(new CompoundTag());horse.discard();
        // A 0.1.11 horse has waited two hours of its old fourteen-day cycle.
        var data=tag.getCompound("NeoForgeData");data.remove("ffm_horse_breed_interval");
        data.putLong("ffm_horse_next_breed",now-144000+336000);
        var loaded=EntityType.HORSE.create(h.getLevel());loaded.load(tag);h.getLevel().addFreshEntity(loaded);
        h.assertTrue(HorseLife.BREED_INTERVAL==180000,"The new interval is exactly 150 minutes at twenty ticks per second");
        h.assertTrue(HorseLife.nextBreed(loaded)==now+36000,"A horse that already waited two hours has only thirty minutes left");
        for(int i=0;i<10;i++)HorseLife.initialize(loaded);
        var saved=loaded.saveWithoutId(new CompoundTag());loaded.discard();
        var again=EntityType.HORSE.create(h.getLevel());again.load(saved);h.getLevel().addFreshEntity(again);
        h.assertTrue(HorseLife.nextBreed(again)==now+36000,"Repeated access and reload never subtract the adjustment again");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void alreadyElapsedLegacyCycleBecomesReadyWithoutRestarting(GameTestHelper h) {
        floor(h);var horse=horse(h,8,8);horse.setNoAi(true);horse.setAge(0);
        long now=h.getLevel().getGameTime();horse.getPersistentData().remove("ffm_horse_breed_interval");
        horse.getPersistentData().putLong("ffm_horse_next_breed",now-200000+336000);
        HorseLife.initialize(horse);
        h.assertTrue(HorseLife.ready(horse)&&HorseLife.nextBreed(horse)==now-20000,"An old cycle older than 150 minutes becomes eligible immediately");h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=520)
    public static void grassStartsWildBreedingOnceAndFoalJoinsHerd(GameTestHelper h) {
        floor(h);var a=horse(h,7,8);var b=horse(h,9,8);var extra=horse(h,12,8);
        var data=group(h,a,null);group(h,b,data);group(h,extra,data);due(a);due(b);due(extra);
        boolean[] sawPair={false};
        h.onEachTick(()->{
            long inLove=List.of(a,b,extra).stream().filter(Horse::isInLove).count();
            if(inLove>0){h.assertTrue(inLove==2,"Only two horses reserve an automatic pair");sawPair[0]=true;}
        });
        h.runAfterDelay(450,()->{
            var foals=h.getLevel().getEntitiesOfClass(Horse.class,a.getBoundingBox().inflate(25),
                    child->child.isBaby()&&SpawnHerd.together(a,child));
            h.assertTrue(sawPair[0]&&foals.size()==1,"Scheduled grazing and ordinary mating AI produce exactly one foal");
            h.assertFalse(foals.getFirst().isTamed(),"Natural offspring remain untamed");
            long reset=List.of(a,b,extra).stream().filter(parent->HorseLife.nextBreed(parent)>h.getLevel().getGameTime()+179000).count();
            h.assertTrue(reset==2,"Exactly the two parents receive a fresh two-and-a-half-hour cycle");
            h.assertTrue(List.of(a,b,extra).stream().noneMatch(Horse::isTamed),"Automatic breeding never silently tames the parents");
            h.assertFalse(HorseLife.ready(foals.getFirst()),"The foal cannot breed immediately");h.succeed();
        });
    }

    @GameTest(template="test_empty")
    public static void solitaryYoungAndRiddenHorsesDoNotStartNaturalBreeding(GameTestHelper h) {
        floor(h);var a=horse(h,7,8);due(a);graze(a);
        h.assertFalse(a.isInLove(),"Grass alone cannot create a foal without a mate");
        var b=horse(h,9,8);due(b);b.setBaby(true);HorseLife.grazed(a);
        h.assertFalse(a.isInLove()||b.isInLove(),"Foals cannot be selected as mates");
        b.setAge(0);var rider=h.spawn(EntityType.PIG,9,2,8);rider.startRiding(b,true);HorseLife.grazed(a);
        h.assertFalse(a.isInLove()||b.isInLove(),"Ridden horses are excluded from automatic breeding");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void goldenCarrotsStillAllowManualBreedingBeforeAutomaticDeadline(GameTestHelper h) {
        floor(h);var a=horse(h,7,8);var b=horse(h,9,8);
        a.setTamed(true);b.setTamed(true);a.setHealth(a.getMaxHealth());b.setHealth(b.getMaxHealth());
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.GOLDEN_CARROT,2));
        a.mobInteract(player,net.minecraft.world.InteractionHand.MAIN_HAND);
        b.mobInteract(player,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(a.isInLove()&&b.isInLove()&&a.canMate(b),"Golden-carrot breeding keeps vanilla behavior before the automatic breeding deadline");
        h.assertTrue(player.getMainHandItem().isEmpty(),"Manual feeding consumes one carrot per horse");h.succeed();
    }

    @GameTest(template="test_empty",batch="ffm_horse_griefing")
    public static void mobGriefingFalsePreservesGrassWithoutBlockingGrazing(GameTestHelper h) {
        floor(h);boolean previous=h.getLevel().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        h.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false,h.getLevel().getServer());
        try {
            var a=horse(h,7,8);var b=horse(h,9,8);due(a);due(b);graze(a);
            h.assertBlockPresent(Blocks.GRASS_BLOCK,7,1,8);
            h.assertTrue(a.isInLove()&&b.isInLove(),"Sheep-style no-griefing grazing still starts the breeding pair");
        } finally {h.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(previous,h.getLevel().getServer());}
        h.succeed();
    }

    @GameTest(template="test_empty",batch="ffm_horse_drops")
    public static void adultOnlyMeatDropsRespectMobLoot(GameTestHelper h) {
        floor(h);
        for(int i=0;i<12;i++) {
            var horse=horse(h,8,8);horse.setNoAi(true);horse.setBaby(i==11);
            horse.hurt(h.getLevel().damageSources().generic(),1000);
            var meat=h.getLevel().getEntitiesOfClass(ItemEntity.class,horse.getBoundingBox().inflate(2),
                    item->item.getItem().is(FantasyFeudalismMobs.RAW_HORSE.get()));
            int count=meat.stream().mapToInt(item->item.getItem().getCount()).sum();
            h.assertTrue(i==11?count==0:count>=1&&count<=2,"Adults drop one or two raw horse, foals drop none; got "+count);
            meat.forEach(Entity::discard);horse.discard();
        }
        boolean previous=h.getLevel().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
        h.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(false,h.getLevel().getServer());
        try {
            var horse=horse(h,8,8);horse.hurt(h.getLevel().damageSources().generic(),1000);
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,horse.getBoundingBox().inflate(2),
                    item->item.getItem().is(FantasyFeudalismMobs.RAW_HORSE.get())).isEmpty(),"doMobLoot=false prevents horse meat drops");
        } finally {h.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(previous,h.getLevel().getServer());}
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=250)
    public static void furnaceAndSmokerCookHorseWithoutDuplicatingInput(GameTestHelper h) {
        floor(h);h.setBlock(4,2,8,Blocks.FURNACE);h.setBlock(8,2,8,Blocks.SMOKER);
        var furnace=(AbstractFurnaceBlockEntity)h.getBlockEntity(new BlockPos(4,2,8));
        var smoker=(AbstractFurnaceBlockEntity)h.getBlockEntity(new BlockPos(8,2,8));
        for(var block:List.of(furnace,smoker)) {
            block.setItem(0,new ItemStack(FantasyFeudalismMobs.RAW_HORSE.get()));block.setItem(1,new ItemStack(Items.COAL));
        }
        h.runAfterDelay(110,()->{
            h.assertTrue(smoker.getItem(2).is(FantasyFeudalismMobs.COOKED_HORSE.get()),"Smoker cooks in one hundred ticks");
            h.assertTrue(furnace.getItem(2).isEmpty(),"Furnace retains its longer cooking time");
        });
        h.runAfterDelay(215,()->{
            for(var block:List.of(furnace,smoker)) {
                h.assertTrue(block.getItem(0).isEmpty(),"Exactly one raw meat is consumed");
                h.assertTrue(block.getItem(2).is(FantasyFeudalismMobs.COOKED_HORSE.get())&&block.getItem(2).getCount()==1,"Cooking produces exactly one cooked horse");
            }
            h.succeed();
        });
    }

    @GameTest(template="test_empty")
    public static void horseMeatHasMuttonFoodStatsAndCompanionFoodTags(GameTestHelper h) {
        var cooked=new ItemStack(FantasyFeudalismMobs.COOKED_HORSE.get());
        var raw=new ItemStack(FantasyFeudalismMobs.RAW_HORSE.get());
        h.assertTrue(cooked.get(DataComponents.FOOD).equals(Items.COOKED_MUTTON.components().get(DataComponents.FOOD)),"Cooked Horse has exactly the food properties of cooked mutton");
        h.assertTrue(raw.get(DataComponents.FOOD).equals(Items.MUTTON.components().get(DataComponents.FOOD)),"Raw Horse uses raw mutton food properties");
        h.assertTrue(SpiderCompanion.healingFood(raw),"New raw meat feeds tamed spiders through the common raw-meat tag");
        h.assertFalse(SpiderCompanion.healingFood(cooked),"Cooked meat remains outside the raw-food rule");h.succeed();
    }
}
