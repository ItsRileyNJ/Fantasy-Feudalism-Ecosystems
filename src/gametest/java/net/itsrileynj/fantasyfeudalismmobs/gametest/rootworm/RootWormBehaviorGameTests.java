package net.itsrileynj.fantasyfeudalismmobs.gametest.rootworm;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.RootWormModule;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.RootWormConfig;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWorm;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.AvoidHeatGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
@GameTestHolder(RootWormModule.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RootWormBehaviorGameTests {
    @GameTest(template = "test_root_worm", timeoutTicks = 120, skyAccess = true)
    public static void rootWormWormRemainsNeutralThenRetaliates(GameTestHelper helper) {
        RootWorm worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 14, 2, 16);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(new BlockPos(16, 2, 16)).getCenter());
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(worm.getTarget() == null, "Unprovoked worm must not target a nearby player");
            worm.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        });
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(worm.getTarget() == player, "Attacked worm should defend itself against the attacker");
            helper.succeed();
        });
    }

    @GameTest(template = "test_root_worm", timeoutTicks = 100, skyAccess = true)
    public static void rootWormWormOpensMouthBeforeBitingThenClosesIt(GameTestHelper helper) {
        var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 15, 2, 16);
        var target = helper.spawn(EntityType.PIG, 22, 2, 16);
        target.setNoAi(true);
        helper.assertTrue(!worm.isBiting() && worm.getMouthOpenAmount(0) == 0, "Idle mouth must be closed");
        float hp = target.getHealth();
        boolean[] observed = new boolean[2];
        worm.setTarget(target);
        worm.hearSound(target, target.position(), 1.0); // Simulate a retaliation target, without introducing player aggression.
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!worm.isBiting() && worm.getMouthOpenAmount(0) == 0, "Pursuit alone must not open the mouth");
            target.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(16, 2, 16))));
        });
        helper.onEachTick(() -> {
            if (worm.isBiting() && target.getHealth() == hp && worm.getMouthOpenAmount(0) > 0) observed[0] = true;
            if (target.getHealth() < hp && !observed[1]) {
                helper.assertTrue(observed[0], "Mouth must open during the windup before bite damage");
                helper.assertTrue(worm.getMouthOpenAmount(0) > 0.5F, "Bite must land with the mouth open");
                observed[1] = true;
                worm.setTarget(null);
                worm.setNoAi(true);
            }
        });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(observed[0] && observed[1], "The actual melee goal must open the mouth and deliver its bite");
            helper.assertTrue(!worm.isBiting() && worm.getMouthOpenAmount(0) == 0, "Mouth must close after the attack");
            helper.succeed();
        });
    }

    @GameTest(template = "test_root_worm", timeoutTicks = 100, skyAccess = true)
    public static void rootWormWormFireAndLavaDamageAreAmplifiedAndConfigurable(GameTestHelper helper) {
        boolean enabled = RootWormConfig.WORM_FIRE_WEAKNESS.get();
        double fire = RootWormConfig.WORM_FIRE_MULTIPLIER.get();
        double lava = RootWormConfig.WORM_LAVA_MULTIPLIER.get();
        try {
            RootWormConfig.WORM_FIRE_WEAKNESS.set(true);
            RootWormConfig.WORM_FIRE_MULTIPLIER.set(2.0);
            RootWormConfig.WORM_LAVA_MULTIPLIER.set(3.0);
            var burnt = helper.spawn(RootWormModule.ROOT_WORM.get(), 8, 2, 8);
            var submerged = helper.spawn(RootWormModule.ROOT_WORM.get(), 12, 2, 8);
            float hp = burnt.getHealth();
            burnt.hurt(helper.getLevel().damageSources().inFire(), 2.0F);
            submerged.hurt(helper.getLevel().damageSources().lava(), 2.0F);
            helper.assertTrue(burnt.getHealth() == hp - 4, "Fire should use fire multiplier");
            helper.assertTrue(submerged.getHealth() == hp - 6, "Lava should use its own multiplier without stacking");
            RootWormConfig.WORM_FIRE_WEAKNESS.set(false);
            var normal = helper.spawn(RootWormModule.ROOT_WORM.get(), 16, 2, 8);
            normal.hurt(helper.getLevel().damageSources().inFire(), 2.0F);
            helper.assertTrue(normal.getHealth() == hp - 2, "Disabled weakness should restore ordinary fire damage");
        } finally {
            RootWormConfig.WORM_FIRE_WEAKNESS.set(enabled);
            RootWormConfig.WORM_FIRE_MULTIPLIER.set(fire);
            RootWormConfig.WORM_LAVA_MULTIPLIER.set(lava);
        }
        helper.succeed();
    }

    @GameTest(template = "test_root_worm", timeoutTicks = 100, skyAccess = true)
    public static void rootWormWormPlansEscapeFromHeatAndHonorsToggle(GameTestHelper helper) {
        boolean previous = RootWormConfig.WORM_FEAR_HEAT.get();
        try {
            RootWormConfig.WORM_FEAR_HEAT.set(true);
            var worm = helper.spawn(RootWormModule.ROOT_WORM.get(), 16, 2, 16);
            // Navigation is queried immediately, before the first physics tick establishes ground contact.
            worm.setOnGround(true);
            BlockPos hot = helper.absolutePos(new BlockPos(19, 2, 16));
            helper.setBlock(19, 2, 16, Blocks.FIRE);
            helper.assertTrue(RootWorm.isHeat(helper.getLevel(), hot), "Vanilla fire should be feared");
            helper.assertTrue(!RootWorm.isHeat(helper.getLevel(), helper.absolutePos(new BlockPos(20, 2, 16))), "Air is not a heat source");
            var flee = new AvoidHeatGoal(worm);
            helper.assertTrue(flee.canUse(), "Nearby heat should produce a reachable escape path");
            flee.start();
            var destination = worm.getNavigation().getPath().getEndNode().asBlockPos();
            helper.assertTrue(destination.distSqr(hot) > worm.blockPosition().distSqr(hot), "Escape destination must be farther from heat");
            RootWormConfig.WORM_FEAR_HEAT.set(false);
            helper.assertTrue(!new AvoidHeatGoal(worm).canUse(), "Disabled avoidance should not start fleeing");
            helper.setBlock(19, 2, 16, Blocks.LAVA);
            helper.assertTrue(RootWorm.isHeat(helper.getLevel(), hot), "Lava should be recognized as heat");
            helper.setBlock(19, 2, 16, Blocks.TORCH);
            helper.assertTrue(!RootWorm.isHeat(helper.getLevel(), hot), "Ordinary torches impose no custom block rules");
        } finally { RootWormConfig.WORM_FEAR_HEAT.set(previous); }
        helper.succeed();
    }
    @GameTest(template = "test_root_worm", timeoutTicks = 100, skyAccess = true)
    public static void rootWormSpawnRulesUseOrdinaryGroundInsteadOfBiomeOrSoilTags(GameTestHelper helper) {
        for (var block : new net.minecraft.world.level.block.Block[]{Blocks.STONE, Blocks.SAND, Blocks.DEEPSLATE, Blocks.GRASS_BLOCK}) {
            helper.setBlock(16, 1, 16, block);
            var pos = helper.absolutePos(new BlockPos(16, 2, 16));
            helper.assertTrue(net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(RootWormModule.ROOT_WORM.get(), helper.getLevel(),
                    net.minecraft.world.entity.MobSpawnType.NATURAL, pos, net.minecraft.util.RandomSource.create(1)),
                    "No custom biome/soil tag should be required to spawn on " + block);
        }
        helper.succeed();
    }
}
