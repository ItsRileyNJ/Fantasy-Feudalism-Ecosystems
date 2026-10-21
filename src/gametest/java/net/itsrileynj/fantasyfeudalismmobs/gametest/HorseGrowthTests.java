package net.itsrileynj.fantasyfeudalismmobs.gametest;

import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HorseGrowthTests {
    private static Horse foal(GameTestHelper h) {
        for(int x=0;x<18;x++)for(int z=0;z<18;z++)h.setBlock(x,1,z,Blocks.STONE);
        var horse=h.spawn(EntityType.HORSE,8,2,8);horse.setNoAi(true);horse.setBaby(true);return horse;
    }

    @GameTest(template="test_empty")
    public static void foalsAgeOncePerThreeTicksAndKeepPartialProgressOnReload(GameTestHelper h) {
        var horse=foal(h);
        h.assertTrue(horse.getAge()==-24000,"Vanilla age units remain intact for feeding and existing saves");
        horse.aiStep();h.assertTrue(horse.getAge()==-24000,"First tick retains baby age");
        var saved=horse.saveWithoutId(new CompoundTag());horse.discard();
        var loaded=EntityType.HORSE.create(h.getLevel());loaded.load(saved);h.getLevel().addFreshEntity(loaded);
        loaded.aiStep();h.assertTrue(loaded.getAge()==-24000,"Reload retains partial growth instead of advancing early");
        loaded.aiStep();h.assertTrue(loaded.getAge()==-23999,"The third total tick advances one vanilla age unit");
        for(int i=0;i<300;i++)loaded.aiStep();
        h.assertTrue(loaded.getAge()==-23899,"Three hundred additional ticks advance exactly one hundred age units");
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=80)
    public static void worldTicksMatureFoalsAfterTripleTimeAndRefreshAdultDimensions(GameTestHelper h) {
        var horse=foal(h);horse.setAge(-20);float babyWidth=horse.getBbWidth();
        h.runAfterDelay(58,()->h.assertTrue(horse.isBaby(),"A foal remains a baby before sixty elapsed ticks for twenty age units"));
        h.runAfterDelay(63,()->{
            h.assertTrue(horse.getAge()==0&&!horse.isBaby(),"Natural world ticks eventually reach adulthood");
            h.assertTrue(horse.getBbWidth()>babyWidth*1.9F,"Vanilla synchronized age transition refreshes adult dimensions");
            h.succeed();
        });
    }

    @GameTest(template="test_empty")
    public static void adultCooldownsOtherAnimalsAndFeedingKeepVanillaRates(GameTestHelper h) {
        var horse=foal(h);horse.setAge(6);
        for(int i=0;i<3;i++)horse.aiStep();
        h.assertTrue(horse.getAge()==3,"Adult breeding cooldown counts down every tick");
        var cow=h.spawn(EntityType.COW,12,2,8);cow.setNoAi(true);cow.setAge(-6);
        var donkey=h.spawn(EntityType.DONKEY,14,2,8);donkey.setNoAi(true);donkey.setAge(-6);
        for(int i=0;i<3;i++){cow.aiStep();donkey.aiStep();}
        h.assertTrue(cow.getAge()==-3&&donkey.getAge()==-3,"Cows and donkeys retain vanilla baby growth");
        horse.setBaby(true);horse.ageUp(30,false);
        h.assertTrue(horse.getAge()==-23400,"Food-based growth still adds vanilla age units immediately");
        h.succeed();
    }
}
