package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.*;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftStructure;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FeudalGameTests {
    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 18; x++) for (int z = 0; z < 18; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
    }
    private static <T extends Mob> T mob(GameTestHelper h, EntityType<? extends T> type, int x, int z) {
        T mob = h.spawn(type, x, 2, z); mob.setNoAi(true); mob.setOnGround(true); return mob;
    }
    private static Spider pet(GameTestHelper h, boolean cave, UUID owner) {
        Spider spider = mob(h, cave ? EntityType.CAVE_SPIDER : EntityType.SPIDER, 4, 4);
        SpiderCompanion.tame(spider, owner); SpiderCompanion.setSitting(spider, false); return spider;
    }
    private static Player player(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL); player.moveTo(h.absoluteVec(new Vec3(5, 2, 4))); return player;
    }
    private static ServerPlayer switchablePlayer(GameTestHelper h) {
        UUID id = UUID.randomUUID();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(id, "ffm-" + id.toString().substring(0, 8)), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }
    private static void near(GameTestHelper h, double actual, double expected, String label) {
        h.assertTrue(Math.abs(actual - expected) < 0.001, label + ": expected " + expected + ", got " + actual);
    }

    @GameTest(template = "test_empty")
    public static void captureBee(GameTestHelper h) {
        floor(h); Player player = player(h); Bee bee = mob(h, EntityType.BEE, 4, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        var event = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, bee);
        NeoForge.EVENT_BUS.post(event);
        h.assertTrue(event.isCanceled() && bee.isRemoved(), "Bee capture must consume the bee and handle the interaction");
        h.assertTrue(player.getMainHandItem().is(FantasyFeudalismMobs.BEE_BOTTLE), "Capture must return a Bee Bottle");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void tamingAndEmptyBottle(GameTestHelper h) {
        floor(h); Player p = player(h);
        for (boolean cave : new boolean[]{false, true}) {
            Spider spider = mob(h, cave ? EntityType.CAVE_SPIDER : EntityType.SPIDER, 4, cave ? 6 : 4);
            long success = 0;
            while (RandomSource.create(success).nextInt(100) >= 33) success++;
            spider.getRandom().setSeed(success);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FantasyFeudalismMobs.BEE_BOTTLE.get()));
            SpiderCompanion.interact(p, InteractionHand.MAIN_HAND, spider);
            h.assertTrue(SpiderCompanion.ownedBy(spider, p), "Both spider types must tame on a successful roll");
            h.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE), "Taming returns the empty bottle");
            h.assertTrue(SpiderCompanion.isSitting(spider), "New companion starts sitting like a wolf");
            long failure = 0;
            while (RandomSource.create(failure).nextInt(100) < 33) failure++;
            Spider wild = mob(h, cave ? EntityType.CAVE_SPIDER : EntityType.SPIDER, 8, cave ? 6 : 4);
            wild.getRandom().setSeed(failure);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FantasyFeudalismMobs.BEE_BOTTLE.get()));
            SpiderCompanion.interact(p, InteractionHand.MAIN_HAND, wild);
            h.assertFalse(SpiderCompanion.isTamed(wild), "Failed roll must not tame");
            h.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE), "Failed taming also returns the bottle");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void companionAttributesAndTerrain(GameTestHelper h) {
        floor(h); Spider spider = pet(h, false, UUID.randomUUID()); Wolf wolf = mob(h, EntityType.WOLF, 9, 9);
        wolf.setTame(true, true);
        near(h, spider.getMaxHealth(), wolf.getMaxHealth() * 0.75, "Tamed health");
        near(h, spider.getAttributeValue(Attributes.MOVEMENT_SPEED), wolf.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.2, "Tamed speed");
        for (PathType type : new PathType[]{PathType.WATER, PathType.WATER_BORDER, PathType.LAVA, PathType.DAMAGE_FIRE, PathType.DANGER_FIRE})
            h.assertTrue(spider.getPathfindingMalus(type) < 0, "Companion avoids " + type);
        h.assertFalse(spider.getNavigation().canFloat(), "Spider navigation must not swim");
        h.assertFalse(spider.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof FloatGoal), "No float goal");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void rawFoodHealing(GameTestHelper h) {
        floor(h); Player p = player(h); Spider spider = pet(h, false, p.getUUID());
        for (Item food : new Item[]{Items.BEEF, Items.PORKCHOP, Items.CHICKEN, Items.MUTTON, Items.RABBIT,
                Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH, Items.ROTTEN_FLESH}) {
            ItemStack stack = new ItemStack(food); h.assertTrue(SpiderCompanion.healingFood(stack), "Missing healing food " + food);
            p.setItemInHand(InteractionHand.MAIN_HAND, stack); spider.setHealth(1);
            int nutrition = stack.getFoodProperties(spider).nutrition();
            SpiderCompanion.interact(p, InteractionHand.MAIN_HAND, spider);
            near(h, spider.getHealth(), 1 + nutrition * 2, "Vanilla wolf healing for " + food);
            h.assertTrue(p.getMainHandItem().isEmpty(), "Healing consumes one food");
        }
        h.assertFalse(SpiderCompanion.healingFood(new ItemStack(Items.COOKED_BEEF)), "Cooked food is not raw meat");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void separateHarvestCooldownsAndReload(GameTestHelper h) {
        floor(h); Player p = player(h); Spider spider = pet(h, true, p.getUUID());
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        h.assertTrue(SpiderCompanion.harvest(p, InteractionHand.MAIN_HAND, spider, true), "Initial silk harvest");
        int string = p.getInventory().countItem(Items.STRING);
        h.assertTrue(string >= 1 && string <= 4, "Silk yield is 1-4 string");
        near(h, p.getMainHandItem().getDamageValue(), 1, "Shears durability");
        h.assertFalse(SpiderCompanion.harvest(p, InteractionHand.MAIN_HAND, spider, true), "Silk cooldown blocks repeat harvest");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        h.assertTrue(SpiderCompanion.harvest(p, InteractionHand.MAIN_HAND, spider, false), "Poison has a separate cooldown");
        h.assertTrue(p.getMainHandItem().is(Items.POTION), "Poison output is a potion");
        h.assertTrue(p.getMainHandItem().get(DataComponents.POTION_CONTENTS).is(Potions.POISON), "Standard vanilla Poison potion");
        long now = h.getLevel().getGameTime();
        h.assertTrue(SpiderCompanion.data(spider).getLong(SpiderCompanion.SILK_READY) == now + 48000, "Two-day silk cooldown");
        h.assertTrue(SpiderCompanion.data(spider).getLong(SpiderCompanion.POISON_READY) == now + 48000, "Two-day poison cooldown");
        CompoundTag saved = spider.saveWithoutId(new CompoundTag());
        CaveSpider restored = EntityType.CAVE_SPIDER.create(h.getLevel()); restored.load(saved); spider.discard(); h.getLevel().addFreshEntity(restored);
        h.assertTrue(SpiderCompanion.ownedBy(restored, p), "Ownership survives save/load");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        h.assertFalse(SpiderCompanion.harvest(p, InteractionHand.MAIN_HAND, restored, false), "Poison cooldown survives reload");
        SpiderCompanion.data(restored).putLong(SpiderCompanion.POISON_READY, now);
        h.assertTrue(SpiderCompanion.harvest(p, InteractionHand.MAIN_HAND, restored, false), "Harvest resumes at cooldown boundary");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void spiderEffects(GameTestHelper h) {
        floor(h);
        for (boolean cave : new boolean[]{false, true}) {
            Spider spider = pet(h, cave, UUID.randomUUID()); Creeper target = mob(h, EntityType.CREEPER, 6, cave ? 6 : 4);
            h.assertTrue(spider.doHurtTarget(target), "Attack must land");
            for (var effect : List.of(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS)) {
                h.assertTrue(target.hasEffect(effect), "Required spider effect");
                near(h, target.getEffect(effect).getDuration(), 120, "Six-second duration");
                near(h, target.getEffect(effect).getAmplifier(), 0, "Level I");
            }
            h.assertTrue(target.hasEffect(MobEffects.POISON) == cave, "Poison only for cave spiders");
            if (cave) near(h, target.getEffect(MobEffects.POISON).getDuration(), 120, "Cave poison replaces vanilla 7/15 seconds");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void spiderEffectExclusionsIncludeVariants(GameTestHelper h) {
        floor(h); Spider spider = pet(h, true, UUID.randomUUID());
        for (EntityType<? extends Mob> type : List.of(EntityType.SKELETON, EntityType.WITHER_SKELETON, EntityType.STRAY, EntityType.BOGGED,
                EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER)) {
            Mob target = mob(h, type, 7, 7); spider.doHurtTarget(target);
            h.assertFalse(target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) || target.hasEffect(MobEffects.WEAKNESS)
                    || target.hasEffect(MobEffects.POISON), "Excluded variant gained effects: " + type);
            target.discard();
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void spiderFireDamage(GameTestHelper h) {
        floor(h); Spider spider = pet(h, false, UUID.randomUUID());
        spider.hurt(h.getLevel().damageSources().inFire(), 4);
        near(h, spider.getHealth(), 25, "Fire deals 25% extra damage"); h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void spiderJumpHeight(GameTestHelper h) {
        floor(h); Spider spider = mob(h, EntityType.SPIDER, 5, 5);
        spider.setInvulnerable(true);
        spider.goalSelector.removeAllGoals(g -> true); spider.targetSelector.removeAllGoals(g -> true); spider.setNoAi(false);
        double[] start = {spider.getY()}, peak = {spider.getY()};
        List<String> trace = new ArrayList<>();
        h.runAfterDelay(2, () -> { start[0] = spider.getY(); peak[0] = start[0]; spider.jumpFromGround(); });
        for (int tick = 3; tick <= 32; tick++) h.runAfterDelay(tick, () -> {
            peak[0] = Math.max(peak[0], spider.getY());
            trace.add(spider.tickCount + ":" + (spider.getY() - start[0]) + "/" + spider.getDeltaMovement().y);
        });
        h.runAfterDelay(34, () -> {
            h.assertTrue(peak[0] - start[0] >= 3 && peak[0] - start[0] < 3.1, "Jump must reach a three-block landing: " + (peak[0] - start[0])
                    + " gravity=" + spider.getGravity() + " strength=" + spider.getAttributeValue(Attributes.JUMP_STRENGTH) + " " + trace); h.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void spiderPathPlansThreeBlockJump(GameTestHelper h) {
        floor(h);
        for (int x = 8; x <= 12; x++) for (int y = 2; y <= 4; y++) for (int z = 2; z <= 12; z++) h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        Spider spider = mob(h, EntityType.SPIDER, 5, 6);
        var path = spider.getNavigation().createPath(h.absolutePos(new BlockPos(10, 5, 6)), 0);
        h.assertTrue(path != null && path.canReach(), "Pathfinder must reach the three-block ledge");
        boolean jump = false;
        for (int i = 1; i < path.getNodeCount(); i++) if (path.getNode(i).y - path.getNode(i - 1).y == 3) jump = true;
        h.assertTrue(jump, "The route must contain a three-block jump edge");
        spider.setInvulnerable(true);
        spider.goalSelector.removeAllGoals(g -> true); spider.targetSelector.removeAllGoals(g -> true);
        spider.getNavigation().moveTo(path, 1.0); spider.setNoAi(false);
        h.succeedWhen(() -> h.assertTrue(spider.getY() >= h.absolutePos(new BlockPos(10, 5, 6)).getY()
                && spider.distanceToSqr(h.absoluteVec(new Vec3(10.5, 5, 6.5))) < 4, "Spider must traverse its planned route"));
    }

    @GameTest(template = "test_empty", timeoutTicks = 60)
    public static void spiderPouncesForwardOnLevelGround(GameTestHelper h) {
        floor(h);
        List<Spider> spiders = new ArrayList<>();
        Map<Spider, Vec3> starts = new HashMap<>();
        Map<Spider, Double> peaks = new HashMap<>();
        for (int i = 0; i < 4; i++) {
            Spider spider = mob(h, i < 2 ? EntityType.SPIDER : EntityType.CAVE_SPIDER, i % 2 == 0 ? 3 : 11, i < 2 ? 3 : 11);
            if (i % 2 != 0) SpiderCompanion.tame(spider, UUID.randomUUID());
            SpiderCompanion.setSitting(spider, false);
            var target = mob(h, EntityType.ZOMBIE, i % 2 == 0 ? 3 : 11, (i < 2 ? 3 : 11) + 3);
            spider.setTarget(target); spider.setInvulnerable(true);
            Goal leap = spider.goalSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal)
                    .filter(g -> g instanceof LeapAtTargetGoal).findFirst().orElseThrow();
            spider.goalSelector.removeAllGoals(g -> true); spider.targetSelector.removeAllGoals(g -> true);
            spider.setNoAi(false); spiders.add(spider);
            h.runAfterDelay(2, () -> {
                starts.put(spider, spider.position()); peaks.put(spider, spider.getY()); leap.start();
            });
        }
        for (int tick = 3; tick <= 28; tick++) h.runAfterDelay(tick, () -> {
            for (Spider spider : spiders) peaks.merge(spider, spider.getY(), Math::max);
        });
        h.runAfterDelay(30, () -> {
            for (Spider spider : spiders) {
                double height = peaks.get(spider) - starts.get(spider).y;
                double forward = spider.getZ() - starts.get(spider).z;
                h.assertTrue(height > 0.4 && height < 1, "Level pounce must have a low arc, got " + height);
                h.assertTrue(forward >= 1.5 && forward < 3, "Pounce must approach without passing the three-block target, got " + forward);
                h.assertTrue(spider.onGround(), "Pounce must land again");
            }
            h.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void spiderNeutralityAndSitOrders(GameTestHelper h) {
        floor(h); ServerPlayer owner = h.makeMockServerPlayerInLevel(); owner.moveTo(h.absoluteVec(new Vec3(6, 2, 6)));
        Spider pet = pet(h, false, owner.getUUID()); Spider wild = mob(h, EntityType.CAVE_SPIDER, 8, 8);
        h.assertTrue(SpiderCompanion.protectedByFollower(owner), "Following pet protects owner");
        wild.setTarget(owner); h.assertTrue(wild.getTarget() == null, "Wild cave spider remains neutral");
        SpiderCompanion.interact(owner, InteractionHand.MAIN_HAND, pet);
        h.assertTrue(SpiderCompanion.isSitting(pet), "Owner can order sitting");
        h.assertFalse(SpiderCompanion.protectedByFollower(owner), "Sitting pet does not grant follower neutrality");
        owner.getServer().getPlayerList().remove(owner); h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void skeletonWeaponsAndRange(GameTestHelper h) {
        floor(h); AbstractSkeleton skeleton = mob(h, EntityType.STRAY, 5, 5);
        near(h, skeleton.getAttributeValue(Attributes.FOLLOW_RANGE), 35, "Skeleton detection range");
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        h.assertTrue(skeleton instanceof CrossbowAttackMob, "Skeleton implements vanilla crossbow contract");
        h.assertTrue(skeleton.canFireProjectileWeapon((CrossbowItem)Items.CROSSBOW), "Crossbows supported");
        h.assertTrue(skeleton.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof RangedCrossbowAttackGoal), "Crossbow AI installed");
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        h.assertFalse(skeleton.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof RangedCrossbowAttackGoal), "Sword removes crossbow goal");
        h.assertTrue(skeleton.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof MeleeAttackGoal), "Sword uses melee AI");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 180)
    public static void skeletonActuallyFiresCrossbow(GameTestHelper h) {
        floor(h); AbstractSkeleton skeleton = mob(h, EntityType.BOGGED, 3, 5);
        skeleton.moveTo(h.absoluteVec(new Vec3(3.5, 8, 5.5))); skeleton.setNoGravity(true); skeleton.setInvulnerable(true);
        var target = mob(h, EntityType.IRON_GOLEM, 14, 5); target.setInvulnerable(true);
        target.moveTo(skeleton.position().add(34, 0, 0));
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        skeleton.goalSelector.removeAllGoals(g -> !(g instanceof RangedCrossbowAttackGoal));
        skeleton.targetSelector.removeAllGoals(g -> true); skeleton.setTarget(target); skeleton.setNoAi(false);
        h.succeedWhen(() -> h.assertTrue(!h.getLevel().getEntitiesOfClass(AbstractArrow.class, skeleton.getBoundingBox().inflate(20),
                a -> a.getOwner() == skeleton).isEmpty(), "Crossbow AI must load and fire a real arrow; target=" + skeleton.getTarget()
                + " distance=" + skeleton.distanceTo(target) + " visible=" + skeleton.getSensing().hasLineOfSight(target)
                + " using=" + skeleton.isUsingItem() + " item=" + skeleton.getMainHandItem() + " tick=" + skeleton.tickCount
                + " goals=" + skeleton.goalSelector.getAvailableGoals().stream().map(g -> g.getGoal().getClass().getSimpleName() + ":" + g.isRunning()).toList()));
    }

    @GameTest(template = "test_empty")
    public static void skeletonProjectileSpeed(GameTestHelper h) {
        floor(h); var skeleton = mob(h, EntityType.WITHER_SKELETON, 4, 4);
        Arrow arrow = new Arrow(h.getLevel(), skeleton, new ItemStack(Items.ARROW), skeleton.getMainHandItem());
        arrow.setDeltaMovement(1.6, 0, 0); h.getLevel().addFreshEntity(arrow);
        near(h, arrow.getDeltaMovement().length(), 2.24, "Skeleton projectile speed is multiplied by 1.4"); h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void crossbowsRetainSkeletonVariantEffects(GameTestHelper h) {
        floor(h);
        for (var type : List.of(EntityType.STRAY, EntityType.BOGGED)) {
            AbstractSkeleton skeleton = mob(h, type, 4, 4);
            Arrow arrow = new Arrow(h.getLevel(), skeleton, new ItemStack(Items.ARROW), new ItemStack(Items.CROSSBOW));
            arrow.setDeltaMovement(3.15, 0, 0); h.getLevel().addFreshEntity(arrow);
            near(h, arrow.getDeltaMovement().length(), 3.15 * 1.4, "Crossbow speed multiplier");
            var effects = arrow.getPickupItemStackOrigin().get(DataComponents.POTION_CONTENTS);
            var expected = type == EntityType.BOGGED ? MobEffects.POISON : MobEffects.MOVEMENT_SLOWDOWN;
            h.assertTrue(effects != null && effects.customEffects().stream().anyMatch(e -> e.is(expected)
                    && e.getDuration() == (type == EntityType.BOGGED ? 100 : 600)), "Crossbow retains vanilla variant arrow effect");
            skeleton.discard(); arrow.discard();
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void skeletonTargetRetention(GameTestHelper h) {
        floor(h); var skeleton = mob(h, EntityType.SKELETON, 4, 4); skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        var first = mob(h, EntityType.IRON_GOLEM, 12, 12); var next = mob(h, EntityType.IRON_GOLEM, 14, 14);
        skeleton.setTarget(first); skeleton.setTarget(next);
        h.assertTrue(skeleton.getTarget() == first, "Ranged skeleton keeps its living target");
        ServerPlayer player = h.makeMockServerPlayerInLevel(); player.moveTo(skeleton.position().add(4, 0, 0));
        skeleton.setTarget(next); h.assertTrue(skeleton.getTarget() == next, "Player inside five blocks permits retargeting");
        player.getServer().getPlayerList().remove(player); h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void skeletonReleasesCreativeAndSpectatorPlayers(GameTestHelper h) {
        floor(h);
        ServerPlayer player = switchablePlayer(h);
        try {
            for (var weapon : List.of(Items.BOW, Items.CROSSBOW)) {
                var skeleton = mob(h, EntityType.SKELETON, 3, 3);
                skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
                player.moveTo(skeleton.position().add(8, 0, 0));
                for (var mode : List.of(GameType.CREATIVE, GameType.SPECTATOR)) {
                    player.setGameMode(GameType.SURVIVAL); skeleton.setTarget(player);
                    h.assertTrue(skeleton.getTarget() == player, "Survival player must be a valid initial target");
                    player.setGameMode(mode);
                    NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(skeleton));
                    h.assertTrue(skeleton.getTarget() == null, "Skeleton must release a player changing to " + mode + " with " + weapon);
                    skeleton.setTarget(player);
                    h.assertTrue(skeleton.getTarget() == null, "Creative/Spectator players cannot be acquired again");
                }
                player.setGameMode(GameType.SURVIVAL); skeleton.setTarget(player); skeleton.setTarget(null);
                h.assertTrue(skeleton.getTarget() == null, "Vanilla AI must be allowed to clear an expired target");
                skeleton.discard();
            }
        } finally { player.getServer().getPlayerList().remove(player); }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 220)
    public static void zombiePursuesMovingPlayer(GameTestHelper h) {
        floor(h);
        var player = switchablePlayer(h);
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(h.absoluteVec(new Vec3(12, 2, 12)));
        Zombie zombie = mob(h, EntityType.ZOMBIE, 3, 3);
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        zombie.setInvulnerable(true); zombie.setTarget(player); zombie.setNoAi(false);
        double start = zombie.distanceTo(player);
        int[] running = {0}, stalled = {0};
        for (int tick = 1; tick <= 120; tick++) {
            final int t = tick;
            h.runAfterDelay(tick, () -> {
                player.setHealth(player.getMaxHealth());
                if (t > 40) player.moveTo(h.absoluteVec(new Vec3(12 - (t - 40) * 0.05, 2, 12)));
                if (zombie.distanceTo(player) > 3 && t > 10) {
                    running[0]++;
                    if (zombie.getDeltaMovement().horizontalDistanceSqr() < 0.0001) stalled[0]++;
                }
            });
        }
        h.runAfterDelay(125, () -> {
            try {
                h.assertTrue(zombie.distanceTo(player) < 3, "Zombie should catch the slowly moving player; start=" + start + " final=" + zombie.distanceTo(player));
                h.assertTrue(running[0] > 0 && stalled[0] < running[0] / 3, "Pursuit should advance consistently; stalled=" + stalled[0] + "/" + running[0]);
            } finally { player.getServer().getPlayerList().remove(player); }
            h.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void zombieKeepsPursuitWhenShortPathFinishes(GameTestHelper h) {
        floor(h);
        long previousTime = h.getLevel().getDayTime();
        h.getLevel().setDayTime(18000); // Drowned intentionally avoid chasing dry targets during daytime.
        h.getLevel().updateSkyBrightness();
        var player = switchablePlayer(h);
        player.setGameMode(GameType.SURVIVAL);
        try {
            for (var type : List.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED,
                    EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIFIED_PIGLIN)) {
                Zombie zombie = mob(h, type, 4, 4);
                player.moveTo(zombie.position().add(2, 0, 0)); zombie.setTarget(player);
                var attack = zombie.goalSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal)
                        .filter(g -> g instanceof ZombieAttackGoal).map(g -> (ZombieAttackGoal)g).findFirst().orElseThrow();
                zombie.getNavigation().moveTo(player, 1);
                zombie.getNavigation().stop(); // The old short route has finished as the player moves on.
                player.moveTo(zombie.position().add(4, 0, 0));
                h.assertFalse(new MeleeAttackGoal(zombie, 1, false).canContinueToUse(), "Vanilla short-path policy is the regression control");
                h.assertTrue(attack.canContinueToUse(), "A completed short route must not restart pursuit for " + type);
                player.setGameMode(GameType.CREATIVE);
                h.assertFalse(attack.canContinueToUse(), "Creative mode still ends zombie pursuit");
                player.setGameMode(GameType.SURVIVAL); zombie.discard();
            }
        } finally {
            player.getServer().getPlayerList().remove(player);
            h.getLevel().setDayTime(previousTime); h.getLevel().updateSkyBrightness();
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void endermanOneSuccessfulHit(GameTestHelper h) {
        floor(h); EnderMan enderman = mob(h, EntityType.ENDERMAN, 5, 5); Player player = player(h);
        enderman.setTarget(player); Vec3 before = enderman.position();
        h.assertTrue(enderman.doHurtTarget(player), "Enderman attack lands");
        near(h, player.getHealth(), 20 - 7 * 1.35, "Enderman damage boost");
        h.assertTrue(enderman.getTarget() == null && enderman.getRemainingPersistentAngerTime() == 0, "Aggression clears after successful hit");
        h.assertTrue(enderman.position().distanceToSqr(before) > 0, "Enderman teleports away");
        enderman.setTarget(player); h.assertTrue(enderman.getTarget() == player, "Enderman can be re-provoked immediately"); h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void zombieVariationAndLowHealth(GameTestHelper h) {
        floor(h);
        for (var type : List.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIFIED_PIGLIN)) {
            Zombie zombie = mob(h, type, 6, 6);
            double baseline = zombie.getAttributeValue(Attributes.MOVEMENT_SPEED);
            double attack = zombie.getAttributeValue(Attributes.ATTACK_DAMAGE);
            h.assertTrue(baseline >= zombie.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * 0.8 - 1e-6
                    && baseline <= zombie.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * 1.2 + 1e-6, "Speed range " + type);
            h.assertTrue(attack >= zombie.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) * 0.8 - 1e-6
                    && attack <= zombie.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) * 1.2 + 1e-6, "Damage range " + type);
            zombie.setHealth(5.9F); MobRules.updateZombie(zombie); MobRules.updateZombie(zombie);
            near(h, zombie.getAttributeValue(Attributes.MOVEMENT_SPEED), baseline * 2, "Low-health speed does not stack");
            zombie.setHealth(6); MobRules.updateZombie(zombie);
            near(h, zombie.getAttributeValue(Attributes.MOVEMENT_SPEED), baseline, "Three-heart threshold"); zombie.discard();
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void phantomHealthThresholdAndInfiniteSpeed(GameTestHelper h) {
        floor(h); var previous = h.getLevel().getDifficulty();
        Phantom phantom;
        try { h.getLevel().getServer().setDifficulty(Difficulty.HARD, true); phantom = mob(h, EntityType.PHANTOM, 5, 5); }
        finally { h.getLevel().getServer().setDifficulty(previous, true); }
        h.assertTrue(phantom.hasEffect(MobEffects.MOVEMENT_SPEED), "Hard-spawned phantom has Swiftness");
        h.assertTrue(phantom.getEffect(MobEffects.MOVEMENT_SPEED).isInfiniteDuration(), "Swiftness is infinite");
        near(h, phantom.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier(), 0, "Swiftness I");
        Player player = player(h); phantom.setTarget(player); h.assertTrue(phantom.getTarget() == null, "Healthy players are not targeted");
        player.setHealth(8); phantom.setTarget(player); h.assertTrue(phantom.getTarget() == player, "Four-heart players can be targeted");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void creeperExplosionPreservesBlocks(GameTestHelper h) {
        floor(h); Creeper creeper = mob(h, EntityType.CREEPER, 8, 8);
        h.setBlock(new BlockPos(8, 1, 8), Blocks.COBBLESTONE);
        h.setBlock(new BlockPos(9, 1, 8), Blocks.DIAMOND_BLOCK);
        h.getLevel().explode(creeper, creeper.getX(), creeper.getY(), creeper.getZ(), 3, Level.ExplosionInteraction.MOB);
        h.assertBlockPresent(Blocks.MOSSY_COBBLESTONE, new BlockPos(8, 1, 8));
        h.assertBlockPresent(Blocks.DIAMOND_BLOCK, new BlockPos(9, 1, 8));
        int plants = 0;
        for (int x = 5; x <= 11; x++) for (int z = 5; z <= 11; z++) if (!h.getBlockState(new BlockPos(x, 2, z)).isAir()) plants++;
        h.assertTrue(plants > 0, "Creeper explosion plants vegetation"); h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void creeperConversionTogglePreservesVegetation(GameTestHelper h) {
        floor(h); Creeper creeper = mob(h, EntityType.CREEPER, 8, 8); h.setBlock(new BlockPos(8, 1, 8), Blocks.COBBLESTONE);
        try {
            FeudalConfig.CREEPER_BLOCK_CONVERSION.set(false);
            h.getLevel().explode(creeper, creeper.getX(), creeper.getY(), creeper.getZ(), 3, Level.ExplosionInteraction.MOB);
            h.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(8, 1, 8));
            h.assertTrue(!h.getBlockState(new BlockPos(7, 2, 8)).isAir()
                    || !h.getBlockState(new BlockPos(8, 2, 7)).isAir()
                    || !h.getBlockState(new BlockPos(9, 2, 8)).isAir()
                    || !h.getBlockState(new BlockPos(8, 2, 9)).isAir(), "Conversion toggle must retain planting");
        } finally { FeudalConfig.CREEPER_BLOCK_CONVERSION.set(true); }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void mossyBlockProperties(GameTestHelper h) {
        var original = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                .setValue(BlockStateProperties.WATERLOGGED, true);
        var result = CreeperVegetation.mossy(original);
        h.assertTrue(result.is(Blocks.MOSSY_STONE_BRICK_STAIRS), "Mossy stair conversion");
        h.assertTrue(result.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.WEST
                && result.getValue(BlockStateProperties.WATERLOGGED), "Conversion retains orientation and waterlogging"); h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void spawnWeightsAndGroups(GameTestHelper h) {
        var list = WeightedRandomList.create(new MobSpawnSettings.SpawnerData(EntityType.SPIDER, 100, 4, 4),
                new MobSpawnSettings.SpawnerData(EntityType.CREEPER, 100, 4, 4), new MobSpawnSettings.SpawnerData(EntityType.GHAST, 50, 1, 1),
                new MobSpawnSettings.SpawnerData(EntityType.HUSK, 95, 4, 4));
        var event = new LevelEvent.PotentialSpawns(h.getLevel(), MobCategory.MONSTER, h.absolutePos(new BlockPos(5, 2, 5)), list);
        NeoForge.EVENT_BUS.post(event);
        for (var data : event.getSpawnerDataList()) {
            if (data.type == EntityType.SPIDER) near(h, data.getWeight().asInt(), 50, "Spider weight");
            if (data.type == EntityType.CREEPER) near(h, data.getWeight().asInt(), 40, "Creeper weight");
            if (data.type == EntityType.GHAST) {
                near(h, data.getWeight().asInt(), 40, "Ghast weight");
                h.assertTrue(data.minCount == 3 && data.maxCount == 3, "Three-ghast groups");
            }
            if (data.type == EntityType.HUSK) h.assertTrue(data.minCount == 4 && data.maxCount == 9, "Zombie-variant groups");
        }
        Ghast ghast = mob(h, EntityType.GHAST, 6, 6); h.assertTrue(ghast.getMaxSpawnClusterSize() == 3, "Vanilla ghast group cap must also change");
        Zombie zombie = mob(h, EntityType.ZOMBIE, 12, 12); h.assertTrue(zombie.getMaxSpawnClusterSize() == 9, "Zombie group cap supports nine");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void armorMaterialsAndEnchantments(GameTestHelper h) {
        floor(h); var skeleton = mob(h, EntityType.SKELETON, 5, 5); int[] materials = new int[4];
        for (int n = 0; n < 100; n++) materials[MobRules.armorMaterial(n)]++;
        h.assertTrue(Arrays.equals(materials, new int[]{50, 5, 25, 20}), "Armor material distribution and iron cap");
        near(h, MobRules.armorChance(Difficulty.EASY), 0.2, "Easy chance");
        near(h, MobRules.armorChance(Difficulty.NORMAL), 0.35, "Normal chance");
        near(h, MobRules.armorChance(Difficulty.HARD), 0.6, "Hard chance");
        h.assertFalse(MobRules.armorEligible(mob(h, EntityType.PHANTOM, 8, 8)), "Non-armored undead excluded");
        int plain = 0, one = 0, two = 0;
        RandomSource random = RandomSource.create(72491);
        for (int i = 0; i < 1000; i++) {
            ItemStack armor = new ItemStack(Items.IRON_CHESTPLATE); skeleton.setItemSlot(EquipmentSlot.CHEST, armor);
            MobRules.enchantArmor(skeleton, random, EquipmentSlot.CHEST);
            var enchantments = armor.getEnchantments();
            if (enchantments.isEmpty()) plain++;
            for (var entry : enchantments.entrySet()) {
                if (entry.getIntValue() == 1) one++; else if (entry.getIntValue() == 2) two++;
                else h.fail("Unexpected enchantment level " + entry.getIntValue());
            }
        }
        h.assertTrue(plain > 440 && plain < 560 && one > 290 && one < 410 && two > 100 && two < 200,
                "Armor enchantment probabilities: " + plain + "/" + one + "/" + two); h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void blazeDashDistanceCooldownAndToggle(GameTestHelper h) {
        floor(h); Blaze blaze = mob(h, EntityType.BLAZE, 5, 5); blaze.setTarget(mob(h, EntityType.IRON_GOLEM, 10, 5));
        long seed = 0; while (RandomSource.create(seed).nextFloat() >= 0.2F) seed++;
        blaze.getRandom().setSeed(seed); MobRules.tryDash(blaze, 100);
        h.assertTrue(blaze.getPersistentData().getInt("ffm_dash_ticks") == 10, "Dash starts for ten ticks");
        Vec3 start = blaze.position(); for (int i = 0; i < 10; i++) MobRules.dashMovement(blaze);
        near(h, blaze.position().subtract(start).horizontalDistance(), 4, "Four-block dash distance");
        blaze.getRandom().setSeed(seed); MobRules.tryDash(blaze, 120);
        h.assertTrue(blaze.getPersistentData().getInt("ffm_dash_ticks") == 0, "Five-second cooldown blocks early dash");
        blaze.getRandom().setSeed(seed); MobRules.tryDash(blaze, 200);
        h.assertTrue(blaze.getPersistentData().getInt("ffm_dash_ticks") == 10, "Dash resumes after five seconds");
        try { FeudalConfig.BLAZE_DASHING.set(false); h.assertFalse(MobRules.dashMovement(blaze), "Toggle disables dash"); }
        finally { FeudalConfig.BLAZE_DASHING.set(true); }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void silverfishFleeSpidersAndHurtZombies(GameTestHelper h) {
        floor(h); Silverfish fish = mob(h, EntityType.SILVERFISH, 5, 5); Zombie zombie = mob(h, EntityType.ZOMBIE, 7, 5);
        h.assertTrue(fish.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof AvoidEntityGoal), "Silverfish flee spiders");
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(0);
        fish.doHurtTarget(zombie); near(h, zombie.getHealth(), 18, "Silverfish deal double damage to zombies"); h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void actualLootOverrides(GameTestHelper h) {
        floor(h);
        var ghast = mob(h, EntityType.GHAST, 5, 5);
        ghast.hurt(h.getLevel().damageSources().generic(), 1000);
        int tears = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, ghast.getBoundingBox().inflate(3))
                .stream().filter(e -> e.getItem().is(Items.GHAST_TEAR)).mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(tears == 2, "A killed ghast must drop exactly two tears: " + tears);
        var creeper = mob(h, EntityType.CREEPER, 12, 12);
        creeper.hurt(h.getLevel().damageSources().generic(), 1000);
        int moss = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, creeper.getBoundingBox().inflate(3))
                .stream().filter(e -> e.getItem().is(Items.MOSS_BLOCK)).mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(moss >= 1 && moss <= 2, "Killed creeper must drop one or two moss blocks: " + moss); h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void endermanPickupConfiguration(GameTestHelper h) {
        floor(h); var enderman = mob(h, EntityType.ENDERMAN, 5, 5);
        Goal pickup = enderman.goalSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal)
                .filter(g -> g.getClass().getSimpleName().equals("EndermanTakeBlockGoal")).findFirst().orElseThrow();
        h.assertFalse(pickup.canUse(), "Block pickup disabled by default");
        try {
            FeudalConfig.ENDERMAN_BLOCK_PICKUP.set(true);
            boolean usable = false;
            for (int i = 0; i < 1000; i++) usable |= pickup.canUse();
            h.assertTrue(usable, "Enabling configuration restores vanilla pickup AI");
        } finally { FeudalConfig.ENDERMAN_BLOCK_PICKUP.set(false); }
        h.assertTrue(FeudalConfig.SPEC.getValues().valueMap().size() == 3, "Exactly three user configuration switches");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void vanillaSpawnFinalizationUsesArmorRules(GameTestHelper h) {
        floor(h);
        for (Difficulty difficulty : List.of(Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD)) {
            int armored = 0;
            RandomSource seeds = RandomSource.create(45001);
            for (int i = 0; i < 500; i++) {
                Zombie zombie = EntityType.ZOMBIE.create(h.getLevel());
                zombie.moveTo(h.absoluteVec(new Vec3(5, 2, 5))); zombie.getRandom().setSeed(seeds.nextLong());
                EventHooks.finalizeMobSpawn(zombie, h.getLevel(), new DifficultyInstance(difficulty, 10000, 0, 0), MobSpawnType.NATURAL, null);
                boolean equipped = false;
                for (EquipmentSlot slot : EquipmentSlot.values()) if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                    ItemStack armor = zombie.getItemBySlot(slot);
                    equipped |= !armor.isEmpty();
                    if (!armor.isEmpty()) h.assertFalse(armor.getItem().toString().contains("diamond") || armor.getItem().toString().contains("netherite"), "Iron armor cap");
                }
                if (equipped) armored++;
            }
            double actual = armored / 500.0;
            h.assertTrue(Math.abs(actual - MobRules.armorChance(difficulty)) < 0.08, "Per-mob armor probability for " + difficulty + ": " + actual);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void armorAppliesToUndeadVariants(GameTestHelper h) {
        floor(h);
        for (var type : List.of(EntityType.WITHER_SKELETON, EntityType.STRAY, EntityType.BOGGED, EntityType.HUSK,
                EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIFIED_PIGLIN)) {
            int armored = 0;
            RandomSource seeds = RandomSource.create(8820);
            for (int i = 0; i < 40; i++) {
                Mob entity = type.create(h.getLevel()); entity.moveTo(h.absoluteVec(new Vec3(5, 2, 5)));
                entity.getRandom().setSeed(seeds.nextLong());
                EventHooks.finalizeMobSpawn(entity, h.getLevel(), new DifficultyInstance(Difficulty.HARD, 10000, 0, 0), MobSpawnType.NATURAL, null);
                for (ItemStack stack : entity.getArmorSlots()) if (!stack.isEmpty()) { armored++; break; }
            }
            h.assertTrue(armored > 0, "Armor spawning must apply to " + type + " despite its equipment overrides");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void persistedSkeletonEquipmentIsPreserved(GameTestHelper h) {
        var skeleton = EntityType.SKELETON.create(h.getLevel());
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        MobRules.initialize(skeleton, true);
        h.assertTrue(skeleton.getMainHandItem().is(Items.IRON_SWORD), "Loading old entities must not reroll their weapons"); h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void ownerFollowTeleportsSafely(GameTestHelper h) {
        floor(h); ServerPlayer owner = h.makeMockServerPlayerInLevel(); owner.moveTo(h.absoluteVec(new Vec3(14, 2, 14)));
        Spider spider = pet(h, false, owner.getUUID());
        h.assertTrue(spider.canBeLeashed(), "Tamed spider can be leashed like a wolf");
        Goal follow = spider.goalSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal)
                .filter(g -> g.getClass().getSimpleName().equals("Follow")).findFirst().orElseThrow();
        h.assertTrue(follow.canUse(), "Spider starts following a distant owner");
        // Safe teleport candidates are random; vanilla following retries on later goal ticks.
        // A single batch can legitimately reject all ten candidates on this flat floor.
        spider.getRandom().setSeed(0);
        follow.start();
        for (int tick = 0; tick < 40 && spider.distanceToSqr(owner) >= 32; tick++) follow.tick();
        h.assertTrue(spider.distanceToSqr(owner) < 32, "Distant spider teleports safely near the owner");
        h.assertTrue(h.getLevel().getFluidState(spider.blockPosition()).isEmpty(), "Teleport avoids fluids");
        owner.getServer().getPlayerList().remove(owner); h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void spiderSinksInWater(GameTestHelper h) {
        floor(h);
        for (int x = 3; x <= 8; x++) for (int y = 2; y <= 5; y++) for (int z = 3; z <= 8; z++) h.setBlock(new BlockPos(x, y, z), Blocks.WATER);
        Spider spider = pet(h, false, UUID.randomUUID()); spider.moveTo(h.absoluteVec(new Vec3(5.5, 4, 5.5)));
        spider.setInvulnerable(true); spider.goalSelector.removeAllGoals(g -> true); spider.targetSelector.removeAllGoals(g -> true); spider.setNoAi(false);
        double start = spider.getY();
        h.runAfterDelay(10, () -> {
            h.assertTrue(spider.getY() < start && spider.getDeltaMovement().y < 0, "Spider must sink instead of swimming"); h.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void spiderSoundThreatRadius(GameTestHelper h) {
        floor(h); Spider spider = pet(h, false, UUID.randomUUID());
        h.assertFalse(SpiderCompanion.hostileNearby(spider), "Quiet with no hostile nearby");
        var creeper = mob(h, EntityType.CREEPER, 14, 4);
        h.assertTrue(SpiderCompanion.hostileNearby(spider), "Hostile within sixteen blocks enables noise");
        creeper.moveTo(spider.position().add(16.01, 0, 0));
        h.assertFalse(SpiderCompanion.hostileNearby(spider), "Hostile outside sixteen blocks does not enable noise"); h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void wildSpiderPreySelection(GameTestHelper h) {
        floor(h);
        for (boolean cave : new boolean[]{false, true}) {
            Spider spider = mob(h, cave ? EntityType.CAVE_SPIDER : EntityType.SPIDER, 5, cave ? 12 : 5);
            Mob prey = cave ? mob(h, EntityType.SILVERFISH, 7, 12) : mob(h, EntityType.BEE, 7, 5);
            boolean found = false;
            for (var wrapped : spider.targetSelector.getAvailableGoals()) {
                Goal goal = wrapped.getGoal();
                if (!goal.getClass().getSimpleName().equals("WildTarget")) continue;
                for (int i = 0; i < 100; i++) if (goal.canUse()) {
                    goal.start();
                    if (spider.getTarget() == prey) found = true;
                    spider.setTarget(null);
                }
            }
            h.assertTrue(found, "Wild spider must hunt its specified prey; cave=" + cave);
            SpiderCompanion.tame(spider, UUID.randomUUID());
            for (var wrapped : spider.targetSelector.getAvailableGoals()) if (wrapped.getGoal().getClass().getSimpleName().equals("WildTarget"))
                h.assertFalse(wrapped.getGoal().canUse(), "Tamed spiders stop hunting wild prey");
            spider.discard(); prey.discard();
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void silverfishMineshaftSpawnEntryAndLight(GameTestHelper h) {
        floor(h); BlockPos pos = h.absolutePos(new BlockPos(6, 2, 6));
        var structure = h.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE).get(ResourceLocation.withDefaultNamespace("mineshaft"));
        var room = new MineshaftPieces.MineShaftRoom(0, RandomSource.create(11), pos.getX() - 2, pos.getZ() - 2, MineshaftStructure.Type.NORMAL);
        room.move(0, pos.getY() - room.getBoundingBox().minY(), 0);
        ChunkPos chunkPos = new ChunkPos(pos); var chunk = h.getLevel().getChunkAt(pos);
        var start = new StructureStart(structure, chunkPos, 0, new PiecesContainer(List.of(room)));
        h.getLevel().structureManager().setStartForStructure(SectionPos.of(pos), structure, start, chunk);
        h.getLevel().structureManager().addReferenceForStructure(SectionPos.of(pos), structure, chunkPos.toLong(), chunk);
        h.assertTrue(FeudalEvents.inMineshaft(h.getLevel(), pos), "Fixture must be inside a real registered mineshaft piece");
        var event = new LevelEvent.PotentialSpawns(h.getLevel(), MobCategory.MONSTER, pos, WeightedRandomList.create());
        NeoForge.EVENT_BUS.post(event);
        h.assertTrue(event.getSpawnerDataList().stream().anyMatch(s -> s.type == EntityType.SILVERFISH
                && s.getWeight().asInt() == 120 && s.minCount == 4 && s.maxCount == 9), "Mineshaft silverfish spawn entry");
        for (int x = 4; x <= 8; x++) for (int y = 2; y <= 5; y++) for (int z = 4; z <= 8; z++) {
            if (x == 4 || x == 8 || z == 4 || z == 8 || y == 5) h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        }
        h.runAfterDelay(10, () -> {
            h.assertTrue(SpawnPlacements.checkSpawnRules(EntityType.SILVERFISH, h.getLevel(), MobSpawnType.NATURAL, pos, RandomSource.create(19)), "Dark mineshaft supports natural silverfish spawning");
            h.setBlock(new BlockPos(6, 2, 7), Blocks.TORCH);
        });
        h.runAfterDelay(20, () -> {
            h.assertFalse(SpawnPlacements.checkSpawnRules(EntityType.SILVERFISH, h.getLevel(), MobSpawnType.NATURAL, pos, RandomSource.create(19)), "Lit mineshaft obeys hostile mob light restrictions"); h.succeed();
        });
    }
}
