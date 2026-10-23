package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.UUID;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeepAuditTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        // GameTestHelper's mock permanently overrides isCreative/isSpectator, even after setGameMode.
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"deep-audit-player"),false);
        var player=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,player,cookie);
        player.setGameMode(GameType.SURVIVAL);player.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(4,2,8)));
        return player;
    }
    @GameTest(template="test_empty", batch="ffm_audit_player_modes")
    public static void wildAndTamedSpidersRejectCreativeAndSpectatorTargets(GameTestHelper h) {
        var player=player(h);
        var spider=h.spawn(EntityType.SPIDER,8,2,8);spider.setNoAi(true);
        try {
            for(boolean tamed:new boolean[]{false,true}) {
                if(tamed){SpiderCompanion.tame(spider,UUID.randomUUID());SpiderCompanion.setSitting(spider,false);}
                for(var mode:new GameType[]{GameType.CREATIVE,GameType.SPECTATOR}) {
                    player.setGameMode(mode);
                    h.assertTrue(mode==GameType.CREATIVE?player.isCreative():player.isSpectator(),"Fixture uses the actual requested player mode");
                    h.assertFalse(SpiderGoals.mayAttack(spider,player),"Spiders must reject "+mode+" even on retaliation/explicit targeting paths");
                    spider.setTarget(player);
                    h.assertTrue(spider.getTarget()==null,"Target event must reject protected player modes");
                }
            }
            h.succeed();
        } finally {h.getLevel().getServer().getPlayerList().remove(player);}
    }

    @GameTest(template="test_empty", batch="ffm_audit_offline_pvp")
    public static void offlineSpiderOwnerCannotBypassServerPvpProtection(GameTestHelper h) {
        var server=h.getLevel().getServer();boolean original=server.isPvpAllowed();
        var player=player(h);
        var spider=h.spawn(EntityType.CAVE_SPIDER,8,2,8);spider.setNoAi(true);
        SpiderCompanion.tame(spider,UUID.randomUUID());SpiderCompanion.setSitting(spider,false);
        try {
            h.assertTrue(SpiderCompanion.owner(spider)==null,"Owner is genuinely offline");
            server.setPvpAllowed(false);
            h.assertFalse(SpiderGoals.mayAttack(spider,player),"Server PvP protection must also apply to offline owners' pets");
            spider.setTarget(player);h.assertTrue(spider.getTarget()==null,"Protected target cannot be retained");
            server.setPvpAllowed(true);
            h.assertTrue(SpiderGoals.mayAttack(spider,player),"Normal survival retaliation remains available when PvP is enabled");
            h.succeed();
        } finally {server.setPvpAllowed(original);server.getPlayerList().remove(player);}
    }

    @GameTest(template="test_empty", batch="ffm_audit_smelting", timeoutTicks=280)
    public static void realFurnacesAndHoppersCookVanillaAndHorseMeat(GameTestHelper h) {
        for(int x:new int[]{6,10}) {
            h.setBlock(x,2,8,Blocks.CHEST);h.setBlock(x,3,8,Blocks.HOPPER);h.setBlock(x,4,8,Blocks.FURNACE);
            var furnace=(FurnaceBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(x,4,8)));
            furnace.setItem(0,new ItemStack(x==6?Items.BEEF:FantasyFeudalismMobs.RAW_HORSE.get()));
            furnace.setItem(1,new ItemStack(Items.COAL));
        }
        h.succeedWhen(()->{
            for(int x:new int[]{6,10}) {
                var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(x,2,8)));
                h.assertTrue(chest.countItem(x==6?Items.COOKED_BEEF:FantasyFeudalismMobs.COOKED_HORSE.get())==1,
                        "Actual furnace ticks and hopper extraction must deliver exactly one cooked item");
            }
        });
    }

    @GameTest(template="test_empty", batch="ffm_audit_brewing", timeoutTicks=450)
    public static void brewingStandStillBrewsAndConsumesNativeIngredients(GameTestHelper h) {
        h.setBlock(8,2,8,Blocks.BREWING_STAND);
        var stand=(BrewingStandBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(8,2,8)));
        stand.setItem(0,PotionContents.createItemStack(Items.POTION,Potions.WATER));
        stand.setItem(3,new ItemStack(Items.NETHER_WART,2));stand.setItem(4,new ItemStack(Items.BLAZE_POWDER));
        h.succeedWhen(()->{
            h.assertTrue(stand.getItem(0).getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).is(Potions.AWKWARD),
                    "Native brewing completes with the correct potion contents");
            h.assertTrue(stand.getItem(3).getCount()==1&&stand.getItem(4).isEmpty(),"Brewing consumes one ingredient and one fuel item");
        });
    }

    @GameTest(template="test_empty", batch="ffm_audit_redstone", timeoutTicks=30)
    public static void redstoneLampRetainsNativePowerAndDelayedShutoff(GameTestHelper h) {
        h.setBlock(8,2,8,Blocks.REDSTONE_LAMP);h.setBlock(9,2,8,Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(2,()->{
            h.assertTrue(h.getBlockState(new BlockPos(8,2,8)).getValue(RedstoneLampBlock.LIT),"Native neighbor updates power the lamp");
            h.setBlock(9,2,8,Blocks.AIR);
        });
        h.runAfterDelay(8,()->{
            h.assertFalse(h.getBlockState(new BlockPos(8,2,8)).getValue(RedstoneLampBlock.LIT),"Native scheduled ticks turn the lamp off");h.succeed();
        });
    }

    @GameTest(template="test_empty", batch="ffm_audit_shearing")
    public static void ordinaryShearingStillDropsWoolAndDamagesTheTool(GameTestHelper h) {
        var player=player(h);
        var sheep=h.spawn(EntityType.SHEEP,8,2,8);sheep.setNoAi(true);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));
        try {
            player.interactOn(sheep,InteractionHand.MAIN_HAND);
            h.assertTrue(sheep.isSheared()&&player.getMainHandItem().getDamageValue()==1,"Native shearing and tool durability remain intact");
            int wool=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,h.getBounds(),
                    e->e.getItem().is(net.minecraft.tags.ItemTags.WOOL)).stream().mapToInt(e->e.getItem().getCount()).sum();
            h.assertTrue(wool>=1&&wool<=3,"Shearing drops the native amount of wool");h.succeed();
        } finally {h.getLevel().getServer().getPlayerList().remove(player);}
    }
}
