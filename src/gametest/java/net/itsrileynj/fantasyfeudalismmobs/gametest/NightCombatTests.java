package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.*;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NightCombatTests {
    private static void floor(GameTestHelper h){for(int x=0;x<24;x++)for(int z=0;z<20;z++)h.setBlock(x,1,z,Blocks.STONE);}
    private static ServerPlayer player(GameTestHelper h,int x,int z){
        var id=UUID.randomUUID();var cookie=CommonListenerCookie.createInitial(new GameProfile(id,"eco-"+id.toString().substring(0,8)),false);
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection=new Connection(PacketFlow.SERVERBOUND);new EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        p.setGameMode(GameType.SURVIVAL);p.moveTo(h.absoluteVec(new Vec3(x,2,z)));return p;
    }
    private static void clean(ServerLevel level){
        for(var p:List.copyOf(level.players()))if(p.getGameProfile().getName().startsWith("eco-"))level.getServer().getPlayerList().remove(p);
        level.getServer().setDifficulty(Difficulty.NORMAL,true);level.setDayTime(18000);
    }
    private static Phantom bind(GameTestHelper h,ServerPlayer player,boolean ai){
        h.getLevel().setDayTime(18000);var phantom=h.spawn(EntityType.PHANTOM,8,5,8);phantom.setNoAi(!ai);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.ENDER_EYE,4));
        h.assertTrue(PhantomPact.interact(player,InteractionHand.MAIN_HAND,phantom).consumesAction(),"The night pact succeeds");return phantom;
    }
    @AfterBatch(batch="ffm_wither_equipment") public static void cleanEquipment(ServerLevel level){clean(level);}
    @AfterBatch(batch="ffm_wither_arrows") public static void cleanArrows(ServerLevel level){clean(level);}
    @AfterBatch(batch="ffm_wither_ranged") public static void cleanRanged(ServerLevel level){clean(level);}
    @AfterBatch(batch="ffm_wither_shield") public static void cleanShield(ServerLevel level){clean(level);}
    @AfterBatch(batch="ffm_wither_block") public static void cleanBlock(ServerLevel level){clean(level);}
    @AfterBatch(batch="ffm_pact_contract") public static void cleanContract(ServerLevel level){clean(level);}
    @AfterBatch(batch="ffm_pact_defense") public static void cleanDefense(ServerLevel level){clean(level);}
    @AfterBatch(batch="ffm_pact_flight") public static void cleanFlight(ServerLevel level){clean(level);}
    @AfterBatch(batch="ffm_pact_sunrise") public static void cleanSunrise(ServerLevel level){clean(level);}

    @GameTest(template="test_empty",batch="ffm_wither_equipment")
    public static void witherEquipmentIsHardOnlyTenPercentBowsAndHalfShieldedSwords(GameTestHelper h){
        var skeleton=h.spawn(EntityType.WITHER_SKELETON,5,2,5);skeleton.setNoAi(true);
        for(var difficulty:List.of(Difficulty.EASY,Difficulty.NORMAL,Difficulty.HARD)){
            h.getLevel().getServer().setDifficulty(difficulty,true);var random=RandomSource.create(24915);int bows=0,swords=0,shields=0,stone=0,iron=0,gold=0;
            for(int n=0;n<10000;n++){
                WitherCombat.equip(skeleton,random);var weapon=skeleton.getMainHandItem();
                if(weapon.is(Items.BOW)){bows++;h.assertTrue(skeleton.getOffhandItem().isEmpty(),"Archers never receive shields");}
                else {swords++;if(skeleton.getOffhandItem().is(Items.SHIELD))shields++;
                    if(weapon.is(Items.STONE_SWORD))stone++;else if(weapon.is(Items.IRON_SWORD))iron++;else if(weapon.is(Items.GOLDEN_SWORD))gold++;
                    else h.fail("Wither swords must be stone, iron or gold");}
            }
            h.assertTrue(difficulty==Difficulty.HARD?bows>900&&bows<1100:bows==0,"Only Hard has approximately ten percent bow users");
            h.assertTrue(Math.abs(shields/(double)swords-.5)<.025,"Half of sword users receive a shield");
            h.assertTrue(Math.abs(stone/(double)swords-.8)<.025&&Math.abs(iron/(double)swords-.15)<.025&&Math.abs(gold/(double)swords-.05)<.02,"Sword materials follow 80/15/5 weights");
        }
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ffm_wither_arrows")
    public static void witherArrowsHalveDirectDamageAndApplyFiveSecondLevelOneEffectsOnlyOnHard(GameTestHelper h){
        h.getLevel().getServer().setDifficulty(Difficulty.HARD,true);
        var wither=h.spawn(EntityType.WITHER_SKELETON,4,2,4);wither.setNoAi(true);
        var target=h.spawn(EntityType.PIG,8,2,4);target.setNoAi(true);var arrow=new Arrow(h.getLevel(),wither,new ItemStack(Items.ARROW),new ItemStack(Items.BOW));
        arrow.setDeltaMovement(1,0,0);h.getLevel().addFreshEntity(arrow);
        h.assertTrue(target.hurt(h.getLevel().damageSources().arrow(arrow,wither),8),"A real accepted arrow damage transaction succeeds");
        h.assertTrue(Math.abs(target.getHealth()-6)<.001,"Eight incoming damage becomes exactly four before armor");
        for(var effect:List.of(MobEffects.WITHER,MobEffects.MOVEMENT_SLOWDOWN)){
            var active=target.getEffect(effect);h.assertTrue(active!=null&&active.getDuration()==100&&active.getAmplifier()==0&&active.isVisible(),"Hard arrows apply visible level I effects for exactly five seconds");
        }
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL,true);
        var normal=h.spawn(EntityType.PIG,10,2,4);normal.setNoAi(true);
        h.assertTrue(normal.hurt(h.getLevel().damageSources().arrow(arrow,wither),8),"Normal arrow damage succeeds");
        h.assertTrue(normal.getHealth()==2&&normal.getActiveEffects().isEmpty(),"Changing away from Hard disables the whole special arrow feature");h.succeed();
    }
    @GameTest(template="test_empty",batch="ffm_wither_ranged",timeoutTicks=220)
    public static void actualWitherBowAiFiresAndDebuffsAnEnemy(GameTestHelper h){
        floor(h);h.getLevel().getServer().setDifficulty(Difficulty.HARD,true);
        var wither=h.spawn(EntityType.WITHER_SKELETON,4,2,8);wither.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));wither.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        var golem=h.spawn(EntityType.IRON_GOLEM,15,2,8);golem.setNoAi(true);wither.setTarget(golem);
        h.succeedWhen(()->h.assertTrue(golem.getHealth()<golem.getMaxHealth()&&golem.hasEffect(MobEffects.WITHER)&&golem.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"World-ticked bow AI lands a projectile with both effects"));
    }
    @GameTest(template="test_empty",batch="ffm_wither_shield",timeoutTicks=450)
    public static void shieldUsersAdvanceCrouchedThenSprintAndHitThePlayer(GameTestHelper h){
        floor(h);h.getLevel().setDayTime(18000);var player=player(h,18,8);
        var wither=h.spawn(EntityType.WITHER_SKELETON,3,2,8);wither.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE_SWORD));wither.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));wither.setTarget(player);
        double start=wither.getX();boolean[] blocking={false},charged={false};
        h.onEachTick(()->{
            if(wither.isBlocking()&&wither.isCrouching()&&wither.getX()>start+.5&&wither.distanceToSqr(player)>25)blocking[0]=true;
            if(blocking[0]&&wither.isSprinting()&&!wither.isCrouching()&&!wither.isUsingItem())charged[0]=true;
        });
        h.succeedWhen(()->h.assertTrue(blocking[0]&&charged[0]&&player.getHealth()<20,"Real shield AI walks, changes stance within five blocks, sprints and lands a melee hit"));
    }
    @GameTest(template="test_empty",batch="ffm_wither_block",timeoutTicks=40)
    public static void shieldBlocksFrontHitsButNotRearHitsAndLosesDurability(GameTestHelper h){
        floor(h);var wither=h.spawn(EntityType.WITHER_SKELETON,8,2,8);wither.setNoAi(true);wither.setYRot(-90);wither.setYHeadRot(-90);
        wither.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));wither.startUsingItem(InteractionHand.OFF_HAND);
        var front=h.makeMockPlayer(GameType.SURVIVAL);front.moveTo(wither.position().add(6,0,0));
        var back=h.makeMockPlayer(GameType.SURVIVAL);back.moveTo(wither.position().add(-6,0,0));
        h.runAfterDelay(8,()->{
            float health=wither.getHealth();wither.hurt(h.getLevel().damageSources().playerAttack(front),6);
            h.assertTrue(wither.getHealth()==health,"The active shield blocks a frontal strike");
            h.assertTrue(wither.getOffhandItem().getDamageValue()>=7,"Blocked attacks spend real shield durability");
            wither.hurt(h.getLevel().damageSources().playerAttack(back),4);
            h.assertTrue(wither.getHealth()<health,"The same shield does not grant protection from behind");h.succeed();
        });
    }
    @GameTest(template="test_empty",batch="ffm_pact_contract")
    public static void phantomPactConsumesOneEyeCannotBeStolenAndSurvivesNbt(GameTestHelper h){
        floor(h);var owner=player(h,8,8);var other=player(h,10,8);var phantom=bind(h,owner,false);
        h.assertTrue(owner.getMainHandItem().getCount()==3,"One successful pact consumes one eye");
        other.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.ENDER_EYE,5));
        for(int n=0;n<100;n++)PhantomPact.interact(other,InteractionHand.MAIN_HAND,phantom);
        h.assertTrue(other.getMainHandItem().getCount()==5&&owner.getUUID().equals(PhantomPact.ownerId(phantom)),"Competing interactions neither steal the pact nor consume eyes");
        var saved=phantom.saveWithoutId(new CompoundTag());phantom.discard();var loaded=EntityType.PHANTOM.create(h.getLevel());loaded.load(saved);h.getLevel().addFreshEntity(loaded);
        h.assertTrue(owner.getUUID().equals(PhantomPact.ownerId(loaded)),"The defensive owner survives entity save/load");
        h.getLevel().setDayTime(6000);PhantomPact.tick(loaded);h.assertFalse(PhantomPact.bound(loaded),"A daylight reload expires the old night's pact");
        h.assertFalse(PhantomPact.interact(owner,InteractionHand.MAIN_HAND,loaded).consumesAction(),"Eyes do not start a daytime pact");
        h.assertTrue(owner.getMainHandItem().getCount()==3,"An invalid daytime interaction does not spend another eye");h.succeed();
    }
    @GameTest(template="test_empty",batch="ffm_pact_defense",timeoutTicks=120)
    public static void guardianOnlyRetaliatesAfterItsPlayerActuallyTakesAHit(GameTestHelper h){
        floor(h);var owner=player(h,8,8);var phantom=bind(h,owner,false);var zombie=h.spawn(EntityType.ZOMBIE,12,2,8);zombie.setNoAi(true);
        phantom.setTarget(zombie);h.assertTrue(phantom.getTarget()==null,"A guardian cannot attack an unprovoked nearby monster");
        h.runAfterDelay(65,()->{
        h.assertTrue(owner.hurt(h.getLevel().damageSources().mobAttack(zombie),2),"The owner actually takes the initiating hit");
        h.assertTrue(phantom.getTarget()==zombie&&PhantomPact.mayAttack(phantom,zombie),"Only after the hit does the attacker become a valid target");
        float health=owner.getHealth();phantom.doHurtTarget(owner);h.assertTrue(owner.getHealth()==health,"Even a forced attack cannot damage the protected player");
        var neutral=h.spawn(EntityType.COW,14,2,8);phantom.setTarget(neutral);h.assertTrue(phantom.getTarget()==null,"A second unprovoked creature remains protected from the guardian");h.succeed();});
    }
    @GameTest(template="test_empty",batch="ffm_pact_flight",timeoutTicks=450)
    public static void nativePhantomFlightActuallyDefendsTheOwner(GameTestHelper h){
        floor(h);var owner=player(h,10,8);var phantom=bind(h,owner,true);var attacker=h.spawn(EntityType.ZOMBIE,8,2,8);attacker.setNoAi(true);
        h.runAfterDelay(65,()->h.assertTrue(owner.hurt(h.getLevel().damageSources().mobAttack(attacker),2),"The owner takes real damage after login protection ends"));
        h.succeedWhen(()->h.assertTrue(attacker.getHealth()<attacker.getMaxHealth()&&owner.getHealth()>=18,"Native flying/sweeping AI hits the aggressor and leaves its owner unharmed"));
    }
    @GameTest(template="test_empty",batch="ffm_pact_sunrise",timeoutTicks=140)
    public static void sunriseEndsThePactAndRestoresNormalSunBurning(GameTestHelper h){
        floor(h);var owner=player(h,8,8);var phantom=bind(h,owner,true);h.getLevel().setDayTime(24000);
        h.succeedWhen(()->h.assertTrue(!PhantomPact.bound(phantom)&&phantom.isOnFire(),"At sunrise the pact ends and ordinary exposed phantoms burn"));
    }
}
