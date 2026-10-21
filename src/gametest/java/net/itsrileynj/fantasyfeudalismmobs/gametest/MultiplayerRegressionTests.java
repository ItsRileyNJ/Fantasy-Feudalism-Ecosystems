package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.*;
import io.netty.buffer.Unpooled;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MultiplayerRegressionTests {
    private static Player player(GameTestHelper h) {
        Player player=h.makeMockPlayer(GameType.SURVIVAL);player.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(4,2,6)));return player;
    }
    private static Spider pet(GameTestHelper h,Player p) {
        Spider spider=h.spawn(EntityType.SPIDER,4,2,4);spider.setNoAi(true);SpiderCompanion.tame(spider,p.getUUID());
        SpiderCompanion.setSitting(spider,false);return spider;
    }
    private static void event(Player player,Entity target) { NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player,InteractionHand.MAIN_HAND,target)); }

    @GameTest(template="test_empty")
    public static void staleBeeCaptureDoesNotDuplicate(GameTestHelper h) {
        Player first=player(h),second=player(h);Bee bee=h.spawn(EntityType.BEE,4,2,4);bee.setNoAi(true);
        first.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE));
        second.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE,16));
        event(first,bee);for(int i=0;i<100;i++)event(second,bee);
        h.assertTrue(first.getMainHandItem().is(FantasyFeudalismMobs.BEE_BOTTLE),"First capture succeeds");
        h.assertTrue(second.getMainHandItem().is(Items.GLASS_BOTTLE)&&second.getMainHandItem().getCount()==16,
                "Stale capture requests must not create more Bee Bottles");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void twoPlayersShareHarvestCooldown(GameTestHelper h) {
        Player first=player(h),second=player(h);Spider spider=pet(h,first);
        for(Player p:List.of(first,second))p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));
        event(first,spider);for(int i=0;i<100;i++)event(second,spider);
        int silk=first.getInventory().countItem(Items.STRING)+second.getInventory().countItem(Items.STRING);
        h.assertTrue(silk>=1&&silk<=4,"Only one silk harvest across two players");
        h.assertTrue(first.getMainHandItem().getDamageValue()+second.getMainHandItem().getDamageValue()==1,"Only one durability consumed");
        for(Player p:List.of(first,second))p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE,16));
        event(first,spider);for(int i=0;i<100;i++)event(second,spider);
        h.assertTrue(first.getInventory().countItem(Items.POTION)+second.getInventory().countItem(Items.POTION)==1,
                "Only one potion across two players, with independent poison cooldown");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void deadSpidersCannotBeHarvested(GameTestHelper h) {
        Player p=player(h);Spider spider=pet(h,p);spider.setHealth(0);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE,16));
        for(int i=0;i<100;i++)event(p,spider);
        h.assertTrue(p.getInventory().countItem(Items.POTION)==0&&p.getMainHandItem().getCount()==16,"Dead entity rejects interaction before vanilla removal");
        h.assertFalse(SpiderCompanion.harvest(p,InteractionHand.MAIN_HAND,spider,false),"Helper also rejects dead entities");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void spectatorCannotMutateInventory(GameTestHelper h) {
        Player owner=player(h),spectator=h.makeMockPlayer(GameType.SPECTATOR);Spider spider=pet(h,owner);
        spectator.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE,16));event(spectator,spider);
        h.assertTrue(spectator.getMainHandItem().getCount()==16&&spectator.getInventory().countItem(Items.POTION)==0,"Spectators cannot harvest");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void secondPlayerCannotStealOwnership(GameTestHelper h) {
        Player first=player(h),second=player(h);Spider spider=pet(h,first);
        second.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FantasyFeudalismMobs.BEE_BOTTLE.get()));event(second,spider);
        h.assertTrue(SpiderCompanion.ownedBy(spider,first)&&!SpiderCompanion.ownedBy(spider,second),"Feeding a tamed spider must not replace its owner");
        second.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);event(second,spider);
        h.assertFalse(SpiderCompanion.isSitting(spider),"Non-owner cannot change sit order");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void reentryDoesNotAccumulateAi(GameTestHelper h) {
        Silverfish fish=h.spawn(EntityType.SILVERFISH,4,2,4);fish.setNoAi(true);
        int goals=fish.goalSelector.getAvailableGoals().size(),targets=fish.targetSelector.getAvailableGoals().size();
        for(int i=0;i<50;i++)NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(fish,h.getLevel(),false));
        h.assertTrue(fish.goalSelector.getAvailableGoals().size()==goals&&fish.targetSelector.getAvailableGoals().size()==targets,
                "Repeated entity re-entry must not accumulate AI goals");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void projectileReentryDoesNotStackSpeed(GameTestHelper h) {
        Skeleton skeleton=h.spawn(EntityType.SKELETON,4,2,4);skeleton.setNoAi(true);
        Arrow arrow=new Arrow(h.getLevel(),skeleton,new ItemStack(Items.ARROW),new ItemStack(Items.CROSSBOW));
        arrow.setDeltaMovement(3.15,0,0);h.getLevel().addFreshEntity(arrow);
        for(int i=0;i<50;i++)NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(arrow,h.getLevel(),false));
        h.assertTrue(Math.abs(arrow.getDeltaMovement().length()-4.41)<.0001,"Arrow speed must be scaled once across re-entry");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void companionIndexTracksOrdersAndRemoval(GameTestHelper h) {
        Player first=player(h),second=player(h);Spider spider=pet(h,first);
        h.assertTrue(SpiderCompanion.protectedByFollower(first),"Indexed follower protects owner");
        h.assertFalse(SpiderCompanion.protectedByFollower(second),"Index must not protect a different player");
        SpiderCompanion.setSitting(spider,true);h.assertFalse(SpiderCompanion.protectedByFollower(first),"Sitting updates protection immediately");
        SpiderCompanion.setSitting(spider,false);h.assertTrue(SpiderCompanion.protectedByFollower(first),"Following resumes protection");
        spider.discard();h.assertFalse(SpiderCompanion.protectedByFollower(first),"Removed companion leaves index");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void companionStatePacketRoundTrip(GameTestHelper h) {
        Player p=player(h);Spider spider=pet(h,p);SpiderCompanion.setSitting(spider,true);
        var values=spider.getEntityData().getNonDefaultValues();
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,new ClientboundSetEntityDataPacket(spider.getId(),values));
            var decoded=ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);
            h.assertTrue(decoded.id()==spider.getId(),"Entity packet ID");
            h.assertTrue(decoded.packedItems().stream().anyMatch(v->v.value().equals(Optional.of(p.getUUID()))),"Owner UUID survives vanilla entity-data packet serialization");
            h.assertTrue(decoded.packedItems().stream().anyMatch(v->Boolean.TRUE.equals(v.value())),"Sitting flag survives serialization");
        }finally{buffer.release();}
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void fullInventoryPoisonConservesItems(GameTestHelper h) {
        Player p=player(h);Spider spider=pet(h,p);
        for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE,16));
        h.assertTrue(SpiderCompanion.harvest(p,InteractionHand.MAIN_HAND,spider,false),"Full inventory can harvest by dropping output");
        int potions=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,spider.getBoundingBox().inflate(4))
                .stream().filter(e->e.getItem().is(Items.POTION)).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(potions==1&&p.getMainHandItem().getCount()==15,"Exactly one potion is dropped and one bottle consumed");h.succeed();
    }
}
