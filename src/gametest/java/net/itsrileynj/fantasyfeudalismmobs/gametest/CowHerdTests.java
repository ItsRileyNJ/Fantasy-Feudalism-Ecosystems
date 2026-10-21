package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.Comparator;
import java.util.List;
import net.itsrileynj.fantasyfeudalismmobs.CowHerd;
import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CowHerdTests {
    private static void floor(GameTestHelper h) {
        for(int x=0;x<18;x++) for(int z=0;z<18;z++) h.setBlock(x,1,z,Blocks.STONE);
    }
    private static SpawnGroupData spawnGroup(GameTestHelper h, Cow cow, SpawnGroupData group) {
        return cow.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(cow.blockPosition()),
                MobSpawnType.NATURAL,group);
    }

    @GameTest(template="test_empty")
    public static void spawnGroupsStayDistinctAndMembershipSurvivesReload(GameTestHelper h) {
        floor(h);
        var first=h.spawn(EntityType.COW,4,2,4);var second=h.spawn(EntityType.COW,6,2,4);
        var separate=h.spawn(EntityType.COW,8,2,4);
        var group=spawnGroup(h,first,null);spawnGroup(h,second,group);spawnGroup(h,separate,null);
        h.assertTrue(CowHerd.together(first,second),"Cows finalized in one spawn group share a herd");
        h.assertFalse(CowHerd.together(first,separate),"Nearby cows from different spawn groups do not merge herds");
        CompoundTag saved=second.saveWithoutId(new CompoundTag());second.discard();
        var loaded=EntityType.COW.create(h.getLevel());loaded.load(saved);h.getLevel().addFreshEntity(loaded);
        h.assertTrue(CowHerd.together(first,loaded),"Herd membership survives entity save/load");
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=200)
    public static void separatedCowUsesNormalNavigationToRejoinItsHerd(GameTestHelper h) {
        floor(h);
        var first=h.spawn(EntityType.COW,4,2,8);var second=h.spawn(EntityType.COW,14,2,8);
        var group=spawnGroup(h,first,null);spawnGroup(h,second,group);
        first.setBaby(false);second.setBaby(false);
        var leader=List.of(first,second).stream().min(Comparator.comparing(Cow::getUUID)).orElseThrow();
        var follower=leader==first?second:first;
        leader.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(3,2,8)));leader.setNoAi(true);
        follower.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(14,2,8)));
        var stranger=h.spawn(EntityType.COW,14,2,10);spawnGroup(h,stranger,null);stranger.setNoAi(true);
        h.succeedWhen(()->h.assertTrue(follower.distanceToSqr(leader)<16,
                "A separated cow must walk back to its own herd rather than stay with an unrelated nearby cow"));
    }

    @GameTest(template="test_empty")
    public static void herdReelectsAfterLeaderDeathAndRespectsLeashes(GameTestHelper h) {
        floor(h);
        var first=h.spawn(EntityType.COW,3,2,8);var second=h.spawn(EntityType.COW,8,2,8);
        var third=h.spawn(EntityType.COW,14,2,8);
        var group=spawnGroup(h,first,null);spawnGroup(h,second,group);spawnGroup(h,third,group);
        var ordered=List.of(first,second,third).stream().sorted(Comparator.comparing(Cow::getUUID)).toList();
        ordered.get(0).discard();
        var leader=ordered.get(1);var follower=ordered.get(2);
        leader.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(3,2,8)));
        follower.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(14,2,8)));
        follower.setOnGround(true);
        var goal=new CowHerd.FollowHerdGoal(follower);
        h.assertTrue(goal.canUse(),"Surviving cows choose a replacement herd leader");
        goal.start();goal.tick();h.assertFalse(follower.getNavigation().isDone(),"The survivor paths to the new leader");
        follower.setLeashedTo(leader,false);
        h.assertFalse(goal.canContinueToUse(),"Herd behavior yields to a leash");goal.stop();
        h.assertFalse(new CowHerd.FollowHerdGoal(follower).canUse(),"A leashed cow cannot start herd following");
        h.succeed();
    }
}
