package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.List;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WolfPackTests {
    private static void floor(GameTestHelper h){for(int x=0;x<18;x++)for(int z=0;z<18;z++)h.setBlock(x,1,z,Blocks.STONE);}
    private static Wolf wolf(GameTestHelper h,int x,int z){var w=h.spawn(EntityType.WOLF,x,2,z);w.setOnGround(true);return w;}
    private static Sheep sheep(GameTestHelper h,int x,int z){var s=h.spawn(EntityType.SHEEP,x,2,z);s.setNoAi(true);return s;}
    private static SpawnGroupData group(GameTestHelper h,Wolf w,SpawnGroupData group){return w.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(w.blockPosition()),MobSpawnType.NATURAL,group);}

    @GameTest(template="test_empty")
    public static void sheepDetectionUsesFifteenBlocksAndChargeIsSharedAtSeven(GameTestHelper h){
        floor(h);var a=wolf(h,2,8);var b=wolf(h,6,8);a.setNoAi(true);b.setNoAi(true);
        var target=sheep(h,17,8);target.moveTo(a.position().add(15.1,0,0));
        h.assertTrue(WolfPacks.prey(a)==null,"A new sheep hunt does not start outside fifteen blocks");
        target.moveTo(a.position().add(14.9,0,0));
        h.assertTrue(WolfPacks.prey(a)==target,"A sheep inside fifteen blocks is detectable");
        WolfPacks.joinHunt(a,target);
        h.assertTrue(WolfPacks.prey(b)==target,"Pack members share one prey target");
        target.moveTo(b.position().add(7.1,0,0));
        h.assertFalse(WolfPacks.charging(a,target)||WolfPacks.charging(b,target),"Both wolves stalk outside seven blocks");
        target.moveTo(b.position().add(7,0,0));
        h.assertTrue(WolfPacks.charging(b,target)&&WolfPacks.charging(a,target),"One member crossing seven blocks triggers the whole pack");
        target.moveTo(b.position().add(12,0,0));
        h.assertTrue(WolfPacks.charging(a,target),"A charging pack does not fall back to stalking if its prey flees");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void eggAndCommandWolvesJoinNearbyWildPacks(GameTestHelper h){
        floor(h);var first=wolf(h,4,8);var data=group(h,first,null);var id=WolfPacks.pack(first);
        for(var reason:List.of(MobSpawnType.SPAWN_EGG,MobSpawnType.COMMAND)) {
            var next=EntityType.WOLF.create(h.getLevel());next.moveTo(first.position().add(2,0,0));
            next.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(next.blockPosition()),reason,null);
            h.getLevel().addFreshEntity(next);
            h.assertTrue(WolfPacks.pack(next).equals(id),"Nearby egg/command wolves cooperate with the existing wild pack");
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void anEscapedSheepDoesNotBlockTheNextNearbyHunt(GameTestHelper h){
        floor(h);var wolf=wolf(h,3,8);wolf.setNoAi(true);
        var escaped=sheep(h,10,8);var next=sheep(h,12,8);
        WolfPacks.joinHunt(wolf,escaped);escaped.moveTo(wolf.position().add(40,0,0));
        h.assertTrue(WolfPacks.prey(wolf)==next,"A sheep outside the pursuit range is replaced by nearby prey");
        h.assertTrue(WolfPacks.joinHunt(wolf,next)==next,"The old live sheep cannot lock the pack into a stale hunt");
        h.assertTrue(WolfPacks.hungry(wolf),"A failed chase grants no meal cooldown");h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=420)
    public static void realPackAiStalksThenKillsOneSheepAndStopsTogether(GameTestHelper h){
        floor(h);var a=wolf(h,2,6);var b=wolf(h,2,10);var data=group(h,a,null);group(h,b,data);
        a.setBaby(false);b.setBaby(false);var prey=sheep(h,15,8);var spare=sheep(h,16,12);
        h.runAfterDelay(40,()->{
            for(var wolf:List.of(a,b)) {
                h.assertTrue(wolf.getTarget()==prey,"Pack members select the same nearest sheep");
                h.assertTrue(((WolfState)wolf).ffm$stalking(),"Wolves use the synchronized stalking state before charging");
                h.assertTrue(wolf.distanceToSqr(prey)>49,"Stalking occurs outside seven blocks");
            }
            h.assertTrue(prey.getHealth()==prey.getMaxHealth(),"No attack hits occur during the distant stalk");
        });
        h.runAfterDelay(330,()->{
            h.assertFalse(prey.isAlive(),"The pack eventually reaches and kills its prey under normal AI");
            h.assertTrue(spare.isAlive()&&spare.getHealth()==spare.getMaxHealth(),"A second sheep is left unharmed during the shared meal cooldown");
            for(var wolf:List.of(a,b)) {
                h.assertFalse(WolfPacks.hungry(wolf),"The entire pack receives a meal cooldown");
                h.assertTrue(wolf.getTarget()==null&&!((WolfState)wolf).ffm$stalking(),"All pack members disengage and stand normally");
            }
            h.succeed();
        });
    }

    @GameTest(template="test_empty")
    public static void mealCooldownCoversUnloadedMembersAndSurvivesSavedDataRoundtrip(GameTestHelper h){
        floor(h);var killer=wolf(h,4,8);var member=wolf(h,6,8);var outsider=wolf(h,8,8);
        var g=group(h,killer,null);group(h,member,g);group(h,outsider,null);
        for(var wolf:List.of(killer,member,outsider))wolf.setNoAi(true);
        var saved=member.saveWithoutId(new CompoundTag());var id=WolfPacks.pack(member);member.discard();
        var prey=sheep(h,5,8);prey.setHealth(.1F);killer.doHurtTarget(prey);
        h.assertTrue(WolfPacks.deadline(killer)==h.getLevel().getGameTime()+24000,"A successful wild sheep kill starts one Minecraft day");
        h.assertTrue(WolfPacks.hungry(outsider),"A different spawn pack receives no meal credit");
        var restored=EntityType.WOLF.create(h.getLevel());restored.load(saved);h.getLevel().addFreshEntity(restored);
        h.assertTrue(WolfPacks.pack(restored).equals(id)&&!WolfPacks.hungry(restored),"An unloaded pack member returns with the shared cooldown");
        var store=WolfPackData.get(h.getLevel());var encoded=store.save(new CompoundTag(),h.getLevel().registryAccess());
        var decoded=WolfPackData.load(encoded,h.getLevel().registryAccess());
        h.assertTrue(decoded.deadline(id)==WolfPacks.deadline(killer),"Server saved data preserves the exact shared deadline");h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=25)
    public static void cooldownExpiresWithoutBlockingDefensiveCombat(GameTestHelper h){
        floor(h);var wolf=wolf(h,5,8);wolf.setNoAi(true);
        WolfPackData.get(h.getLevel()).feed(WolfPacks.pack(wolf),h.getLevel().getGameTime()+6);
        var prey=sheep(h,8,8);wolf.setTarget(prey);
        h.assertTrue(wolf.getTarget()==null,"Fed wolves cannot acquire voluntary prey");
        var enemy=h.spawn(EntityType.ZOMBIE,6,2,8);enemy.setNoAi(true);wolf.setTarget(enemy);
        h.assertTrue(wolf.getTarget()==enemy,"Meal cooldown does not suppress defensive non-prey combat");
        h.runAfterDelay(4,()->h.assertFalse(WolfPacks.hungry(wolf),"The pack remains fed before its deadline"));
        h.runAfterDelay(8,()->{h.assertTrue(WolfPacks.hungry(wolf),"Hunting becomes available after the saved deadline");h.succeed();});
    }

    @GameTest(template="test_empty")
    public static void tamingEndsWildHuntButPreservesTamedCombatSittingAndFeeding(GameTestHelper h){
        floor(h);var wolf=wolf(h,5,8);var prey=sheep(h,8,8);
        wolf.setTarget(prey);((WolfState)wolf).ffm$setHunting(true);((WolfState)wolf).ffm$setStalking(true);
        var owner=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        wolf.setOwnerUUID(owner.getUUID());wolf.setTame(true,true);
        h.assertTrue(wolf.getTarget()==null&&!((WolfState)wolf).ffm$stalking(),"Taming immediately ends an active wild stalk");
        WolfPackData.get(h.getLevel()).feed(WolfPacks.pack(wolf),h.getLevel().getGameTime()+24000);
        wolf.setTarget(prey);
        h.assertTrue(wolf.getTarget()==prey,"A tamed wolf can still accept a sheep combat target during its former pack's cooldown");
        var melee=wolf.goalSelector.getAvailableGoals().stream().map(g->g.getGoal())
                .filter(g->g instanceof MeleeAttackGoal&&!(g instanceof WolfHuntGoals.Hunt)).findFirst().orElseThrow();
        h.assertTrue(melee.canUse(),"The original melee behavior remains usable by a tamed wolf");
        h.assertFalse(new WolfHuntGoals.Hunt(wolf).canUse(),"A tamed wolf never enters the wild pack attack goal");
        wolf.setTarget(null);wolf.setOrderedToSit(true);wolf.setInSittingPose(true);
        WolfPacks.beforeTick(wolf);
        h.assertTrue(wolf.isOrderedToSit()&&wolf.isInSittingPose(),"Pack bookkeeping preserves sitting orders");
        wolf.setHealth(1);owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.BEEF));
        wolf.mobInteract(owner,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(wolf.getHealth()>1&&owner.getMainHandItem().isEmpty(),"Owners still heal tamed wolves with ordinary food");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void tamedKillsAndNonSheepDeathsDoNotFeedWildPacks(GameTestHelper h){
        floor(h);var wild=wolf(h,4,8);var pet=wolf(h,6,8);pet.setTame(true,true);
        var prey=sheep(h,8,8);prey.setHealth(.1F);pet.doHurtTarget(prey);
        h.assertTrue(WolfPacks.hungry(wild),"A tamed wolf's kill does not put wild wolves on cooldown");
        var cow=h.spawn(EntityType.COW,8,2,10);cow.setNoAi(true);cow.setHealth(.1F);wild.doHurtTarget(cow);
        h.assertTrue(WolfPacks.hungry(wild),"Non-sheep deaths do not grant this pack meal");h.succeed();
    }
}
