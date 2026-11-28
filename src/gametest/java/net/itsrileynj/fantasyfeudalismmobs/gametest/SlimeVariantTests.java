package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.ArrayList;
import java.util.Collections;
import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.itsrileynj.fantasyfeudalismmobs.SlimeVariants;
import net.itsrileynj.fantasyfeudalismmobs.VariantSlime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.TippedArrowRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SlimeVariantTests {
    @GameTest(template = "test_empty")
    public static void flameSlimesSplitOnceAndVariantsDropTheirOwnLoot(GameTestHelper h) {
        var flame = h.spawn(SlimeVariants.FLAME.get(), 8, 2, 8);
        flame.setNoAi(true);
        h.assertTrue(flame.getSize() == 2, "Flame slimes start at half the largest vanilla slime size");
        flame.kill();
        h.assertTrue(dropCount(flame, Items.GUNPOWDER) >= 1 && dropCount(flame, Items.GUNPOWDER) <= 2
                && dropCount(flame, Items.COAL) >= 1 && dropCount(flame, Items.COAL) <= 2,
                "Flame slime death drops 1–2 of each material");
        flame.remove(Entity.RemovalReason.KILLED);
        var children = h.getLevel().getEntitiesOfClass(VariantSlime.class, flame.getBoundingBox().inflate(3), VariantSlime::isAlive);
        h.assertTrue(children.size() >= 2 && children.size() <= 4 && children.stream().allMatch(child -> child.getSize() == 1),
                "The parent splits into vanilla-style half-sized offspring");
        children.forEach(child -> {
            child.kill();
            child.remove(Entity.RemovalReason.KILLED);
        });
        h.assertTrue(h.getLevel().getEntitiesOfClass(VariantSlime.class, flame.getBoundingBox().inflate(4), VariantSlime::isAlive).isEmpty(),
                "Flame offspring never split again");

        var snow = h.spawn(SlimeVariants.SNOW.get(), 19, 2, 8);
        snow.setSize(4, true);
        h.assertTrue(snow.getSize() == 1, "Snow slimes remain at quarter size");
        snow.kill();
        snow.remove(Entity.RemovalReason.KILLED);
        h.assertTrue(dropCount(snow, SlimeVariants.BLUE_SLIME.get()) == 1, "Snow slimes drop Blue Slime");
        h.assertTrue(h.getLevel().getEntitiesOfClass(VariantSlime.class, snow.getBoundingBox().inflate(4), VariantSlime::isAlive).isEmpty(),
                "Snow slimes never split");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 130)
    public static void flameSlimesSpawnAndHopOnCoveredLava(GameTestHelper h) {
        for (int x = 4; x < 23; x++) {
            for (int z = 4; z < 23; z++) {
                h.setBlock(x, 1, z, Blocks.STONE);
                h.setBlock(x, 2, z, Blocks.LAVA);
                h.setBlock(x, 8, z, Blocks.STONE);
            }
        }
        h.runAfterDelay(12, () -> verifyLavaHops(h));
    }

    private static void verifyLavaHops(GameTestHelper h) {
        var level = h.getLevel();
        var type = SlimeVariants.FLAME.get();
        var pos = h.absolutePos(new BlockPos(13, 2, 13));
        h.assertTrue(SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, pos, level.random),
                "A covered underground lava surface is a valid spawn. Lava: " + level.getFluidState(pos)
                        + ", sky: " + level.canSeeSky(pos.above())
                        + ", height: " + level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        pos.getX(), pos.getZ()));
        h.assertFalse(SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, pos.above(), level.random),
                "Air above lava alone is not a spawn");
        var slime = type.create(level);
        slime.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        h.assertTrue(slime.checkSpawnObstruction(level), "Native spawn obstruction checks accept the lava");
        slime.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
        double surface = pos.getY() + level.getFluidState(pos).getHeight(level, pos);
        h.assertTrue(Math.abs(slime.getY() - surface) < .01, "Flame slimes start on the liquid surface");
        level.addFreshEntity(slime);
        var hopped = new boolean[1];
        for (int tick = 1; tick <= 80; tick++) {
            h.runAfterDelay(tick, () -> {
                h.assertTrue(slime.getY() >= surface - .02, "The flame slime stays at or above the lava surface");
                hopped[0] |= slime.getY() >= surface + .25;
            });
        }
        h.runAfterDelay(81, () -> {
            h.assertTrue(hopped[0] && slime.getHealth() == slime.getMaxHealth(), "The flame slime hops and takes no lava damage");
            h.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void tinySnowSlimeHitsApplyPowderSnowFrost(GameTestHelper h) {
        var slime = h.spawn(SlimeVariants.SNOW.get(), 8, 2, 8);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(slime.position());
        slime.playerTouch(player);
        h.assertTrue(player.getHealth() < player.getMaxHealth(), "The tiny slime can land a normal contact hit");
        h.assertTrue(player.isFullyFrozen(), "The hit uses the native powder-snow frost meter");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void blueSlimeBrewsAllFreezingForms(GameTestHelper h) {
        var level = h.getLevel();
        var brewing = level.potionBrewing();
        var awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        var potion = brewing.mix(SlimeVariants.BLUE_SLIME.toStack(), awkward);
        var splash = brewing.mix(Items.GUNPOWDER.getDefaultInstance(), potion);
        var lingering = brewing.mix(Items.DRAGON_BREATH.getDefaultInstance(), splash);
        var ingredients = new ArrayList<>(Collections.nCopies(9, Items.ARROW.getDefaultInstance()));
        ingredients.set(4, lingering);
        var input = CraftingInput.of(3, 3, ingredients);
        var recipe = new TippedArrowRecipe(CraftingBookCategory.MISC);
        h.assertTrue(recipe.matches(input, level), "Freezing uses the vanilla tipped-arrow recipe");
        var arrows = recipe.assemble(input, level.registryAccess());
        h.assertTrue(isFreezing(potion, Items.POTION) && isFreezing(splash, Items.SPLASH_POTION)
                && isFreezing(lingering, Items.LINGERING_POTION) && isFreezing(arrows, Items.TIPPED_ARROW)
                && arrows.getCount() == 8, "All four delivery forms retain their freezing potion data");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void drinkingAndSplashingGiveAFullThreeSecondFreeze(GameTestHelper h) {
        floor(h);
        var drinker = cow(h, 8);
        var splashTarget = cow(h, 16);
        var drink = PotionContents.createItemStack(Items.POTION, SlimeVariants.FREEZING_POTION);
        drink.getItem().finishUsingItem(drink, h.getLevel(), drinker);
        impact(h, splashTarget, Items.SPLASH_POTION);
        h.assertTrue(freezeDuration(drinker) == 60 && freezeDuration(splashTarget) == 60,
                "Drinking and splash impact both freeze for 60 ticks immediately");
        h.assertTrue(drinker.isFullyFrozen() && drinker.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0,
                "The freeze meter fills and the target stops moving");
        h.runAfterDelay(58, () -> h.assertTrue(drinker.isFullyFrozen() && freezeDuration(drinker) > 0,
                "Frost remains through the three-second timer"));
        h.runAfterDelay(62, () -> {
            h.assertTrue(!drinker.isFullyFrozen() && freezeDuration(drinker) == 0
                    && drinker.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0, "The potion wears off after three seconds");
            h.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void lingeringAndArrowEffectsKeepTheFullFreezeDuration(GameTestHelper h) {
        floor(h);
        var cloudTarget = cow(h, 8);
        var arrowTarget = cow(h, 17);
        impact(h, cloudTarget, Items.LINGERING_POTION);
        var arrows = PotionContents.createItemStack(Items.TIPPED_ARROW, SlimeVariants.FREEZING_POTION);
        var arrow = new Arrow(h.getLevel(), arrowTarget.getX() - 3, arrowTarget.getY(.55), arrowTarget.getZ(), arrows, null);
        arrow.shoot(1, 0, 0, 1.5F, 0);
        h.getLevel().addFreshEntity(arrow);
        h.runAfterDelay(6, () -> h.assertTrue(freezeDuration(arrowTarget) >= 54,
                "A real tipped arrow impact applies the full timer rather than one eighth"));
        h.succeedWhen(() -> h.assertTrue(freezeDuration(cloudTarget) >= 55 && freezeDuration(arrowTarget) > 0,
                "A real lingering cloud applies the full timer to the target"));
    }

    private static void floor(GameTestHelper h) {
        for (int x = 5; x < 22; x++) for (int z = 5; z < 13; z++) h.setBlock(x, 1, z, Blocks.STONE);
    }

    private static LivingEntity cow(GameTestHelper h, int x) {
        var cow = h.spawn(EntityType.COW, x, 2, 8);
        cow.setNoAi(true);
        return cow;
    }

    private static void impact(GameTestHelper h, LivingEntity target, Item container) {
        class TestPotion extends ThrownPotion {
            TestPotion() {
                super(h.getLevel(), target.getX(), target.getY(), target.getZ());
            }

            void hit() {
                setItem(PotionContents.createItemStack(container, SlimeVariants.FREEZING_POTION));
                onHit(new EntityHitResult(target));
            }
        }
        var potion = new TestPotion();
        potion.hit();
        h.assertTrue(potion.isRemoved(), "The thrown potion shattered");
    }

    private static boolean isFreezing(ItemStack stack, Item container) {
        return stack.is(container) && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .is(SlimeVariants.FREEZING_POTION);
    }

    private static int freezeDuration(LivingEntity target) {
        return target.getActiveEffects().stream().filter(effect -> effect.getEffect().is(SlimeVariants.id("frozen")))
                .mapToInt(effect -> effect.getDuration()).findFirst().orElse(0);
    }

    private static int dropCount(VariantSlime slime, Item item) {
        return slime.level().getEntitiesOfClass(ItemEntity.class, slime.getBoundingBox().inflate(3),
                entity -> entity.getItem().is(item)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
    }
}
