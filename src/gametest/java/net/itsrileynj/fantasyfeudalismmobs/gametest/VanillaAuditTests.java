package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.*;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.*;

/** Native operations and negative controls for the surfaces intercepted by this mod. */
@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VanillaAuditTests {
    private static void floor(GameTestHelper h){for(int x=0;x<24;x++)for(int z=0;z<20;z++)h.setBlock(x,1,z,Blocks.STONE_BRICKS);}
    private static net.minecraft.world.entity.player.Player player(GameTestHelper h){var p=h.makeMockPlayer(GameType.SURVIVAL);p.moveTo(h.absoluteVec(new Vec3(4,2,5)));return p;}
    @GameTest(template="test_empty",batch="ffm_audit_grief_off")
    public static void creepersRespectMobGriefingButStillDamageEntities(GameTestHelper h){
        floor(h);h.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false,h.getLevel().getServer());
        var creeper=h.spawn(EntityType.CREEPER,8,2,8);creeper.setNoAi(true);var pig=h.spawn(EntityType.PIG,9,2,8);pig.setNoAi(true);
        h.getLevel().explode(creeper,creeper.getX(),creeper.getY(),creeper.getZ(),3,Level.ExplosionInteraction.MOB);
        for(int x=4;x<13;x++)for(int z=4;z<13;z++){
            h.assertTrue(h.getBlockState(new BlockPos(x,1,z)).is(Blocks.STONE_BRICKS),"mobGriefing=false prevents moss conversion");
            h.assertTrue(h.getBlockState(new BlockPos(x,2,z)).isAir(),"mobGriefing=false prevents vegetation placement");
        }
        h.assertTrue(pig.getHealth()<pig.getMaxHealth(),"The gamerule does not turn off native explosion damage");h.succeed();
    }
    @AfterBatch(batch="ffm_audit_grief_off") public static void resetGrief(ServerLevel level){level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,level.getServer());}
    @GameTest(template="test_empty",batch="ffm_audit_grief_on")
    public static void enabledCreeperVegetationRetainsNativeBlockProperties(GameTestHelper h){
        floor(h);var pos=new BlockPos(8,1,8);var before=Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.EAST).setValue(StairBlock.WATERLOGGED,true);
        h.setBlock(pos,before);var creeper=h.spawn(EntityType.CREEPER,8,2,8);creeper.setNoAi(true);
        h.getLevel().explode(creeper,creeper.getX(),creeper.getY(),creeper.getZ(),3,Level.ExplosionInteraction.MOB);
        var after=h.getBlockState(pos);h.assertTrue(after.is(Blocks.MOSSY_STONE_BRICK_STAIRS)&&after.getValue(StairBlock.FACING)==Direction.EAST&&after.getValue(StairBlock.WATERLOGGED),"Moss retains facing and waterlogging");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void catchingALeashedBeeReturnsExactlyOneLead(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE));var bee=h.spawn(EntityType.BEE,4,2,4);bee.setNoAi(true);bee.setLeashedTo(p,true);
        for(int i=0;i<20;i++)NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(p,InteractionHand.MAIN_HAND,bee));
        h.assertTrue(p.getMainHandItem().is(FantasyFeudalismMobs.BEE_BOTTLE)&&bee.isRemoved(),"One original bee is captured once");
        int leads=h.getLevel().getEntitiesOfClass(ItemEntity.class,h.getBounds(),e->e.getItem().is(Items.LEAD)).stream().mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(leads==1,"The bee's real lead is dropped once, without loss or duplication");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void commandsKeepExplicitSkeletonEquipmentAndEnchantments(GameTestHelper h){
        for(var type:List.of(EntityType.SKELETON,EntityType.STRAY,EntityType.WITHER_SKELETON)){
            var skeleton=type.create(h.getLevel());skeleton.moveTo(h.absoluteVec(new Vec3(6,2,6)));skeleton.setNoAi(true);
            var weapon=new ItemStack(Items.DIAMOND_SWORD);weapon.enchant(h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS),5);weapon.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Command sword"));
            skeleton.setItemSlot(EquipmentSlot.MAINHAND,weapon);skeleton.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));
            h.getLevel().addFreshEntity(skeleton);
            h.assertTrue(ItemStack.matches(skeleton.getMainHandItem(),weapon)&&skeleton.getOffhandItem().is(Items.SHIELD),"Explicit equipment survives the join hook: "+type);skeleton.discard();
        }
        var unarmed=EntityType.WITHER_SKELETON.create(h.getLevel());unarmed.moveTo(h.absoluteVec(new Vec3(7,2,7)));unarmed.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));h.getLevel().addFreshEntity(unarmed);
        h.assertTrue(unarmed.getMainHandItem().isEmpty()&&unarmed.getOffhandItem().is(Items.SHIELD),"An offhand-only command loadout is also an explicit equipment choice");h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void realFinalizedSpawnsStillReceiveTheSkeletonWeaponMix(GameTestHelper h){
        floor(h);int bows=0,crossbows=0,swords=0;
        for(int i=0;i<300;i++){
            var skeleton=EntityType.SKELETON.spawn(h.getLevel(),h.absolutePos(new BlockPos(8,2,8)),MobSpawnType.SPAWN_EGG);
            h.assertTrue(skeleton!=null&&skeleton.getSpawnType()==MobSpawnType.SPAWN_EGG,"The real NeoForge finalization path records its spawn reason");
            if(skeleton.getMainHandItem().is(Items.BOW))bows++;else if(skeleton.getMainHandItem().is(Items.CROSSBOW))crossbows++;else if(skeleton.getMainHandItem().getItem() instanceof SwordItem)swords++;
            skeleton.discard();
        }
        h.assertTrue(bows>0&&crossbows>0&&swords>0,"Preserving explicit commands must not accidentally preserve every vanilla default bow on actual finalized spawns");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void playerProjectilesKeepTheirNativeLaunchVelocity(GameTestHelper h){
        var p=player(h);var arrow=new Arrow(h.getLevel(),p,new ItemStack(Items.ARROW),new ItemStack(Items.BOW));arrow.setDeltaMovement(1,.2,-.4);h.getLevel().addFreshEntity(arrow);
        h.assertTrue(arrow.getDeltaMovement().distanceTo(new Vec3(1,.2,-.4))<1e-9&&!arrow.getPersistentData().getBoolean("ffm_faster"),"Only skeleton projectiles get the speed change");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void ordinaryBoneMealStillGrowsCropsAndSpendsOneItem(GameTestHelper h){
        floor(h);h.setBlock(8,1,8,Blocks.FARMLAND);h.setBlock(8,2,8,Blocks.WHEAT);var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BONE_MEAL,3));
        var pos=h.absolutePos(new BlockPos(8,2,8));var result=Items.BONE_MEAL.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)));
        h.assertTrue(result.consumesAction()&&p.getMainHandItem().getCount()==2&&h.getBlockState(new BlockPos(8,2,8)).getValue(CropBlock.AGE)>0,"Vanilla bone meal retains native growth and inventory behavior");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void milkRemovesTheModCombatEffectsAndReturnsABucket(GameTestHelper h){
        var p=player(h);for(var effect:List.of(MobEffects.WITHER,MobEffects.MOVEMENT_SLOWDOWN,MobEffects.WEAKNESS,MobEffects.POISON))p.addEffect(new MobEffectInstance(effect,120,0));
        var result=Items.MILK_BUCKET.finishUsingItem(new ItemStack(Items.MILK_BUCKET),h.getLevel(),p);
        h.assertTrue(result.is(Items.BUCKET)&&p.getActiveEffects().isEmpty(),"Ordinary milk cures the same native effects used by the mod");h.succeed();
    }
    @GameTest(template="test_empty",batch="ffm_audit_loot_off",timeoutTicks=60)
    public static void mobLootOffSuppressesHorseCreeperAndGhastAdditions(GameTestHelper h){
        floor(h);h.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(false,h.getLevel().getServer());
        for(var type:List.of(EntityType.HORSE,EntityType.CREEPER,EntityType.GHAST)){
            var mob=h.spawn(type,8,2,8);mob.hurt(h.getLevel().damageSources().genericKill(),1000);
        }
        h.runAfterDelay(25,()->{h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,h.getBounds()).isEmpty(),"doMobLoot=false suppresses normal and mod death loot");h.succeed();});
    }
    @AfterBatch(batch="ffm_audit_loot_off") public static void resetLoot(ServerLevel level){level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(true,level.getServer());}
    @GameTest(template="test_empty",timeoutTicks=40)
    public static void axesDisableWitherShieldsForOneHundredTicks(GameTestHelper h){
        floor(h);var p=player(h);var wither=h.spawn(EntityType.WITHER_SKELETON,8,2,8);wither.setNoAi(true);wither.setYHeadRot(-90);p.moveTo(wither.position().add(4,0,0));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_AXE));
        wither.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE_SWORD));wither.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));wither.startUsingItem(InteractionHand.OFF_HAND);
        h.runAfterDelay(8,()->{
            wither.hurt(h.getLevel().damageSources().playerAttack(p),5);
            h.assertTrue(!wither.isUsingItem()&&!WitherCombat.shieldReady(wither),"A native axe strike disables the raised shield");
            h.assertTrue(wither.getPersistentData().getLong(WitherCombat.SHIELD_DISABLED)==h.getLevel().getGameTime()+100,"Disable time matches the vanilla five-second shield cooldown");
            wither.setTarget(p);var goal=new WitherCombat.ShieldAdvance(wither);goal.start();goal.tick();h.assertFalse(wither.isUsingItem(),"The shield AI cannot immediately bypass axe disabling");goal.stop();h.succeed();
        });
    }
    @GameTest(template="test_empty",timeoutTicks=40)
    public static void piercingArrowsBypassTheNewShieldLikeVanillaShields(GameTestHelper h){
        floor(h);var p=player(h);var wither=h.spawn(EntityType.WITHER_SKELETON,8,2,8);wither.setNoAi(true);wither.setYHeadRot(-90);p.moveTo(wither.position().add(4,0,0));
        wither.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));wither.startUsingItem(InteractionHand.OFF_HAND);
        var arrow=new Arrow(h.getLevel(),p,new ItemStack(Items.ARROW),new ItemStack(Items.CROSSBOW));var saved=arrow.saveWithoutId(new CompoundTag());saved.putByte("PierceLevel",(byte)1);arrow.load(saved);h.getLevel().addFreshEntity(arrow);
        h.runAfterDelay(8,()->{float health=wither.getHealth();wither.hurt(h.getLevel().damageSources().arrow(arrow,p),5);h.assertTrue(wither.getHealth()<health,"Piercing arrows retain their native shield bypass");h.succeed();});
    }
    @GameTest(template="test_empty")
    public static void weaknessAndGoldenAppleStillStartVillagerCuring(GameTestHelper h){
        var p=player(h);var zombie=h.spawn(EntityType.ZOMBIE_VILLAGER,4,2,4);zombie.setNoAi(true);zombie.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,1200));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GOLDEN_APPLE,2));
        h.assertTrue(zombie.interact(p,InteractionHand.MAIN_HAND).consumesAction()&&zombie.isConverting()&&p.getMainHandItem().getCount()==1,"Zombie-family modifications preserve ordinary villager curing");h.succeed();
    }
}
