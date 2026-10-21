package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.List;
import java.util.UUID;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpiderCombatRegressionTests {
    private static Spider spider(GameTestHelper h, int variant) {
        Spider spider = variant < 2 ? h.spawn(EntityType.SPIDER, 4, 2, 4) : h.spawn(EntityType.CAVE_SPIDER, 4, 2, 4);
        spider.setNoAi(true);
        if (variant % 2 != 0) SpiderCompanion.tame(spider, UUID.randomUUID());
        SpiderCompanion.setSitting(spider, false);
        return spider;
    }

    @GameTest(template = "test_empty")
    public static void allSpiderVariantsIgnoreFallsBelowNineBlocks(GameTestHelper h) {
        for (int i = 0; i < 4; i++) {
            Spider spider = spider(h, i);
            float health = spider.getHealth();
            spider.causeFallDamage(8.99F, 1, h.getLevel().damageSources().fall());
            h.assertTrue(spider.getHealth() == health, "Falls below nine blocks are harmless, variant " + i);
            spider.causeFallDamage(9, 1, h.getLevel().damageSources().fall());
            h.assertTrue(spider.getHealth() < health, "Nine-block falls retain vanilla damage, variant " + i);
            spider.discard();
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void survivingFallDoesNotClearSpiderTarget(GameTestHelper h) {
        var target = h.spawn(EntityType.CREEPER, 7, 2, 4); target.setNoAi(true);
        for (int i = 0; i < 4; i++) {
            Spider spider = spider(h, i); spider.setTarget(target);
            spider.causeFallDamage(9, 1, h.getLevel().damageSources().fall());
            h.assertTrue(spider.getTarget() == target, "Surviving a fall retains the target, variant " + i);
            spider.discard();
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void actualDropsIncludeTheFinalLandingStep(GameTestHelper h) {
        for (int x = 0; x < 18; x++) for (int z = 0; z < 18; z++) h.setBlock(x, 1, z, Blocks.STONE);
        var target = h.spawn(EntityType.CREEPER, 17, 2, 17); target.setNoAi(true);
        var actors = new java.util.ArrayList<Spider>();
        var drops = new java.util.ArrayList<Double>();
        for (int row = 0; row < 3; row++) for (int variant = 0; variant < 4; variant++) {
            double drop = row == 0 ? 8.5 : row == 1 ? 9 : 9.5;
            Spider spider = spider(h, variant);
            spider.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(3 + variant * 4, 2 + drop, 3 + row * 5)));
            spider.goalSelector.removeAllGoals(g -> true); spider.targetSelector.removeAllGoals(g -> true);
            spider.setNoAi(false); spider.setTarget(target); actors.add(spider); drops.add(drop);
        }
        h.runAfterDelay(35, () -> {
            for (int i = 0; i < actors.size(); i++) {
                Spider spider = actors.get(i); double drop = drops.get(i);
                h.assertTrue(spider.onGround(), "Physical drop must finish");
                h.assertTrue(drop < 9 ? spider.getHealth() == spider.getMaxHealth()
                                : spider.getHealth() > 0 && spider.getHealth() < spider.getMaxHealth(),
                        "Physical " + drop + "-block drop must use the correct threshold for " + spider.getType());
                h.assertTrue(spider.getTarget() == target, "Landing must preserve the target");
            }
            h.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void attackLeapStartsOnlyAboutThreeBlocksAway(GameTestHelper h) {
        var target = h.spawn(EntityType.CREEPER, 7, 2, 4); target.setNoAi(true);
        for (int i = 0; i < 4; i++) {
            Spider spider = spider(h, i); spider.setTarget(target);
            var leap = spider.goalSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal)
                    .filter(g -> g instanceof LeapAtTargetGoal).findFirst().orElseThrow();
            spider.setOnGround(true);
            for (double distance : new double[]{1, 2, 2.5, 3.5, 4}) {
                target.moveTo(spider.position().add(distance, 0, 0));
                for (int attempt = 0; attempt < 50; attempt++)
                    h.assertFalse(leap.canUse(), "Attack leap must not start from " + distance + " blocks, variant " + i);
            }
            target.moveTo(spider.position().add(3, 0, 0));
            boolean available = false;
            for (int attempt = 0; attempt < 100; attempt++) available |= leap.canUse();
            h.assertTrue(available, "Attack leap must remain available at three blocks, variant " + i);
            spider.discard();
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void spiderEffectsRequireSuccessfulHitsOnEligibleHostiles(GameTestHelper h) {
        for (boolean cave : new boolean[]{false, true}) {
            Spider spider = spider(h, cave ? 3 : 1);
            for (var type : List.of(EntityType.CREEPER, EntityType.PILLAGER, EntityType.SILVERFISH, EntityType.ENDERMITE)) {
                Mob target = h.spawn(type, 7, 2, 4); target.setNoAi(true);
                target.setInvulnerable(true);
                h.assertFalse(spider.doHurtTarget(target), "Invulnerable target rejects the hit");
                h.assertTrue(target.getActiveEffects().isEmpty(), "Rejected hits must not apply effects");
                target.setInvulnerable(false);
                h.assertTrue(spider.doHurtTarget(target), "Hostile hit succeeds");
                for (var effect : List.of(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS)) {
                    var actual = target.getEffect(effect);
                    h.assertTrue(actual != null && actual.getAmplifier() == 0 && actual.getDuration() == 120,
                            "Eligible hostile receives six seconds of level I " + effect);
                }
                var poison = target.getEffect(MobEffects.POISON);
                h.assertTrue(cave ? poison != null && poison.getAmplifier() == 0 && poison.getDuration() == 120 : poison == null,
                        "Only the cave spider adds six seconds of Poison I");
                target.discard();
            }
            spider.discard();
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 120)
    public static void followingSpidersReachAndDebuffTheirActualCombatTarget(GameTestHelper h) {
        for (int x = 0; x < 18; x++) for (int z = 0; z < 18; z++) h.setBlock(x, 1, z, Blocks.GRASS_BLOCK);
        Spider normal = spider(h, 1), cave = spider(h, 3);
        normal.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(4, 2, 4)));
        cave.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(12, 2, 4)));
        var first = h.spawn(EntityType.VINDICATOR, 4, 2, 7); first.setNoAi(true);
        var second = h.spawn(EntityType.VINDICATOR, 12, 2, 7); second.setNoAi(true);
        normal.setTarget(first); cave.setTarget(second); normal.setNoAi(false); cave.setNoAi(false);
        h.succeedWhen(() -> {
            h.assertTrue(first.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && first.hasEffect(MobEffects.WEAKNESS), "Regular companion must reach and debuff its enemy");
            h.assertTrue(second.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && second.hasEffect(MobEffects.WEAKNESS)
                    && second.hasEffect(MobEffects.POISON), "Cave companion must reach and apply all three effects");
        });
    }
}
