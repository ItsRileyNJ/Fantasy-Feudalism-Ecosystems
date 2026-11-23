package net.itsrileynj.fantasyfeudalismmobs.gametest.rootworm;

import net.itsrileynj.fantasyfeudalismmobs.rootworm.RootWormModule;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWorm;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWormPart;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(RootWormModule.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RootWormHitboxGameTests {
    @GameTest(template = "test_root_worm", timeoutTicks = 100, skyAccess = true)
    public static void rootWormHeadAndTailOutsideMovementBoxCanBePickedAndAttacked(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6);
        for (int endpoint = 0; endpoint < 2; endpoint++) {
            var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 10 + endpoint * 12, 2, 16);
            worm.setNoAi(true);
            worm.setNoGravity(true);
            worm.setYBodyRot(endpoint == 0 ? 0 : 90);
            var part = worm.getParts()[endpoint == 0 ? 0 : 8];
            Vec3 forward = Vec3.directionFromRotation(0, worm.yBodyRot);
            Vec3 side = new Vec3(forward.z, 0, -forward.x);
            Vec3 center = part.getBoundingBox().getCenter();
            Vec3 from = center.add(side.scale(2)), to = center.subtract(side.scale(2));
            player.setPos(from.subtract(0, player.getEyeHeight(), 0));
            var hit = pick(player, worm, from, to);
            helper.assertTrue(hit != null && hit.getEntity() instanceof RootWormPart, "Mouse/projectile picking must find the visible " + (endpoint == 0 ? "head" : "tail"));
            helper.assertTrue(!worm.getBoundingBox().contains(hit.getLocation()), "Regression must hit outside the old movement box");
            Entity packetTarget = ServerboundInteractPacket.createAttackPacket(hit.getEntity(), false).getTarget(helper.getLevel());
            helper.assertTrue(packetTarget == hit.getEntity(), "Server attack packets must resolve multipart IDs");
            float health = worm.getHealth();
            player.attack(packetTarget);
            helper.assertTrue(worm.getHealth() < health, "A normal player attack at either end must damage the worm");
            helper.assertTrue(worm.getLastHurtByMob() == player, "Part hits must retain the attacker for retaliation");
        }
        helper.succeed();
    }

    @GameTest(template = "test_root_worm", timeoutTicks = 100, skyAccess = true)
    public static void rootWormSegmentBoxesFollowTurnsTeleportsAndScaleWithoutWideningMovement(GameTestHelper helper) {
        var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 16, 2, 16);
        worm.setNoAi(true);
        for (float yaw : new float[]{0, 90, 180, 270}) {
            worm.setYBodyRot(yaw);
            Vec3 forward = Vec3.directionFromRotation(0, yaw);
            helper.assertTrue(worm.getParts()[0].position().subtract(worm.position()).dot(forward) > 1,
                    "The head box must rotate with the visible front");
            helper.assertTrue(worm.getParts()[8].position().subtract(worm.position()).dot(forward) < -0.9,
                    "The tail box must rotate with the visible rear");
            for (int i = 0; i < worm.getParts().length; i++) {
                var part = worm.getParts()[i];
                helper.assertTrue(part.getId() == worm.getId() + i + 1 && helper.getLevel().getEntityOrPart(part.getId()) == part,
                        "Parts must use registered, consecutive IDs");
                if (i > 0) helper.assertTrue(part.getBoundingBox().intersects(worm.getParts()[i - 1].getBoundingBox()), "The body must not have gaps between damage boxes");
            }
        }
        worm.setYBodyRot(0);
        Vec3 headBefore = worm.getParts()[0].position();
        Vec3 delta = new Vec3(5, 1, -4);
        worm.setPos(worm.position().add(delta));
        helper.assertTrue(worm.getParts()[0].position().subtract(headBefore).distanceToSqr(delta) < 1e-8, "Teleporting must move every hitbox immediately");
        helper.assertTrue(Math.abs(worm.getBbWidth() - 0.7) < 0.001 && Math.abs(worm.getBbHeight() - 0.45) < 0.001,
                "Walking/navigation must keep its small physical footprint");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 outside = worm.position().add(0, 0.2, 1.7);
        helper.assertTrue(pick(player, worm, outside.add(2, 0, 0), outside.add(-2, 0, 0)) == null,
                "Hitboxes must not expand to a giant square around the worm");
        float oldWidth = worm.getParts()[0].getBbWidth();
        worm.getAttribute(Attributes.SCALE).setBaseValue(2);
        worm.refreshDimensions();
        helper.assertTrue(Math.abs(worm.getParts()[0].getBbWidth() - oldWidth * 2) < 0.001,
                "Hitboxes must follow entity scale just as the model does");
        helper.succeed();
    }

    @GameTest(template = "test_root_worm", timeoutTicks = 100, skyAccess = true)
    public static void rootWormSegmentsShareDamageCooldownAndAreRemovedWithTheirParent(GameTestHelper helper) {
        var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 16, 2, 16);
        var source = helper.getLevel().damageSources().generic();
        float health = worm.getHealth();
        helper.assertTrue(worm.getParts()[0].hurt(source, 2), "A segment should accept damage");
        worm.getParts()[8].hurt(source, 2);
        helper.assertTrue(worm.getHealth() == health - 2, "Overlapping parts must share one health pool and damage cooldown");
        worm.discard();
        for (var part : worm.getParts()) {
            helper.assertTrue(!part.isPickable() && !part.hurt(source, 2), "Removed worms must leave no targetable ghost parts");
            helper.assertTrue(helper.getLevel().getEntityOrPart(part.getId()) == null, "Part IDs must be unregistered on removal");
        }
        RootWorm detached = RootWormModule.ROOT_WORM.get().create(helper.getLevel());
        detached.setId(Integer.MAX_VALUE - 32);
        for (int i = 0; i < detached.getParts().length; i++) {
            helper.assertTrue(detached.getParts()[i].getId() == detached.getId() + i + 1,
                    "A spawn-packet ID assignment must update all part IDs on the receiving side");
        }
        helper.succeed();
    }

    @GameTest(template = "test_root_worm", timeoutTicks = 100, skyAccess = true)
    public static void rootWormSegmentFireAndNameTagInteractionsReachTheParent(GameTestHelper helper) {
        var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 16, 2, 16);
        worm.setNoAi(true);
        var head = worm.getParts()[0];
        head.setRemainingFireTicks(80);
        helper.assertTrue(worm.getRemainingFireTicks() == 80 && head.getRemainingFireTicks() == 80,
                "Flaming projectile fire must apply to the worm rather than an unticked part");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var tag = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NAME_TAG);
        tag.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Rootie"));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tag);
        var result = head.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(result.consumesAction() && worm.hasCustomName() && worm.getCustomName().getString().equals("Rootie"),
                "Clicking a segment with a name tag must name the actual worm");
        helper.succeed();
    }

    @GameTest(template = "test_root_worm", timeoutTicks = 100, skyAccess = true)
    public static void rootWormFlyingTippedArrowHitsTheHeadAndKeepsItsFireAndPotionEffects(GameTestHelper helper) {
        var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 16, 2, 16);
        worm.setNoAi(true);
        worm.setNoGravity(true);
        worm.setYBodyRot(0);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var arrow = new net.minecraft.world.entity.projectile.Arrow(helper.getLevel(), player,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TIPPED_ARROW), null);
        arrow.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 200));
        arrow.setNoGravity(true);
        arrow.setRemainingFireTicks(200);
        Vec3 from = worm.getParts()[0].getBoundingBox().getCenter().add(2, 0, 0);
        arrow.setPos(from);
        arrow.setDeltaMovement(-1, 0, 0);
        helper.getLevel().addFreshEntity(arrow);
        float health = worm.getHealth();
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(worm.getHealth() < health, "A flying arrow must hit the head outside the old movement box");
            helper.assertTrue(worm.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN), "Tipped arrow effects must reach the actual living worm");
            helper.assertTrue(worm.isOnFire(), "Flaming arrows must still ignite the worm");
            helper.succeed();
        });
    }

    private static EntityHitResult pick(Player player, RootWorm worm, Vec3 from, Vec3 to) {
        return ProjectileUtil.getEntityHitResult(player, from, to, new AABB(from, to).inflate(0.25),
                entity -> entity.isPickable() && (entity == worm || entity instanceof RootWormPart part && part.getParent() == worm),
                from.distanceToSqr(to));
    }
}
