package net.itsrileynj.fantasyfeudalismmobs.gametest.rootworm;

import net.itsrileynj.fantasyfeudalismmobs.rootworm.RootWormModule;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.RootWormConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(RootWormModule.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RootWormHearingGameTests {
    @GameTest(template = "test_root_worm", timeoutTicks = 100, skyAccess = true)
    public static void rootWormVanillaActionEventsAreHeardWithoutMakingANeutralWormHostile(GameTestHelper helper) {
        var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 16, 2, 16);
        worm.setNoAi(true);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setInvisible(true); // Seeing the actor is not required.
        int step = 0;
        for (var event : java.util.List.of(GameEvent.EAT, GameEvent.HIT_GROUND, GameEvent.ENTITY_DAMAGE, GameEvent.BLOCK_DESTROY, GameEvent.BLOCK_PLACE, GameEvent.EQUIP, GameEvent.UNEQUIP)) {
            Vec3 position = worm.position().add(3 + step++ * 0.1, 0, 0);
            player.setPos(position);
            helper.getLevel().gameEvent(event, position, GameEvent.Context.of(player));
            helper.assertTrue(worm.hasRecentSoundFrom(player) && worm.lastHeardPosition().distanceToSqr(position) < 1e-8,
                    "Registered hearing must receive " + event.key().location());
            helper.assertTrue(worm.getTarget() == null, "Hearing alone must not cause unprovoked attacks");
        }
        helper.succeed();
    }

    @GameTest(template = "test_root_worm", timeoutTicks = 100, skyAccess = true)
    public static void rootWormRunningIsLouderSneakingIsQuietAndJumpTakeoffIsHeard(GameTestHelper helper) {
        var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 16, 2, 16);
        worm.setNoAi(true);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 far = worm.position().add(12, 0, 0);
        player.setPos(far);
        player.setSprinting(false);
        helper.getLevel().gameEvent(GameEvent.STEP, far, GameEvent.Context.of(player));
        helper.assertTrue(!worm.hasRecentSound(), "Ordinary footsteps should have a shorter range");
        player.setSprinting(true);
        helper.getLevel().gameEvent(GameEvent.STEP, far, GameEvent.Context.of(player));
        helper.assertTrue(worm.hasRecentSoundFrom(player), "Running should be heard at the normal loud-sound radius");
        Vec3 quiet = worm.position().add(3, 0, 0);
        player.setPos(quiet);
        player.setSprinting(false);
        player.setShiftKeyDown(true);
        player.setPose(Pose.CROUCHING);
        helper.getLevel().gameEvent(GameEvent.STEP, quiet, GameEvent.Context.of(player));
        helper.assertTrue(worm.lastHeardPosition().distanceToSqr(far) < 1e-8, "Sneaking footsteps must not refresh the sound location");
        player.setShiftKeyDown(false);
        player.setPose(Pose.STANDING);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent(player));
        helper.assertTrue(worm.lastHeardPosition().distanceToSqr(quiet) < 1e-8, "Jump takeoff should be heard through the registered NeoForge event");
        helper.succeed();
    }

    @GameTest(template = "test_root_worm", timeoutTicks = 140, skyAccess = true)
    public static void rootWormQuietMovementDoesNotUpdateTheKnownPositionAndMemoryExpires(GameTestHelper helper) {
        var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 16, 2, 16);
        worm.setNoAi(true);
        worm.setNoGravity(true);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 heard = worm.position().add(3, 0, 0);
        player.setPos(heard);
        helper.getLevel().gameEvent(GameEvent.EAT, heard, GameEvent.Context.of(player));
        player.setPos(heard.add(5, 0, 0));
        helper.assertTrue(worm.lastHeardPosition().distanceToSqr(heard) < 1e-8, "A blind creature must not silently follow the actor's live coordinates");
        helper.runAfterDelay(RootWormConfig.SOUND_MEMORY_TICKS.get() + 2, () -> {
            helper.assertTrue(!worm.hasRecentSound(), "Old sounds must expire");
            helper.succeed();
        });
    }

    @GameTest(template = "test_root_worm", timeoutTicks = 140, skyAccess = true)
    public static void rootWormRetaliatoryBiteConnectsWhenAPlayerSlowlyRetreats(GameTestHelper helper) {
        var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 16, 2, 16);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 start = worm.position().add(0, 0, 1.9);
        player.setPos(start);
        worm.setOnGround(true);
        // HurtByTargetGoal compares timestamps; damaging on spawn tick zero is not a real retaliation setup.
        helper.runAfterDelay(2, () -> worm.hurt(helper.getLevel().damageSources().playerAttack(player), 1));
        float health = player.getHealth();
        int[] ticks = {0};
        helper.onEachTick(() -> {
            int tick = ++ticks[0];
            player.setPos(start.add(0, 0, tick * 0.04));
            if (tick % 4 == 0) helper.getLevel().gameEvent(GameEvent.STEP, player.position(), GameEvent.Context.of(player));
        });
        helper.runAfterDelay(110, () -> {
            helper.assertTrue(player.getHealth() < health, "A slowly retreating player must be bitten instead of causing endless repositioning");
            helper.succeed();
        });
    }

    @GameTest(template = "test_root_worm", timeoutTicks = 100, skyAccess = true)
    public static void rootWormHearingWorksThroughAWallButBitesCannotDamageThroughIt(GameTestHelper helper) {
        var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 16, 2, 16);
        worm.setNoAi(true);
        worm.setYBodyRot(0);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(16, 2, 18))));
        helper.setBlock(16, 2, 17, Blocks.STONE);
        helper.getLevel().gameEvent(GameEvent.EAT, player.position(), GameEvent.Context.of(player));
        helper.assertTrue(worm.hasRecentSoundFrom(player), "Nearby sounds do not require vision");
        helper.assertTrue(!worm.canBite(player), "A wall between the head and target must stop the bite");
        helper.succeed();
    }

    @GameTest(template = "test_root_worm", timeoutTicks = 100, skyAccess = true)
    public static void rootWormShortenedBiteAnimationFinishesWithinElevenTicks(GameTestHelper helper) {
        var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 16, 2, 16);
        worm.setNoAi(true);
        worm.beginBite();
        helper.assertTrue(worm.isBiting(), "Bite animation must begin");
        helper.runAfterDelay(11, () -> {
            helper.assertTrue(!worm.isBiting() && worm.getMouthOpenAmount(0) == 0, "The new nine-tick bite must have recovered before the old twelve-tick duration");
            helper.succeed();
        });
    }
    @GameTest(template = "test_root_worm", timeoutTicks = 100, skyAccess = true)
    public static void rootWormHeldItemChangesAreHeardButDurabilityChangesAreQuiet(GameTestHelper helper) {
        var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 16, 2, 16);
        worm.setNoAi(true);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 equippedAt = worm.position().add(4, 0, 0);
        player.setPos(equippedAt);
        var sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent(
                player, net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY, sword));
        helper.assertTrue(worm.hasRecentSoundFrom(player), "Equipping a held item must be audible");
        player.setPos(equippedAt.add(1, 0, 0));
        var wornSword = sword.copy();
        wornSword.setDamageValue(1);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent(
                player, net.minecraft.world.entity.EquipmentSlot.MAINHAND, sword, wornSword));
        helper.assertTrue(worm.lastHeardPosition().distanceToSqr(equippedAt) < 1e-8, "Item wear must not be mistaken for equipping");
        helper.assertTrue(worm.getTarget() == null, "Equipping alone must keep the worm neutral");
        helper.succeed();
    }

}
