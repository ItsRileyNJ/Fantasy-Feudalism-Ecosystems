package net.itsrileynj.fantasyfeudalismmobs.gametest.scorpion;

import java.util.*;
import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.itsrileynj.fantasyfeudalismmobs.scorpion.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ScorpionTests {
    private static Scorpion spawn(GameTestHelper h) {var s=h.spawn(ScorpionModule.SCORPION.get(),16,2,16);s.setOnGround(true);return s;}
    private static net.minecraft.world.entity.animal.Pig victim(GameTestHelper h,Scorpion s) {
        var p=h.spawn(EntityType.PIG,16,2,15);p.setNoAi(true);p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40);p.setHealth(40);return p;
    }
    @GameTest(template="test_empty")
    public static void scorpionSpawnEggCreatesSpiderSizedIndependentMonster(GameTestHelper h) {
        h.setBlock(8,1,8,Blocks.SAND);var p=h.makeMockPlayer(GameType.CREATIVE);p.getAbilities().instabuild=true;
        var egg=new ItemStack(ScorpionModule.EGG.get());p.setItemInHand(InteractionHand.MAIN_HAND,egg);var pos=h.absolutePos(new BlockPos(8,1,8));
        h.assertTrue(egg.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos).add(0,.5,0),Direction.UP,pos,false))).consumesAction(),"Egg uses native placement");
        var scorpions=h.getLevel().getEntitiesOfClass(Scorpion.class,new AABB(pos).inflate(3));h.assertTrue(scorpions.size()==1,"One egg makes one scorpion");
        var s=scorpions.getFirst();h.assertTrue(Math.abs(s.getBbWidth()-1.4)<.001&&Math.abs(s.getBbHeight()-.9)<.001&&s.getMaxHealth()==16,"Scorpion has vanilla spider dimensions and eight hearts");
        h.assertTrue(!(((Entity)s) instanceof net.minecraft.world.entity.monster.Spider),"Scorpion does not inherit pet-spider mechanics");h.succeed();
    }
    @GameTest(template="test_empty",batch="scorpion_biomes")
    public static void scorpionNaturalEntriesAreRareSinglesInEveryBadlandsVariant(GameTestHelper h) {
        var allowed=List.of(Biomes.BADLANDS,Biomes.ERODED_BADLANDS,Biomes.WOODED_BADLANDS);
        for(var biome:List.of(Biomes.DESERT,Biomes.BADLANDS,Biomes.ERODED_BADLANDS,Biomes.WOODED_BADLANDS,Biomes.PLAINS,Biomes.FOREST,Biomes.SAVANNA)) {
            h.setBiome(biome);var entries=h.getLevel().getBiome(h.absolutePos(new BlockPos(8,2,8))).value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
            boolean found=entries.stream().anyMatch(e->e.type==ScorpionModule.SCORPION.get()&&e.minCount==1&&e.maxCount==1&&e.getWeight().asInt()==3);
            h.assertTrue(found==allowed.contains(biome),"Rare singleton entries occur only in badlands variants");
        }h.succeed();
    }
    @GameTest(template="test_root_worm",timeoutTicks=100)
    public static void scorpionActualMeleeGoalTelegraphsThenDealsPoisonousSting(GameTestHelper h) {
        var s=spawn(h);s.targetSelector.removeAllGoals(g->true);var p=victim(h,s);s.setTarget(p);boolean[] seen={false,false};int[] started={-1};
        h.onEachTick(()->{
            if(s.isStinging()&&!seen[0]){seen[0]=true;started[0]=s.tickCount;h.assertTrue(p.getHealth()==40,"The wind-up starts before any damage");}
            if(p.getHealth()<40&&!seen[1]){seen[1]=true;h.assertTrue(seen[0]&&s.tickCount-started[0]>=Scorpion.STING_WINDUP,"Sting cannot hit before its wind-up");h.assertTrue(p.hasEffect(MobEffects.POISON),"Successful sting poisons the victim");}
        });
        h.runAfterDelay(35,()->{h.assertTrue(seen[0]&&seen[1],"Real melee AI delivers an animated poisonous sting");h.succeed();});
    }
    @GameTest(template="test_root_worm",timeoutTicks=60)
    public static void scorpionMovingAwayDuringWindupAvoidsTheSting(GameTestHelper h) {
        var s=spawn(h);s.setNoAi(true);var p=victim(h,s);s.beginSting(p);
        h.runAfterDelay(3,()->p.setPos(s.position().add(6,0,0)));
        h.runAfterDelay(20,()->{h.assertTrue(p.getHealth()==40&&!p.hasEffect(MobEffects.POISON),"The strike rechecks range instead of hitting a retreating victim remotely");h.assertTrue(!s.isStinging(),"Sting animation finishes");h.succeed();});
    }
    @GameTest(template="test_root_worm")
    public static void scorpionRejectedDamageDoesNotApplyPoison(GameTestHelper h) {
        var s=spawn(h);s.setNoAi(true);var p=victim(h,s);p.setInvulnerable(true);
        h.assertTrue(!s.doHurtTarget(p)&&p.getHealth()==40&&!p.hasEffect(MobEffects.POISON),"An invulnerable target gets neither damage nor poison");h.succeed();
    }
    @GameTest(template="test_root_worm")
    public static void scorpionCreativeAndSpectatorPlayersAreProtected(GameTestHelper h) {
        var s=spawn(h);s.setNoAi(true);
        for(var mode:List.of(GameType.CREATIVE,GameType.SPECTATOR)) {
            var p=h.makeMockPlayer(mode);p.setPos(s.position().add(0,0,1));s.beginSting(p);
            h.assertTrue(!s.isStinging()&&!s.doHurtTarget(p)&&!p.hasEffect(MobEffects.POISON),"Creative and spectator players are never stung");
        }h.succeed();
    }
    @GameTest(template="test_root_worm",timeoutTicks=100)
    public static void scorpionTargetsNearbySurvivalPlayersWithoutBeingProvoked(GameTestHelper h) {
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"scorpion-test"),false);
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);p.setGameMode(GameType.SURVIVAL);
        var s=spawn(h);p.moveTo(s.position().add(3,0,0));
        h.runAfterDelay(30,()->{try {h.assertTrue(s.getTarget()==p,"Scorpion naturally targets survival players");h.succeed();}finally{h.getLevel().getServer().getPlayerList().remove(p);}});
    }
    @GameTest(template="test_root_worm")
    public static void scorpionUsesNativeArthropodTagAndResistsItsOwnVenom(GameTestHelper h) {
        var s=spawn(h);s.setNoAi(true);
        h.assertTrue(s.getType().is(net.minecraft.tags.EntityTypeTags.ARTHROPOD),"Native arthropod enchantment rules recognize scorpions");
        h.assertTrue(!s.addEffect(new MobEffectInstance(MobEffects.POISON,100,0)),"Scorpions resist poison");
        h.assertTrue(s.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,0)),"Ordinary negative effects still work");h.succeed();
    }
    @GameTest(template="test_root_worm")
    public static void scorpionReloadPreservesHealthWithoutResumingAnOldAttack(GameTestHelper h) {
        var s=spawn(h);s.setNoAi(true);s.setHealth(9);s.beginSting(victim(h,s));var tag=new net.minecraft.nbt.CompoundTag();s.saveWithoutId(tag);
        var copy=ScorpionModule.SCORPION.get().create(h.getLevel());copy.load(tag);
        h.assertTrue(copy.getHealth()==9&&!copy.isStinging()&&copy.getStingAmount(0)==0,"Saved scorpion health survives reload and no stale target is struck");h.succeed();
    }
}
