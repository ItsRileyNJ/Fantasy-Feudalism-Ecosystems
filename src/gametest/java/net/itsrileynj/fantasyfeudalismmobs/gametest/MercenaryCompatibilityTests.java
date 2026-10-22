package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.UUID;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import io.netty.channel.embedded.EmbeddedChannel;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Registered only with the user-supplied Mercenaries mod; no false passes when it is absent. */
@EventBusSubscriber(modid=FantasyFeudalismMobs.MOD_ID, bus=EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class MercenaryCompatibilityTests {
    @SubscribeEvent public static void register(RegisterGameTestsEvent event) {
        if (ModList.get().isLoaded("villagerscontract")) event.register(MercenaryCompatibilityTests.class);
    }
    private static Mob guard(GameTestHelper h) {
        for(int x=0;x<18;x++)for(int z=0;z<18;z++)h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
        EntityType<?> type=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("villagerscontract:defender"));
        Mob guard=(Mob)type.create(h.getLevel());guard.moveTo(h.absoluteVec(new Vec3(8,2,8)));guard.setNoAi(true);
        invoke(guard,"bind",new Class<?>[]{BlockPos.class,UUID.class,BlockPos.class},null,new UUID(0,7),guard.blockPosition());
        h.getLevel().addFreshEntity(guard);return guard;
    }
    private static Object invoke(Object object,String method,Class<?>[] types,Object...args) {
        try{return object.getClass().getMethod(method,types).invoke(object,args);}
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    private static boolean hostile(Mob guard,LivingEntity target) {
        return (boolean)invoke(guard,"isHostile",new Class<?>[]{LivingEntity.class},target);
    }
    private static Spider spider(GameTestHelper h,boolean cave,boolean tame) {
        Spider spider=cave?h.spawn(EntityType.CAVE_SPIDER,5,2,8):h.spawn(EntityType.SPIDER,5,2,8);spider.setNoAi(true);
        if(tame){SpiderCompanion.tame(spider,new UUID(0,7));SpiderCompanion.setSitting(spider,false);}return spider;
    }
    @GameTest(template="test_empty",templateNamespace=FantasyFeudalismMobs.MOD_ID,batch="ffm_compat")
    public static void hiredGuardsRecognizeBothCompanionTypes(GameTestHelper h) {
        Mob guard=guard(h);
        for(boolean cave:new boolean[]{false,true}) {
            Spider pet=spider(h,cave,true);
            h.assertFalse(hostile(guard,pet),"Tamed spiders are not hostile mercenary targets");
            h.assertTrue(guard.isAlliedTo(pet),"Guards recognize spider companions like other tamed pets");
            h.assertFalse(guard.canAttack(pet),"Guard combat validation rejects companion targets");
            guard.setTarget(pet);h.assertTrue(guard.getTarget()==null,"Guard does not retain a companion target");pet.discard();
        }
        h.succeed();
    }
    @GameTest(template="test_empty",templateNamespace=FantasyFeudalismMobs.MOD_ID,batch="ffm_compat")
    public static void companionsRejectFriendlyGuards(GameTestHelper h) {
        Mob guard=guard(h);
        for(boolean cave:new boolean[]{false,true}) {
            Spider pet=spider(h,cave,true);
            h.assertFalse(SpiderGoals.mayAttack(pet,guard),"Companion recognizes the guard's alliance");
            pet.setTarget(guard);h.assertTrue(pet.getTarget()==null,"Retaliation/owner defense cannot retain a friendly guard");pet.discard();
        }
        h.succeed();
    }
    @GameTest(template="test_empty",templateNamespace=FantasyFeudalismMobs.MOD_ID,batch="ffm_compat")
    public static void guardFriendlyFireProtectionIncludesCompanions(GameTestHelper h) {
        Mob guard=guard(h);Spider pet=spider(h,false,true);float health=pet.getHealth();
        pet.hurt(guard.damageSources().mobAttack(guard),4);
        h.assertTrue(pet.getHealth()==health,"Mercenary friendly-fire cancellation protects the tamed spider");h.succeed();
    }
    @GameTest(template="test_empty",templateNamespace=FantasyFeudalismMobs.MOD_ID,batch="ffm_compat")
    public static void companionsDoNotDebuffAlliedGuards(GameTestHelper h) {
        Mob guard=guard(h);Spider pet=spider(h,true,true);MobRules.afterHit(pet,guard);
        h.assertTrue(guard.getActiveEffects().isEmpty(),"Companion debuffs apply only to hostile targets, not allied mercenaries");h.succeed();
    }
    @GameTest(template="test_empty",templateNamespace=FantasyFeudalismMobs.MOD_ID,batch="ffm_compat")
    public static void alliedGuardsDoNotBreakTheCompanionQuietRule(GameTestHelper h) {
        Mob guard=guard(h);Spider pet=spider(h,false,true);
        h.assertFalse(SpiderCompanion.hostileNearby(pet),"An allied guard is not a hostile sound trigger");
        var zombie=h.spawn(EntityType.ZOMBIE,9,2,8);zombie.setNoAi(true);
        h.assertTrue(SpiderCompanion.hostileNearby(pet),"A genuine hostile still enables companion sounds");h.succeed();
    }
    @GameTest(template="test_empty",templateNamespace=FantasyFeudalismMobs.MOD_ID,batch="ffm_compat")
    public static void wildSpidersAndRealUndeadRemainEnemies(GameTestHelper h) {
        Mob guard=guard(h);
        for(boolean cave:new boolean[]{false,true})h.assertTrue(hostile(guard,spider(h,cave,false)),"Wild spiders remain hostile");
        var zombie=h.spawn(EntityType.ZOMBIE,12,2,8);zombie.setNoAi(true);
        var skeleton=h.spawn(EntityType.SKELETON,13,2,8);skeleton.setNoAi(true);
        h.assertTrue(guard.canAttack(zombie)&&guard.canAttack(skeleton),"Both modified undead families remain mercenary enemies");
        h.assertTrue(zombie.targetSelector.getAvailableGoals().stream().anyMatch(g->g.getGoal().getClass().getSimpleName().equals("EnemyGuardTargetGoal")),"Mercenary hostile-target AI coexists with zombie modifications");h.succeed();
    }
    @GameTest(template="test_empty",templateNamespace=FantasyFeudalismMobs.MOD_ID,batch="ffm_compat")
    public static void mercenaryArrowsDoNotReceiveSkeletonSpeedMultiplier(GameTestHelper h) {
        Mob guard=guard(h);
        Arrow arrow=new Arrow(h.getLevel(),guard,new ItemStack(Items.ARROW),new ItemStack(Items.CROSSBOW));
        arrow.setDeltaMovement(1,0,0);h.getLevel().addFreshEntity(arrow);
        h.assertTrue(Math.abs(arrow.getDeltaMovement().x-1)<.00001&&!arrow.getPersistentData().getBoolean("ffm_faster"),"Mercenary ballistics are not multiplied by the skeleton rule");
        h.assertFalse(guard.getPersistentData().getBoolean("ffm_armor_rolled"),"Undead armor rolls do not apply to mercenaries");h.succeed();
    }
    @GameTest(template="test_empty",templateNamespace=FantasyFeudalismMobs.MOD_ID,batch="ffm_compat")
    public static void guardsClassifySkeletonsByTheirActualWeapon(GameTestHelper h) throws Exception {
        var skeleton=h.spawn(EntityType.SKELETON,4,2,4);skeleton.setNoAi(true);
        var method=Class.forName("dev.villagerscontract.KitingCrossbowGoal").getMethod("preferredDistance",LivingEntity.class);
        for(var row:java.util.List.of(new Object[]{Items.BOW,20.0},new Object[]{Items.CROSSBOW,12.0},new Object[]{Items.STONE_SWORD,7.0})) {
            skeleton.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack((Item)row[0]));
            h.assertTrue((double)method.invoke(null,skeleton)==(double)row[1],"Guard spacing follows the equipped skeleton weapon: "+row[0]);
        }
        h.succeed();
    }
    @GameTest(template="test_empty",templateNamespace=FantasyFeudalismMobs.MOD_ID,batch="ffm_compat")
    public static void rejectedWithdrawCorrectsUnchangedHiddenInventory(GameTestHelper h) {
        rejectedPrediction(h,false);
    }
    @GameTest(template="test_empty",templateNamespace=FantasyFeudalismMobs.MOD_ID,batch="ffm_compat")
    public static void rejectedBeeBottleSwapCorrectsUnchangedHotbar(GameTestHelper h) {
        rejectedPrediction(h,true);
    }
    @GameTest(template="test_empty",templateNamespace=FantasyFeudalismMobs.MOD_ID,batch="ffm_compat")
    public static void hiredGuardsRespectTemporaryPhantomOwnership(GameTestHelper h){
        var guard=guard(h);var phantom=h.spawn(EntityType.PHANTOM,5,4,5);phantom.setNoAi(true);
        var pact=new net.minecraft.nbt.CompoundTag();pact.putUUID("Owner",new UUID(0,7));phantom.getPersistentData().put(PhantomPact.PACT,pact);
        h.assertFalse(hostile(guard,phantom),"A pact phantom is excluded from proactive monster hunting");
        h.assertTrue(guard.isAlliedTo(phantom)&&!guard.canAttack(phantom),"A hired guard protects its owner's guardian");
        pact.putUUID("Owner",new UUID(0,8));
        h.assertFalse(guard.isAlliedTo(phantom),"Other players' mercenaries are not falsely allied to the guardian");
        h.assertFalse(guard.canAttack(phantom),"Guards do not proactively hunt another player's peaceful guardian");h.succeed();
    }
    @GameTest(template="test_empty",templateNamespace=FantasyFeudalismMobs.MOD_ID,batch="ffm_compat")
    public static void guardsClassifyWitherBowsAndShieldedSwordsByTheirWeapon(GameTestHelper h)throws Exception{
        var skeleton=h.spawn(EntityType.WITHER_SKELETON,4,2,4);skeleton.setNoAi(true);
        var method=Class.forName("dev.villagerscontract.KitingCrossbowGoal").getMethod("preferredDistance",LivingEntity.class);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
        h.assertTrue((double)method.invoke(null,skeleton)==20.0,"Guard kiting recognizes a wither archer");
        skeleton.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));skeleton.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));
        h.assertTrue((double)method.invoke(null,skeleton)==7.0,"A shield does not misclassify a melee wither as an archer");h.succeed();
    }
    private static Object mountedFixture(String name,Object...args){
        try{
            var type=Class.forName("dev.villagerscontract.MountedGuardGameTests");
            var method=java.util.Arrays.stream(type.getDeclaredMethods()).filter(m->m.getName().equals(name)&&m.getParameterCount()==args.length).findFirst().orElseThrow();
            method.setAccessible(true);return method.invoke(null,args);
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    @GameTest(template="combat_arena",templateNamespace="villagerscontract",batch="ffm_mounted_bow_audit",skyAccess=true,timeoutTicks=450)
    public static void mountedMercenaryKeepsSafeBowSeparationAcrossGrass(GameTestHelper h)throws Exception{mountedWeapon(h,true);}
    @GameTest(template="combat_arena",templateNamespace="villagerscontract",batch="ffm_mounted_crossbow_audit",skyAccess=true,timeoutTicks=450)
    public static void mountedMercenaryHoldsSafeGroundAgainstAnExplicitCrossbow(GameTestHelper h)throws Exception{mountedWeapon(h,false);}
    @SuppressWarnings({"unchecked","rawtypes"})
    private static void mountedWeapon(GameTestHelper h,boolean bow)throws Exception{
        mountedFixture("grassyStep",h);var owner=(net.minecraft.server.level.ServerPlayer)mountedFixture("player",h,8,22);
        var guard=(Mob)mountedFixture("guard",h,owner,8,22);
        var horse=(net.minecraft.world.entity.animal.horse.Horse)mountedFixture("horse",h,owner.getUUID(),10,22);
        horse.setPos(h.absoluteVec(new Vec3(20.32,2,22.043)));mountedFixture("ride",h,guard,horse);horse.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.225);
        var enemy=h.spawn(EntityType.SKELETON,3,2,22);enemy.setNoAi(true);enemy.setPersistenceRequired();enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);enemy.setHealth(200);
        enemy.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(bow?Items.BOW:Items.CROSSBOW));
        var order=Class.forName("dev.villagerscontract.GuardOrder");invoke(guard,"issueOrder",new Class<?>[]{order,net.minecraft.world.entity.player.Player.class},Enum.valueOf((Class)order,"FOLLOW"),owner);
        guard.setTarget(enemy);guard.setNoAi(false);float health=horse.getHealth();Vec3 start=horse.position();
        h.succeedWhen(()->{
            h.assertTrue(guard.getVehicle()==horse&&horse.getHealth()==health&&!guard.isInWall(),"Native mounted navigation keeps the rider safe");
            if(bow)h.assertTrue(guard.distanceToSqr(enemy)>=19*19&&horse.position().distanceToSqr(start)>1,"A bow threat requires real separation near twenty blocks, accepting safe diagonal routes");
            else h.assertTrue(enemy.getHealth()<200&&h.relativeVec(horse.position()).x<24,"At seventeen blocks a crossbow threat is already outside its twelve-block retreat threshold; the guard holds and fires");
        });
    }
    private static void rejectedPrediction(GameTestHelper h,boolean swap) {
        Mob guard=guard(h);var player=h.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        try {
            player.moveTo(guard.position().add(1,0,0));
            player.getInventory().clearContent();
            if(swap)player.getInventory().setItem(0,new ItemStack(FantasyFeudalismMobs.BEE_BOTTLE.get(),3));
            player.setShiftKeyDown(true);guard.interact(player,InteractionHand.MAIN_HAND);player.setShiftKeyDown(false);
            var menu=player.containerMenu;h.assertTrue(menu.getClass().getSimpleName().equals("GuardMenu"),"Real guard menu opened");
            var channel=(EmbeddedChannel)player.connection.getConnection().channel();channel.runPendingTasks();
            while(channel.readOutbound()!=null){}
            menu.clicked(11,0,swap?ClickType.SWAP:ClickType.QUICK_MOVE,player);channel.runPendingTasks();
            boolean corrected=false;Object packet;
            while((packet=channel.readOutbound())!=null)
                if(packet instanceof ClientboundContainerSetSlotPacket update && update.getContainerId()==-2
                        && update.getSlot()==(swap?0:20))
                    corrected=swap?update.getItem().is(FantasyFeudalismMobs.BEE_BOTTLE)&&update.getItem().getCount()==3:update.getItem().isEmpty();
            h.assertTrue(corrected,"Rejected prediction receives explicit authoritative private-inventory correction even without a server change");
            menu.broadcastChanges();channel.runPendingTasks();
            while((packet=channel.readOutbound())!=null)
                h.assertFalse(packet instanceof ClientboundContainerSetSlotPacket update&&update.getContainerId()==-2,"Idle menus do not resend corrections every tick");
            h.succeed();
        } finally {player.closeContainer();h.getLevel().getServer().getPlayerList().remove(player);}
    }
}
