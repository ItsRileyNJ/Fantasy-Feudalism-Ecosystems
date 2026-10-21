package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.List;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SilverfishBurrowingTests {
    private static void floor(GameTestHelper h) {
        for(int x=0;x<18;x++)for(int z=0;z<18;z++)h.setBlock(x,1,z,Blocks.STONE);
    }
    private static Silverfish fish(GameTestHelper h, int x) {
        var fish=h.spawn(EntityType.SILVERFISH,x,2,4);fish.setNoAi(true);return fish;
    }
    private static Zombie zombie(GameTestHelper h, EntityType<? extends Zombie> type, float health) {
        var zombie=h.spawn(type,8,2,8);zombie.setNoAi(true);
        for(var slot:EquipmentSlot.values())zombie.setItemSlot(slot,ItemStack.EMPTY);
        zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
        zombie.setHealth(health);return zombie;
    }
    private static Goal merge(Silverfish fish) {
        return fish.goalSelector.getAvailableGoals().stream().map(g->g.getGoal())
                .filter(g->g instanceof RandomStrollGoal).findFirst().orElseThrow();
    }

    @GameTest(template="test_empty")
    public static void onlyFinalHitReceivesCreditAndOtherDeathsDoNotCount(GameTestHelper h) {
        floor(h);
        var assistant=fish(h,4);var killer=fish(h,6);var bystander=fish(h,10);
        var zombie=zombie(h,EntityType.ZOMBIE,3);
        h.assertTrue(assistant.doHurtTarget(zombie),"An assisting hit succeeds");
        h.assertFalse(SilverfishBurrowing.hasKill(assistant),"A nonlethal hit earns no credit");
        zombie.invulnerableTime=0;
        h.assertTrue(killer.doHurtTarget(zombie),"The final hit succeeds");
        h.assertTrue(SilverfishBurrowing.hasKill(killer),"The final hitter alone earns credit");
        h.assertFalse(SilverfishBurrowing.hasKill(assistant)||SilverfishBurrowing.hasKill(bystander),"Credit is never shared with the pack");
        h.assertFalse(killer.doHurtTarget(zombie),"Hitting the dead zombie cannot produce another kill");
        var environmental=zombie(h,EntityType.ZOMBIE,3);
        assistant.doHurtTarget(environmental);environmental.invulnerableTime=0;
        environmental.hurt(h.getLevel().damageSources().generic(),100);
        h.assertFalse(SilverfishBurrowing.hasKill(assistant),"A prior attacker does not own an environmental killing blow");
        var other=h.spawn(EntityType.PILLAGER,8,2,10);other.setNoAi(true);other.setHealth(0.1F);
        assistant.doHurtTarget(other);
        h.assertFalse(SilverfishBurrowing.hasKill(assistant),"Killing a non-zombie does not count");
        var goal=merge(killer);h.assertTrue(goal.canUse(),"The killer can burrow");goal.start();
        h.assertTrue(SilverfishBurrowing.isBurrowed(killer),"Only the killer hides");
        h.assertFalse(SilverfishBurrowing.isBurrowed(assistant)||SilverfishBurrowing.isBurrowed(bystander),"The rest remain above ground");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void zombieVariantsCreditSurvivesReloadAndBurrowingNeedsNoGriefing(GameTestHelper h) {
        floor(h);
        boolean griefing=h.getLevel().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        h.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false,h.getLevel().getServer());
        try {
            for(var type:List.of(EntityType.ZOMBIE,EntityType.HUSK,EntityType.DROWNED,EntityType.ZOMBIE_VILLAGER,EntityType.ZOMBIFIED_PIGLIN)) {
                var killer=fish(h,4);killer.doHurtTarget(zombie(h,type,0.1F));
                CompoundTag saved=killer.saveWithoutId(new CompoundTag());killer.discard();
                var restored=EntityType.SILVERFISH.create(h.getLevel());restored.load(saved);h.getLevel().addFreshEntity(restored);
                h.assertTrue(SilverfishBurrowing.hasKill(restored),"Kill credit survives save/load for "+type);
                restored.setTarget(zombie(h,EntityType.ZOMBIE,20));
                h.assertTrue(restored.getTarget()==null,"A credited silverfish stops hunting and prepares to burrow");
                restored.setRemainingFireTicks(200); restored.setSharedFlagOnFire(true);
                var goal=merge(restored);h.assertTrue(goal.canUse(),"Burrowing works without block griefing");goal.start();
                h.assertTrue(SilverfishBurrowing.isBurrowed(restored)&&restored.isInvisible()&&restored.isNoAi(),"The killer hides without continuing AI");
                h.assertFalse(restored.isOnFire(),"Burrowing extinguishes the killer instead of leaving visible flames");
                h.assertBlockPresent(Blocks.STONE,4,1,4);
                restored.discard();
            }
        } finally {h.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(griefing,h.getLevel().getServer());}
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=650,batch="ffm_silverfish_timer")
    public static void burrowLastsThirtySecondsIncludingMidwaySaveLoad(GameTestHelper h) {
        floor(h);
        var original=fish(h,4);original.doHurtTarget(zombie(h,EntityType.ZOMBIE,0.1F));
        h.assertFalse(SilverfishBurrowing.isBurrowed(original),"The timer starts on entering a block, not on the kill");
        for(var effect:List.of(MobEffects.MOVEMENT_SLOWDOWN,MobEffects.WEAKNESS,MobEffects.POISON))
            original.addEffect(new MobEffectInstance(effect,120,0));
        var goal=merge(original);h.assertTrue(goal.canUse(),"Adjacent stone is available");goal.start();
        long deadline=SilverfishBurrowing.deadline(original);
        h.assertTrue(deadline==h.getLevel().getGameTime()+600,"Thirty seconds is exactly 600 game ticks");
        Silverfish[] actor={original};
        h.runAfterDelay(121,()->{
            h.assertTrue(actor[0].getActiveEffects().isEmpty(),"Six-second debuffs must expire while burrowed");
            h.assertTrue(actor[0].isInvisible(),"Expiring effects must not reveal the hidden killer");
        });
        h.runAfterDelay(300,()->{
            h.assertTrue(actor[0].isAlive(),"The hidden fish survives the first fifteen seconds");
            CompoundTag saved=actor[0].saveWithoutId(new CompoundTag());actor[0].discard();
            var restored=EntityType.SILVERFISH.create(h.getLevel());restored.load(saved);h.getLevel().addFreshEntity(restored);actor[0]=restored;
            h.assertTrue(SilverfishBurrowing.deadline(restored)==deadline,"Reload does not reset or extend the deadline");
            h.assertTrue(restored.noPhysics&&restored.isInvisible()&&restored.isSilent(),"Reload restores the hidden inert state");
        });
        h.runAfterDelay(599,()->h.assertTrue(actor[0].isAlive(),"The hidden fish must not despawn before thirty seconds"));
        h.runAfterDelay(601,()->{
            h.assertTrue(actor[0].isRemoved(),"The fish despawns once the thirty-second deadline is reached");
            h.assertBlockPresent(Blocks.STONE,4,1,4);
            h.succeed();
        });
    }
}
