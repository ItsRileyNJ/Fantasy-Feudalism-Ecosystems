package net.itsrileynj.fantasyfeudalismmobs;

import java.util.List;
import net.itsrileynj.fantasyfeudalismmobs.mixin.EndermanAccessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.phys.Vec3;

public final class MobRules {
    private static final ResourceLocation SPEED_VARIATION = id("zombie_speed");
    private static final ResourceLocation DAMAGE_VARIATION = id("zombie_damage");
    private static final ResourceLocation LOW_HEALTH = id("zombie_low_health");

    public static void initialize(Mob mob, boolean loadedFromDisk) {
        if (mob instanceof Spider spider) {
            SpiderCompanion.sync(spider);
            spider.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(SpiderGoals.JUMP_POWER);
            if (SpiderCompanion.isTamed(spider)) SpiderCompanion.applyTameAttributes(spider);
        }
        if (mob instanceof AbstractSkeleton) {
            mob.getAttribute(Attributes.FOLLOW_RANGE).removeModifier(ResourceLocation.withDefaultNamespace("random_spawn_bonus"));
            mob.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(35);
        }
        if (mob.getPersistentData().getBoolean("ffm_initialized")) return;
        mob.getPersistentData().putBoolean("ffm_initialized", true);
        if (loadedFromDisk) return; // Spawn-only rolls must not replace an existing world's equipment on load.
        if (mob instanceof Zombie) {
            variation(mob, Attributes.MOVEMENT_SPEED, SPEED_VARIATION);
            variation(mob, Attributes.ATTACK_DAMAGE, DAMAGE_VARIATION);
        }
        if (mob instanceof AbstractSkeleton skeleton) {
            int roll = mob.getRandom().nextInt(100);
            Item weapon = roll < 15 ? Items.CROSSBOW : roll < 20
                    ? (mob.getRandom().nextInt(100) < 80 ? Items.STONE_SWORD : Items.IRON_SWORD) : Items.BOW;
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
        }
        if (mob instanceof Phantom && mob.level().getDifficulty() == Difficulty.HARD) {
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 0));
        }
    }

    private static void variation(Mob mob, net.minecraft.core.Holder<Attribute> attribute, ResourceLocation id) {
        var instance = mob.getAttribute(attribute);
        if (instance != null && !instance.hasModifier(id)) instance.addPermanentModifier(
                new AttributeModifier(id, mob.getRandom().nextDouble() * 0.4 - 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    public static void updateZombie(Zombie zombie) {
        var speed = zombie.getAttribute(Attributes.MOVEMENT_SPEED);
        if (zombie.getHealth() < 6.0F) {
            if (!speed.hasModifier(LOW_HEALTH)) speed.addTransientModifier(
                    new AttributeModifier(LOW_HEALTH, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else speed.removeModifier(LOW_HEALTH);
    }

    public static boolean armorEligible(Mob mob) { return mob instanceof Zombie || mob instanceof AbstractSkeleton; }

    public static float armorChance(Difficulty difficulty) {
        return switch (difficulty) { case EASY -> 0.20F; case NORMAL -> 0.35F; case HARD -> 0.60F; default -> 0; };
    }

    public static int armorMaterial(int percentile) {
        return percentile < 50 ? 0 : percentile < 75 ? 2 : percentile < 95 ? 3 : 1;
    }

    public static void equipArmor(Mob mob, RandomSource random, DifficultyInstance difficulty) {
        if (mob.getPersistentData().getBoolean("ffm_armor_rolled")) return;
        mob.getPersistentData().putBoolean("ffm_armor_rolled", true);
        if (random.nextFloat() >= armorChance(difficulty.getDifficulty())) return;
        int material = armorMaterial(random.nextInt(100));
        boolean first = true;
        // Preserve vanilla armor-slot selection after the single per-mob eligibility roll.
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            if (!first && random.nextFloat() < (difficulty.getDifficulty() == Difficulty.HARD ? 0.1F : 0.25F)) break;
            first = false;
            Item item = Mob.getEquipmentForSlot(slot, material);
            if (item != null && mob.getItemBySlot(slot).isEmpty()) mob.setItemSlot(slot, new ItemStack(item));
        }
    }

    public static void finalizeArmor(Mob mob, RandomSource random, DifficultyInstance difficulty) {
        if (!armorEligible(mob)) return;
        equipArmor(mob, random, difficulty);
        for (EquipmentSlot slot : EquipmentSlot.values()) if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR)
            enchantArmor(mob, random, slot);
    }

    public static void enchantArmor(Mob mob, RandomSource random, EquipmentSlot slot) {
        ItemStack stack = mob.getItemBySlot(slot);
        if (stack.isEmpty()) return;
        int roll = random.nextInt(100);
        int level = roll < 35 ? 1 : roll < 50 ? 2 : 0;
        if (level == 0) return;
        var registry = mob.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        List<EnchantmentInstance> candidates = registry.holders()
                .filter(h -> h.is(EnchantmentTags.IN_ENCHANTING_TABLE) && stack.isPrimaryItemFor(h) && h.value().getMaxLevel() >= level)
                .map(h -> new EnchantmentInstance(h, level)).toList();
        WeightedRandom.getRandomItem(random, candidates).ifPresent(e -> stack.enchant(e.enchantment, level));
    }

    public static void afterHit(Mob attacker, Entity victim) {
        if (attacker instanceof Spider spider && SpiderCompanion.isTamed(spider)
                && victim instanceof LivingEntity target && target instanceof Enemy && !target.isAlliedTo(spider)
                && !SpiderCompanion.effectExcluded(target)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0), spider);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0), spider);
            if (spider instanceof CaveSpider) target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0), spider);
        }
        if (attacker instanceof EnderMan enderman && victim instanceof Player player) {
            enderman.stopBeingAngry();
            enderman.setTarget(null);
            enderman.setLastHurtByMob(null);
            Vec3 start = enderman.position();
            for (int i = 0; i < 64; i++) {
                double x = start.x + (enderman.getRandom().nextDouble() - 0.5) * 64;
                double y = start.y + enderman.getRandom().nextInt(64) - 32;
                double z = start.z + (enderman.getRandom().nextDouble() - 0.5) * 64;
                if (player.distanceToSqr(x, y, z) <= player.distanceToSqr(start)) continue;
                if (((EndermanAccessor)enderman).ffm$teleport(x, y, z)) break;
            }
        }
    }

    public static void dash(Blaze blaze) {
        tryDash(blaze, blaze.level().getGameTime());
    }

    public static void tryDash(Blaze blaze, long now) {
        var data = blaze.getPersistentData();
        if (!FeudalConfig.BLAZE_DASHING.get() || blaze.getTarget() == null
                || data.getLong("ffm_dash_ready") > now || now % 20 != 0 || blaze.getRandom().nextFloat() >= 0.2F) return;
        Vec3 toward = blaze.getTarget().position().subtract(blaze.position());
        Vec3 side = new Vec3(-toward.z, 0, toward.x).normalize();
        if (data.getBoolean("ffm_dash_left")) side = side.scale(-1);
        data.putBoolean("ffm_dash_left", !data.getBoolean("ffm_dash_left"));
        data.putDouble("ffm_dash_x", side.x * 0.4);
        data.putDouble("ffm_dash_z", side.z * 0.4);
        data.putInt("ffm_dash_ticks", 10);
        data.putLong("ffm_dash_ready", now + 100);
    }

    public static boolean dashMovement(Blaze blaze) {
        var data = blaze.getPersistentData();
        int ticks = data.getInt("ffm_dash_ticks");
        if (!FeudalConfig.BLAZE_DASHING.get()) { data.putInt("ffm_dash_ticks", 0); return false; }
        if (ticks <= 0) return false;
        Vec3 velocity = new Vec3(data.getDouble("ffm_dash_x"), 0, data.getDouble("ffm_dash_z"));
        if (blaze.level().noCollision(blaze, blaze.getBoundingBox().move(velocity))) {
            blaze.move(MoverType.SELF, velocity);
            blaze.hasImpulse = true;
        } else data.putInt("ffm_dash_ticks", 1);
        data.putInt("ffm_dash_ticks", data.getInt("ffm_dash_ticks") - 1);
        blaze.setDeltaMovement(0, blaze.getDeltaMovement().y, 0);
        return true;
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID, path); }
    private MobRules() {}
}
