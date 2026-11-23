package net.itsrileynj.fantasyfeudalismmobs.gametest.rootworm;

import net.itsrileynj.fantasyfeudalismmobs.rootworm.RootWormModule;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWormPart;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWormSearchPose;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(RootWormModule.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RootWormSearchGameTests {
    @GameTest(template = "test_root_worm", timeoutTicks = 100, skyAccess = true)
    public static void rootWormHeardSoundRaisesTheFrontScansAndKeepsTheTailGroundedAndHeadHittable(GameTestHelper helper) {
        var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 16, 2, 16);
        worm.setOnGround(true);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(worm.position().add(0, 0, 0.5));
        helper.getLevel().gameEvent(GameEvent.EAT, player.position(), GameEvent.Context.of(player));
        Vec3[] raisedHead = {null};
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(worm.isSearching() && worm.getSearchAmount(0) > 0.99F, "Hearing should trigger a smoothly raised listening pose");
            helper.assertTrue(worm.getParts()[0].getY() - worm.getY() > 0.5, "The head damage box must rise with the visible head");
            helper.assertTrue(worm.getBoundingBoxForCulling().contains(worm.getParts()[0].getBoundingBox().getCenter()),
                    "Rendering bounds must include the raised head");
            for (int i = 4; i < 9; i++) helper.assertTrue(Math.abs(worm.getParts()[i].getY() - worm.getY()) < 0.025,
                    "The rear five segments must remain on the ground");
            for (int i = 1; i < 9; i++) helper.assertTrue(worm.getParts()[i].getBoundingBox().intersects(worm.getParts()[i - 1].getBoundingBox()),
                    "Raised damage boxes must remain connected");
            helper.assertTrue(worm.getTarget() == null && !worm.isBiting() && worm.getMouthOpenAmount(0) == 0,
                    "Searching remains neutral and keeps the mouth closed");
            helper.assertTrue(worm.getNavigation().isDone(), "The worm should pause to listen while raised");
            var lateTrackingCopy = RootWormModule.ROOT_WORM.get().create(helper.getLevel());
            lateTrackingCopy.getEntityData().assignValues(worm.getEntityData().getNonDefaultValues());
            lateTrackingCopy.tickCount = 10000;
            helper.assertTrue(lateTrackingCopy.getSearchPose(0, 0.5F).equals(worm.getSearchPose(0, 0.5F)),
                    "A newly tracking client must recover the current scan pose without depending on its entity tick counter");
            raisedHead[0] = worm.getParts()[0].position();
        });
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(worm.getParts()[0].position().distanceToSqr(raisedHead[0]) > 0.01,
                    "The raised head should scan from side to side");
            var center = worm.getParts()[0].getBoundingBox().getCenter();
            Vec3 from = center.add(2, 0, 0), to = center.add(-2, 0, 0);
            var hit = ProjectileUtil.getEntityHitResult(player, from, to, new AABB(from, to).inflate(0.1),
                    entity -> entity instanceof RootWormPart part && part.getParent() == worm && part.isPickable(), from.distanceToSqr(to));
            helper.assertTrue(hit != null && hit.getLocation().y > worm.getBoundingBox().maxY,
                    "The raised head must be pickable above the original movement box");
            float health = worm.getHealth();
            player.attack(hit.getEntity());
            helper.assertTrue(worm.getHealth() < health, "A normal attack on the raised body must damage the worm");
            helper.succeed();
        });
    }

    @GameTest(template = "test_root_worm", timeoutTicks = 130, skyAccess = true)
    public static void rootWormForgottenSoundsLowerTheBodyAgain(GameTestHelper helper) {
        var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 16, 2, 16);
        worm.setOnGround(true);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(worm.position().add(0, 0, 0.5));
        helper.getLevel().gameEvent(GameEvent.EAT, player.position(), GameEvent.Context.of(player));
        helper.runAfterDelay(15, () -> helper.assertTrue(worm.getSearchAmount(0) > 0.99F, "The sound must first produce a raised pose"));
        helper.runAfterDelay(96, () -> {
            helper.assertTrue(!worm.isSearching() && worm.getSearchAmount(0) == 0, "Forgetting the sound must finish lowering the body");
            helper.assertTrue(Math.abs(worm.getParts()[0].getY() - worm.getY() + 0.1) < 0.001,
                    "The head box must return to its normal height");
            helper.succeed();
        });
    }

    @GameTest(template = "test_root_worm", timeoutTicks = 140, skyAccess = true)
    public static void rootWormRetaliationInterruptsSearchingAndStillBitesASlowlyRetreatingPlayer(GameTestHelper helper) {
        var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 16, 2, 16);
        worm.setOnGround(true);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 start = worm.position().add(0, 0, 0.5);
        player.setPos(start);
        helper.getLevel().gameEvent(GameEvent.EAT, start, GameEvent.Context.of(player));
        float health = player.getHealth();
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(worm.isSearching(), "The worm must be listening before being attacked");
            worm.hurt(helper.getLevel().damageSources().playerAttack(player), 1);
        });
        int[] ticks = {0};
        helper.onEachTick(() -> {
            int tick = ++ticks[0];
            if (tick > 15) {
                player.setPos(start.add(0, 0, (tick - 15) * 0.04));
                if (tick % 4 == 0) helper.getLevel().gameEvent(GameEvent.STEP, player.position(), GameEvent.Context.of(player));
            }
        });
        helper.runAfterDelay(32, () -> helper.assertTrue(!worm.isSearching() && worm.getSearchAmount(0) == 0,
                "Combat must interrupt the listening pose and lower the body"));
        helper.runAfterDelay(115, () -> {
            helper.assertTrue(player.getHealth() < health, "The search animation must not prevent a retaliatory bite");
            helper.succeed();
        });
    }

    @GameTest(template = "test_root_worm", timeoutTicks = 100, skyAccess = true)
    public static void rootWormLowCeilingsAndBurningPreventRaising(GameTestHelper helper) {
        var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 16, 2, 16);
        worm.setOnGround(true);
        for (int x = 13; x <= 19; x++) for (int z = 13; z <= 19; z++) helper.setBlock(x, 3, z, Blocks.STONE);
        worm.setSearching(true);
        helper.assertTrue(!worm.isSearching(), "The worm must not lift through a low ceiling");
        for (int x = 13; x <= 19; x++) for (int z = 13; z <= 19; z++) helper.setBlock(x, 3, z, Blocks.AIR);
        worm.setSearching(true);
        helper.assertTrue(worm.isSearching(), "An unobstructed grounded worm should be able to raise its body");
        worm.setRemainingFireTicks(80);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!worm.isSearching(), "Burning must interrupt searching so the worm can flee");
            helper.succeed();
        });
    }

    @GameTest(template = "test_root_worm", timeoutTicks = 100, skyAccess = true)
    public static void rootWormSharedSearchCurvePreservesSegmentSpacingAndAnchorsAcrossTheScan(GameTestHelper helper) {
        for (float amount : new float[]{0, 0.25F, 0.5F, 1}) for (int age = 0; age < 80; age += 4) {
            for (int i = 0; i < 8; i++) {
                var pose = RootWormSearchPose.sample(i, amount, age);
                var next = RootWormSearchPose.sample(i + 1, amount, age);
                double spacing = new Vec3(pose.x(), pose.lift(), pose.z()).distanceTo(new Vec3(next.x(), next.lift(), next.z()));
                double resting = net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWormShape.SEGMENTS.get(i + 1).z()
                        - net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWormShape.SEGMENTS.get(i).z();
                helper.assertTrue(Math.abs(spacing - resting) < 0.001, "Raising and scanning must not stretch or separate the body segments");
                if (i >= 4) helper.assertTrue(pose.lift() == 0 && pose.x() == 0 && pose.pitch() == 0 && pose.yaw() == 0,
                        "The tail must stay anchored throughout the animation");
            }
        }
        helper.succeed();
    }
}
