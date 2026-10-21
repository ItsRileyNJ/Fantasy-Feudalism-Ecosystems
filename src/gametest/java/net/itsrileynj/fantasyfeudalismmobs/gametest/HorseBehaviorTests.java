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
        h.assertFalse(horse.isInLove(),"Grazing before fourteen days does not start breeding");goal.stop();
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
        h.assertTrue(HorseLife.nextBreed(foals.getFirst())==h.getLevel().getGameTime()+336000,"A foal starts its own full automatic breeding cycle");
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=90)
    public static void worldTicksCompleteGrazingInsteadOfFreezingAi(GameTestHelper h) {
        floor(h);var horse=horse(h,8,8);
        due(horse);
        // Start through GoalSelector so MOVE/LOOK/JUMP locks are registered, as in
        // normal gameplay. Repeated selection only avoids a random waiting period.
        for(int i=0;i<1000&&!horse.isEating();i++)horse.goalSelector.tick();
        h.assertTrue(horse.isEating(),"Scheduled grazing starts the native pose");
        h.runAfterDelay(55,()->{
            h.assertBlockPresent(Blocks.DIRT,8,1,8);
            h.assertFalse(horse.isEating(),"Native pose must not freeze server AI or the grazing countdown");
            h.succeed();
        });
    }

    @GameTest(template="test_empty",timeoutTicks=25)
    public static void fourteenDayDeadlineSurvivesReloadAndHasExactBoundary(GameTestHelper h) {
        floor(h);var original=horse(h,8,8);original.setNoAi(true);
        h.assertTrue(HorseLife.nextBreed(original)==h.getLevel().getGameTime()+336000,"First automatic breeding is fourteen complete Minecraft days away");
        long deadline=h.getLevel().getGameTime()+6;
        original.getPersistentData().putLong("ffm_horse_next_breed",deadline);
        var saved=original.saveWithoutId(new CompoundTag());original.discard();
        var loaded=EntityType.HORSE.create(h.getLevel());loaded.load(saved);h.getLevel().addFreshEntity(loaded);
        h.assertTrue(HorseLife.nextBreed(loaded)==deadline,"Reload never resets the saved breeding deadline");
        h.runAfterDelay(4,()->h.assertFalse(HorseLife.ready(loaded),"The horse cannot breed before the deadline"));
        h.runAfterDelay(7,()->{h.assertTrue(HorseLife.ready(loaded),"The horse becomes eligible once the saved deadline passes");h.succeed();});
    }

    @GameTest(template="test_empty",timeoutTicks=210)
    public static void grassStartsWildBreedingOnceAndFoalJoinsHerd(GameTestHelper h) {
        floor(h);var a=horse(h,7,8);var b=horse(h,9,8);var extra=horse(h,12,8);
        var data=group(h,a,null);group(h,b,data);group(h,extra,data);due(a);due(b);due(extra);
        b.goalSelector.removeAllGoals(g->g instanceof net.minecraft.world.entity.ai.goal.RandomStrollGoal);
        extra.goalSelector.removeAllGoals(g->g instanceof net.minecraft.world.entity.ai.goal.RandomStrollGoal);
        for(int i=0;i<1000&&!a.isEating();i++)a.goalSelector.tick();
        h.assertTrue(a.isEating(),"Normal goal selection starts grazing with its movement locks");
        h.runAfterDelay(45,()->{
            h.assertTrue(a.isInLove()&&b.isInLove(),"World-ticked grazing starts love mode for a nearby eligible mate");
            h.assertFalse(extra.isInLove(),"A third horse is not enrolled into the reserved pair");
            h.assertFalse(a.isTamed()||b.isTamed(),"Natural breeding does not silently tame either parent");
            h.assertTrue(a.canMate(b)&&b.canMate(a),"Untamed horses can mate through the natural breeding pair");
        });
        h.runAfterDelay(170,()->{
            var foals=h.getLevel().getEntitiesOfClass(Horse.class,a.getBoundingBox().inflate(18),
                    child->child.isBaby()&&SpawnHerd.together(a,child));
            h.assertTrue(foals.size()==1,"Exactly one foal is born, got "+foals.size());
            var foal=foals.getFirst();
            h.assertFalse(foal.isTamed(),"Natural offspring remain untamed");
            h.assertTrue(HorseLife.nextBreed(a)>h.getLevel().getGameTime()+335000
                    &&HorseLife.nextBreed(b)>h.getLevel().getGameTime()+335000,"Both parents receive a fresh fourteen-day cycle");
            h.assertFalse(HorseLife.ready(a)||HorseLife.ready(b)||HorseLife.ready(foal),"Parents and foal cannot immediately start another natural breeding");
            h.succeed();
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
