package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.UUID;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AdvancementTests {
    static ServerPlayer player(GameTestHelper h){
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"adv-test"),false);
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        for(int i=0;i<61;i++)p.tick(); // Let native post-login damage immunity expire.
        p.moveTo(h.absoluteVec(new Vec3(8,2,8)));return p;
    }
    static void remove(ServerPlayer p){p.server.getPlayerList().remove(p);}
    public static boolean complete(ServerPlayer p,String name){var holder=p.server.getAdvancements().get(EcosystemAchievements.id(name));return holder!=null&&p.getAdvancements().getOrStartProgress(holder).isDone();}
    private static <T extends Mob> T still(GameTestHelper h,EntityType<T> type,int x){var mob=h.spawn(type,x,2,8);mob.setNoAi(true);return mob;}
    private static void floor(GameTestHelper h){for(int x=0;x<18;x++)for(int z=0;z<18;z++)h.setBlock(x,1,z,Blocks.GRASS_BLOCK);}

    @GameTest(template="test_empty",batch="adv_metadata")
    public static void approvedTreeLoadsWithoutJoinRewardsAndMasterRequiresEveryGoal(GameTestHelper h){
        var p=player(h);
        try{
            long count=p.server.getAdvancements().getAllAdvancements().stream().filter(a->a.id().getNamespace().equals(FantasyFeudalismMobs.MOD_ID)&&a.id().getPath().startsWith("ecosystems/")).count();
            h.assertTrue(count==17,"Exactly seventeen approved advancements load: "+count);
            for(String name:EcosystemAchievements.GOALS)h.assertFalse(complete(p,name),"Login cannot earn "+name);
            for(String name:EcosystemAchievements.GOALS){
                h.assertFalse(complete(p,"master_naturalist"),"Master waits for every goal");
                var holder=p.server.getAdvancements().get(EcosystemAchievements.id(name));
                for(String criterion:holder.value().criteria().keySet())p.getAdvancements().award(holder,criterion);
            }
            h.assertTrue(complete(p,"master_naturalist"),"Native earn events complete Master after all sixteen goals");
            p.getAdvancements().save();h.succeed();
        }finally{remove(p);}
    }
    @GameTest(template="test_empty",batch="adv_captures")
    public static void actualBeeCaptureAndSpiderActionsCreditOnlyTheActingPlayer(GameTestHelper h){
        var p=player(h);var other=player(h);
        try{
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE));
            var bee=still(h,EntityType.BEE,4);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract(p,InteractionHand.MAIN_HAND,bee));
            h.assertTrue(complete(p,"bottled_buzz")&&!complete(other,"bottled_buzz"),"Capturing the bee awards only its catcher");
            for(boolean cave:new boolean[]{false,true}){
                Spider spider=cave?still(h,EntityType.CAVE_SPIDER,6):still(h,EntityType.SPIDER,5);
                long seed=0;while(net.minecraft.util.RandomSource.create(seed).nextInt(100)>=33)seed++;
                spider.getRandom().setSeed(seed);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FantasyFeudalismMobs.BEE_BOTTLE.get()));
                SpiderCompanion.interact(p,InteractionHand.MAIN_HAND,spider);
                h.assertTrue(complete(p,"friendly_fangs")&&complete(p,"eight_legged_alliance")==cave,"Alliance remembers both species");
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));SpiderCompanion.harvest(p,InteractionHand.MAIN_HAND,spider,true);
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE));SpiderCompanion.harvest(p,InteractionHand.MAIN_HAND,spider,false);
                other.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));
                h.assertFalse(SpiderCompanion.harvest(other,InteractionHand.MAIN_HAND,spider,true)||complete(other,"silk_supplier"),"Failed cooldown harvest awards nothing");
            }
            h.assertTrue(complete(p,"silk_supplier")&&complete(p,"venom_on_tap"),"Both successful harvests award their own achievements");h.succeed();
        }finally{remove(p);remove(other);}
    }
    @GameTest(template="test_empty",batch="adv_bats")
    public static void batCaptureThirdInsertionAndFertilizerHaveSeparateSuccessTriggers(GameTestHelper h){
        floor(h);long time=h.getLevel().getDayTime();h.getLevel().setDayTime(6000);var p=player(h);var other=player(h);
        var pos=h.absolutePos(new BlockPos(8,2,8));h.setBlock(8,2,8,FantasyFeudalismMobs.BAT_ENCLOSURE.get());
        try{
            for(int i=0;i<4;i++){
                var who=i==3?other:p;var bat=h.spawn(EntityType.BAT,4+i,3,5);bat.setNoAi(true);
                who.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FantasyFeudalismMobs.BAT_NET.get()));
                BatNetItem.capture(who,InteractionHand.MAIN_HAND,bat);
                who.gameMode.useItemOn(who,h.getLevel(),who.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
                if(i<2)h.assertFalse(complete(p,"full_house"),"Fewer than three bats cannot complete Full House");
            }
            h.assertTrue(complete(p,"night_collector")&&complete(p,"full_house")&&!complete(other,"full_house"),"Third successful insertion counts; a rejected fourth does not");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE));
            ((BatEnclosureBlockEntity)h.getLevel().getBlockEntity(pos)).feed(p,InteractionHand.MAIN_HAND);
            h.assertTrue(complete(p,"natural_helpers"),"Actual fertilizer production awards Natural Helpers");h.succeed();
        }finally{remove(p);remove(other);h.getLevel().setDayTime(time);}
    }
    @GameTest(template="test_empty",batch="adv_pets")
    public static void ownedCatKillAndSittingWatchDogCreditTheirOwner(GameTestHelper h){
        floor(h);var p=player(h);var other=player(h);
        try{
            var cat=still(h,EntityType.CAT,4);cat.setTame(true,true);cat.setOwnerUUID(p.getUUID());
            var mouse=h.spawn(FantasyFeudalismMobs.FIELD_MOUSE.get(),5,2,8);cat.doHurtTarget(mouse);
            h.assertTrue(!mouse.isAlive()&&complete(p,"pest_control")&&!complete(other,"pest_control"),"Cat's one-hit mouse kill credits its owner only");
            var dog=still(h,EntityType.WOLF,12);dog.setTame(true,true);dog.setOwnerUUID(p.getUUID());dog.setOrderedToSit(true);dog.setInSittingPose(true);
            var fox=still(h,EntityType.FOX,9);var chicken=still(h,EntityType.CHICKEN,11);fox.setTarget(chicken);
            h.assertTrue(fox.getTarget()==null&&complete(p,"watch_dog")&&!complete(other,"watch_dog"),"Actual prevented livestock hunt credits the sitting dog's owner");h.succeed();
        }finally{remove(p);remove(other);}
    }
    @GameTest(template="test_empty",batch="adv_pact")
    public static void phantomPactRequiresSuccessfulNightBinding(GameTestHelper h){
        var p=player(h);long time=h.getLevel().getDayTime();
        try{
            var phantom=still(h,EntityType.PHANTOM,4);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.ENDER_EYE,2));
            h.getLevel().setDayTime(6000);PhantomPact.interact(p,InteractionHand.MAIN_HAND,phantom);
            h.assertFalse(complete(p,"night_pact"),"Failed daytime pact awards nothing");
            h.getLevel().setDayTime(18000);PhantomPact.interact(p,InteractionHand.MAIN_HAND,phantom);
            h.assertTrue(complete(p,"night_pact"),"A real night pact awards its binder");h.succeed();
        }finally{remove(p);h.getLevel().setDayTime(time);}
    }
    @GameTest(template="test_empty",batch="adv_foal")
    public static void automaticFoalCreditsOwnersAndManualBreedingDoesNot(GameTestHelper h){
        floor(h);var p=player(h);
        try{
            var a=still(h,EntityType.HORSE,5);var b=still(h,EntityType.HORSE,6);
            a.setTamed(true);b.setTamed(true);a.setOwnerUUID(p.getUUID());b.setOwnerUUID(p.getUUID());
            a.setInLove(p);b.setInLove(p);a.spawnChildFromBreeding(h.getLevel(),b);
            h.assertFalse(complete(p,"new_arrival"),"Player-fed birth is not automatic");
            a.setAge(0);b.setAge(0);a.setNoAi(false);b.setNoAi(false);
            for(var horse:java.util.List.of(a,b)){HorseLife.initialize(horse);horse.getPersistentData().putLong("ffm_horse_next_breed",0);}
            HorseLife.grazed(a);h.assertTrue(HorseLife.automaticPair(a,b),"Fixture is a grazing-triggered automatic pair");
            a.spawnChildFromBreeding(h.getLevel(),b);
            h.assertTrue(complete(p,"new_arrival"),"Native automatic birth credits the owner");h.succeed();
        }finally{remove(p);}
    }
    @GameTest(template="test_empty",batch="adv_endo")
    public static void hardEndermanActuallyOneShotsFullHealthButArmorTotemAndPartialHealthRemainDistinct(GameTestHelper h){
        floor(h);var server=h.getLevel().getServer();var old=h.getLevel().getDifficulty();server.setDifficulty(Difficulty.HARD,true);
        try{
            for(int mode=0;mode<4;mode++){
                var p=player(h);try{
                    if(mode==1)p.setHealth(10);
                    if(mode==2){p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Items.IRON_LEGGINGS));p.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.IRON_BOOTS));}
                    if(mode==3)p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.TOTEM_OF_UNDYING));
                    p.doTick(); // Native equipment changes update armor attributes during the player tick.
                    var enderman=still(h,EntityType.ENDERMAN,6);enderman.doHurtTarget(p);
                    h.assertTrue(complete(p,"everyones_got_a_plan_until")== (mode==0),"Only an actual full-health death qualifies, fixture "+mode+" health="+p.getHealth());
                    h.assertTrue(p.isAlive()==(mode>=2),"Armor and the native totem still prevent death");enderman.discard();
                }finally{remove(p);}
            }
            h.succeed();
        }finally{server.setDifficulty(old,true);}
    }
    @GameTest(template="test_empty",batch="adv_endo_normal")
    public static void normalAndEasyEndermanDamageAreUnchanged(GameTestHelper h){
        floor(h);var server=h.getLevel().getServer();var old=h.getLevel().getDifficulty();
        try{
            for(var difficulty:new Difficulty[]{Difficulty.NORMAL,Difficulty.EASY}){
                server.setDifficulty(difficulty,true);var p=player(h);
                try{var enderman=still(h,EntityType.ENDERMAN,6);enderman.doHurtTarget(p);
                    float damage=difficulty==Difficulty.NORMAL?9.45F:6.075F;
                    h.assertTrue(Math.abs(p.getHealth()-(20-damage))<.001&&!complete(p,"everyones_got_a_plan_until"),"Unchanged native difficulty scaling: "+difficulty+" health="+p.getHealth());enderman.discard();
                }finally{remove(p);}
            }h.succeed();
        }finally{server.setDifficulty(old,true);}
    }
    @GameTest(template="test_empty",batch="adv_shield",timeoutTicks=30)
    public static void axeMustDisableAnActuallyRaisedWitherShield(GameTestHelper h){
        var p=player(h);var wither=still(h,EntityType.WITHER_SKELETON,8);wither.setYHeadRot(-90);
        p.moveTo(wither.position().add(4,0,0));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_AXE));
        wither.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE_SWORD));wither.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));wither.startUsingItem(InteractionHand.OFF_HAND);
        h.runAfterDelay(8,()->{try{wither.hurt(h.getLevel().damageSources().playerAttack(p),5);
            h.assertTrue(!WitherCombat.shieldReady(wither)&&complete(p,"shield_breaker"),"Native axe shield block awards Shield Breaker");h.succeed();
        }finally{remove(p);}});
    }
    @GameTest(template="test_empty",batch="adv_range_flight",timeoutTicks=330)
    public static void realSkeletonArrowCanEarnTheChallengeAtThirtyFiveBlocks(GameTestHelper h){
        for(int x=0;x<43;x++)for(int z=5;z<12;z++)h.setBlock(x,1,z,Blocks.STONE);
        var p=player(h);var skeleton=still(h,EntityType.SKELETON,4);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
        p.moveTo(skeleton.position().add(35,0,0));skeleton.setTarget(p);int[] ticks={0};boolean[] done={false};
        h.onEachTick(()->{
            if(done[0])return;
            if(complete(p,"thats_where_id_be")){done[0]=true;remove(p);h.succeed();return;}
            if(ticks[0]++%20==0)skeleton.performRangedAttack(p,1);
        });
        h.runAtTickTime(310,()->{if(!done[0]){try{h.assertTrue(complete(p,"thats_where_id_be"),"Native skeleton arrow must actually reach and hit its target at maximum range");}finally{done[0]=true;remove(p);}}});
    }
    @GameTest(template="test_empty",batch="adv_range")
    public static void maximumRangeIsRecordedAtFiringAndSurvivesArrowSaveLoad(GameTestHelper h){
        var p=player(h);var other=player(h);var skeleton=still(h,EntityType.SKELETON,4);skeleton.setTarget(p);
        try{
            for(double distance:new double[]{34.9,35.01,35}){
                p.moveTo(skeleton.position().add(distance,0,0));
                var arrow=new Arrow(h.getLevel(),skeleton,new ItemStack(Items.ARROW),new ItemStack(Items.BOW));
                EcosystemAchievements.arrowFired(arrow,skeleton);
                var loaded=EntityType.ARROW.create(h.getLevel());loaded.load(arrow.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
                p.moveTo(skeleton.position().add(3,0,0));p.invulnerableTime=0;
                p.hurt(h.getLevel().damageSources().arrow(loaded,skeleton),1);
                h.assertTrue(complete(p,"thats_where_id_be")== (distance==35),"Only exact maximum launch range counts even after movement/save-load: "+distance);
                if(distance==35){other.invulnerableTime=0;other.hurt(h.getLevel().damageSources().arrow(loaded,skeleton),1);h.assertFalse(complete(other,"thats_where_id_be"),"An intercepted arrow does not credit the wrong player");}
            }h.succeed();
        }finally{remove(p);remove(other);}
    }
}
