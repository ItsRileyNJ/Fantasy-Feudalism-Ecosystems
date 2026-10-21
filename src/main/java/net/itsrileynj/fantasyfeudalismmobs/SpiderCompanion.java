package net.itsrileynj.fantasyfeudalismmobs;

import java.util.UUID;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.common.Tags;

/** Persistent state on the vanilla spider, so existing entity IDs, rendering and saves remain valid. */
public final class SpiderCompanion {
    public static final long HARVEST_COOLDOWN = 48_000L;
    private static final String ROOT = "fantasy_feudalism_mobs";
    public static final String SILK_READY = "silkReadyAt";
    public static final String POISON_READY = "poisonReadyAt";

    public static CompoundTag data(Entity spider) {
        CompoundTag persistent = spider.getPersistentData();
        if (!persistent.contains(ROOT)) persistent.put(ROOT, new CompoundTag());
        return persistent.getCompound(ROOT);
    }

    public static boolean isTamed(Entity entity) {
        return entity instanceof Spider && ((SpiderState)entity).ffm$owner().isPresent();
    }

    public static boolean isSitting(Spider spider) { return ((SpiderState)spider).ffm$sitting(); }
    public static void sync(Spider spider) {
        CompoundTag tag = data(spider);
        ((SpiderState)spider).ffm$setState(tag.hasUUID("owner") ? Optional.of(tag.getUUID("owner")) : Optional.empty(), tag.getBoolean("sitting"));
        CompanionIndex.track(spider);
    }
    public static void setSitting(Spider spider, boolean sitting) {
        data(spider).putBoolean("sitting", sitting);
        sync(spider);
        spider.getNavigation().stop();
        spider.setTarget(null);
    }

    @Nullable
    public static Player owner(Spider spider) {
        return ((SpiderState)spider).ffm$owner().map(id -> spider.level().getPlayerByUUID(id)).orElse(null);
    }

    public static boolean ownedBy(Spider spider, Player player) {
        return ((SpiderState)spider).ffm$owner().filter(player.getUUID()::equals).isPresent();
    }

    public static void tame(Spider spider, UUID owner) {
        data(spider).putUUID("owner", owner);
        sync(spider);
        applyTameAttributes(spider);
        spider.setHealth(spider.getMaxHealth());
        spider.setPersistenceRequired();
        setSitting(spider, true); // Same initial order as a newly tamed vanilla wolf.
    }

    public static void applyTameAttributes(Spider spider) {
        spider.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30.0); // 75% of a 1.21.1 tamed wolf's 40.
        spider.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.36); // Wolf's 0.3 * 1.2.
        for (PathType type : new PathType[]{PathType.WATER, PathType.WATER_BORDER, PathType.LAVA,
                PathType.DAMAGE_FIRE, PathType.DANGER_FIRE}) spider.setPathfindingMalus(type, -1.0F);
    }

    public static boolean healingFood(ItemStack stack) {
        return stack.is(Tags.Items.FOODS_RAW_MEAT) || stack.is(Tags.Items.FOODS_RAW_FISH)
                || stack.is(Items.ROTTEN_FLESH);
    }

    public static void hearts(Spider spider) {
        if (spider.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART, spider.getX(), spider.getY() + 0.5, spider.getZ(),
                    7, spider.getBbWidth() * 0.25, 0.3, spider.getBbWidth() * 0.25, 0.02);
        }
    }

    public static InteractionResult interact(Player player, InteractionHand hand, Spider spider) {
        if (!spider.isAlive() || spider.isRemoved() || player.isSpectator()) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        boolean beeBottle = held.is(FantasyFeudalismMobs.BEE_BOTTLE);
        if (!isTamed(spider)) {
            if (!beeBottle) return InteractionResult.PASS;
            if (!spider.level().isClientSide) {
                giveFilledBottle(player, hand, held, new ItemStack(Items.GLASS_BOTTLE));
                if (spider.getRandom().nextInt(100) < 33) {
                    tame(spider, player.getUUID());
                    hearts(spider);
                }
            }
            return InteractionResult.sidedSuccess(spider.level().isClientSide);
        }
        if (held.is(Items.GLASS_BOTTLE) || held.is(Items.SHEARS)) {
            if (!spider.level().isClientSide) harvest(player, hand, spider, held.is(Items.SHEARS));
            return InteractionResult.sidedSuccess(spider.level().isClientSide);
        }
        if (beeBottle || (healingFood(held) && spider.getHealth() < spider.getMaxHealth())) {
            if (!spider.level().isClientSide) {
                if (beeBottle) giveFilledBottle(player, hand, held, new ItemStack(Items.GLASS_BOTTLE));
                else if (spider.getHealth() < spider.getMaxHealth()) {
                    var food = held.getFoodProperties(spider);
                    spider.heal(2.0F * (food == null ? 1 : food.nutrition())); // Vanilla wolf healing.
                    held.consume(1, player);
                }
                hearts(spider);
            }
            return InteractionResult.sidedSuccess(spider.level().isClientSide);
        }
        if (ownedBy(spider, player)) {
            if (!spider.level().isClientSide) setSitting(spider, !isSitting(spider));
            return InteractionResult.sidedSuccess(spider.level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    public static boolean harvest(Player player, InteractionHand hand, Spider spider, boolean silk) {
        if (spider.level().isClientSide || !spider.isAlive() || spider.isRemoved() || player.isSpectator() || !isTamed(spider)) return false;
        String key = silk ? SILK_READY : POISON_READY;
        long now = spider.level().getGameTime();
        if (data(spider).getLong(key) > now) return false;
        ItemStack held = player.getItemInHand(hand);
        if (silk) {
            if (!held.is(Items.SHEARS)) return false;
            ItemStack result = new ItemStack(Items.STRING, 1 + spider.getRandom().nextInt(4));
            if (!player.getInventory().add(result)) player.drop(result, false);
            held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        } else {
            if (!held.is(Items.GLASS_BOTTLE)) return false;
            ItemStack poison = PotionContents.createItemStack(Items.POTION, Potions.POISON);
            giveFilledBottle(player, hand, held, poison);
        }
        data(spider).putLong(key, now + HARVEST_COOLDOWN);
        return true;
    }

    public static void giveFilledBottle(Player player, InteractionHand hand, ItemStack input, ItemStack output) {
        player.setItemInHand(hand, ItemUtils.createFilledResult(input, player, output));
    }

    public static boolean protectedByFollower(Player player) {
        // An unsat, loaded companion in the owner's level is following under vanilla wolf rules,
        // including one about to teleport back to the owner.
        return CompanionIndex.protects(player);
    }

    public static boolean effectExcluded(LivingEntity target) {
        return target instanceof AbstractSkeleton || target instanceof Spider || target instanceof Zombie;
    }

    public static boolean hostileNearby(Spider spider) {
        return !spider.level().getEntitiesOfClass(LivingEntity.class, spider.getBoundingBox().inflate(16),
                e -> e != spider && e instanceof Enemy && !isTamed(e) && e.isAlive() && !e.isAlliedTo(spider)
                        && e.distanceToSqr(spider) <= 256).isEmpty();
    }

    private SpiderCompanion() {}
}
