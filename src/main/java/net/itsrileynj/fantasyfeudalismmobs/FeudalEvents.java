package net.itsrileynj.fantasyfeudalismmobs;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class FeudalEvents {
    private record SpawnEntry(EntityType<?> type, int weight, int min, int max) {}
    // NaturalSpawner rechecks list membership by identity. Never recreate a selected entry.
    // Keys contain registry objects and numbers, never worlds or chunks.
    private static final Map<SpawnEntry, MobSpawnSettings.SpawnerData> SPAWN_ENTRIES = new ConcurrentHashMap<>();

    private static MobSpawnSettings.SpawnerData spawnEntry(EntityType<?> type, int weight, int min, int max) {
        return SPAWN_ENTRIES.computeIfAbsent(new SpawnEntry(type, weight, min, max),
                key -> new MobSpawnSettings.SpawnerData(key.type(), key.weight(), key.min(), key.max()));
    }

    public static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(EntityType.SILVERFISH, (type, level, reason, pos, random) ->
                reason == MobSpawnType.NATURAL && inMineshaft(level.getLevel(), pos)
                        ? Monster.checkMonsterSpawnRules(type, level, reason, pos, random)
                        : Silverfish.checkSilverfishSpawnRules(type, level, reason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (event.getEntity() instanceof Mob mob) {
            MobRules.initialize(mob, event.loadedFromDisk());
            GuardDogs.install(mob);
        }
        if (event.getEntity() instanceof Silverfish fish) SilverfishBurrowing.restore(fish);
        if (event.getEntity() instanceof Wolf wolf) WolfPacks.initialize(wolf);
        if (event.getEntity() instanceof Horse horse) {
            HorseLife.initialize(horse);
            horse.setEating(false); // An interrupted saved grazing goal must not leave a horse immobile.
        }
        if (event.getEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof AbstractSkeleton skeleton
                && !event.loadedFromDisk() && !arrow.getPersistentData().getBoolean("ffm_faster")) {
            SkeletonBallistics.accelerate(arrow, skeleton);
            arrow.getPersistentData().putBoolean("ffm_faster", true);
            if (arrow instanceof Arrow tipped && arrow.getWeaponItem() != null && arrow.getWeaponItem().getItem() instanceof CrossbowItem) {
                // CrossbowItem bypasses AbstractSkeleton#getArrow; retain the variants' vanilla effects.
                if (arrow.getOwner() instanceof Bogged) tipped.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
                else if (arrow.getOwner() instanceof Stray) tipped.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600));
            }
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Spider spider && event.getLevel() instanceof ServerLevel level)
            CompanionIndex.forget(spider, level);
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.isSpectator() || !event.getTarget().isAlive() || event.getTarget().isRemoved()) return;
        if (event.getTarget() instanceof Bee bee && player.getItemInHand(event.getHand()).is(Items.GLASS_BOTTLE)) {
            if (!event.getLevel().isClientSide) {
                SpiderCompanion.giveFilledBottle(player, event.getHand(), player.getItemInHand(event.getHand()),
                        new ItemStack(FantasyFeudalismMobs.BEE_BOTTLE.get()));
                bee.discard();
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        } else if (event.getTarget() instanceof Spider spider) {
            InteractionResult result = SpiderCompanion.interact(player, event.getHand(), spider);
            if (result != InteractionResult.PASS) { event.setCanceled(true); event.setCancellationResult(result); }
        }
    }

    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        if(event.getSource().getDirectEntity() instanceof Mob hunter && GuardDogs.blocks(hunter,event.getEntity())) {
            event.setCanceled(true);return;
        }
        if (event.getEntity() instanceof Spider spider && SpiderCompanion.isTamed(spider)
                && event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * 1.25F);
        if (event.getEntity() instanceof Zombie && event.getSource().getEntity() instanceof Silverfish)
            event.setAmount(event.getAmount() * 2.0F);
        if (event.getEntity() instanceof Player && event.getSource().getEntity() instanceof EnderMan)
            event.setAmount(event.getAmount() * 1.35F);
        if (event.getSource().getEntity() instanceof Phantom && event.getEntity() instanceof Player player && player.getHealth() > 8)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void hurtCompanion(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0 && event.getEntity() instanceof Spider spider && SpiderCompanion.isTamed(spider)
                && SpiderCompanion.isSitting(spider)) {
            SpiderCompanion.setSitting(spider, false);
        }
    }

    @SubscribeEvent
    public static void spiderFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Spider spider) {
            double distance = ((SpiderState)spider).ffm$landingDistance();
            if (!Double.isFinite(distance)) distance = event.getDistance();
            // The accumulator is a float; tolerate rounding at an exact nine-block drop.
            if (distance < 9.0 - 1.0E-5) event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void wildSpiderEatsBee(LivingDeathEvent event) {
        if (event.getEntity() instanceof Bee && event.getSource().getDirectEntity() instanceof Spider spider
                && !spider.level().isClientSide && spider.isAlive() && !SpiderCompanion.isTamed(spider)) {
            spider.setHealth(spider.getMaxHealth());
        }
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void silverfishKillsZombie(LivingDeathEvent event) {
        if (event.getEntity() instanceof Zombie && event.getSource().getEntity() instanceof Silverfish fish
                && !fish.level().isClientSide) SilverfishBurrowing.creditKill(fish);
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void wolfMeal(LivingDeathEvent event) {
        if(!event.getEntity().level().isClientSide && event.getSource().getEntity() instanceof Fox fox
                && GuardDogs.foxFood(event.getEntity()))GuardDogs.feedFox(fox);
        if(event.getEntity() instanceof Sheep && event.getSource().getEntity() instanceof Wolf wolf
                && !wolf.level().isClientSide && !wolf.isTame()) WolfPacks.ateSheep(wolf);
    }

    @SubscribeEvent
    public static void targeting(LivingChangeTargetEvent event) {
        LivingEntity proposed = event.getNewAboutToBeSetTarget();
        if(event.getEntity() instanceof Mob hunter && GuardDogs.blocks(hunter,proposed)) {
            event.setNewAboutToBeSetTarget(null);return;
        }
        if(event.getEntity() instanceof Wolf wolf && !wolf.isTame() && WolfPacks.food(proposed)
                && !WolfPacks.hungry(wolf) && wolf.getLastHurtByMob()!=proposed) {
            event.setNewAboutToBeSetTarget(null);return;
        }
        if (event.getEntity() instanceof Silverfish fish && proposed != null && SilverfishBurrowing.hasKill(fish)) {
            event.setNewAboutToBeSetTarget(null);
            return;
        }
        if (event.getEntity() instanceof AbstractSkeleton && proposed instanceof Player player
                && (player.isCreative() || player.isSpectator())) {
            event.setNewAboutToBeSetTarget(null);
            return;
        }
        if (event.getEntity() instanceof Spider spider && proposed != null && !SpiderGoals.mayAttack(spider, proposed)) {
            event.setCanceled(true); return;
        }
        if (event.getEntity() instanceof Phantom && proposed instanceof Player player && player.getHealth() > 8) {
            event.setCanceled(true); return;
        }
        if (event.getEntity() instanceof AbstractSkeleton skeleton && skeleton.isHolding(s -> s.getItem() instanceof ProjectileWeaponItem)) {
            LivingEntity current = skeleton.getTarget();
            if (proposed != null && current != null && current.canBeSeenAsEnemy() && current != proposed
                    && !(current instanceof Player p && (p.isCreative() || p.isSpectator()))) {
                Player nearby = skeleton.level().getNearestPlayer(skeleton, 5.0);
                if (nearby == null) event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void beforeTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (entity instanceof Silverfish fish && SilverfishBurrowing.isBurrowed(fish)) {
            SilverfishBurrowing.tick(fish);
            event.setCanceled(true);
            return;
        }
        if (entity instanceof Wolf wolf) { WolfPacks.beforeTick(wolf);GuardDogs.beforeTick(wolf); }
        if (entity instanceof Fox fox) GuardDogs.beforeTick(fox);
        if (entity instanceof Zombie zombie) MobRules.updateZombie(zombie);
        if (entity instanceof AbstractSkeleton skeleton && skeleton.getTarget() instanceof Player player
                && (player.isCreative() || player.isSpectator())) {
            skeleton.setTarget(null);
            skeleton.stopUsingItem();
            skeleton.setAggressive(false);
            skeleton.getNavigation().stop();
        }
        if (entity instanceof Blaze blaze) MobRules.dash(blaze);
        if (entity instanceof Phantom phantom && phantom.getTarget() instanceof Player player && player.getHealth() > 8) phantom.setTarget(null);
        if (entity instanceof Spider spider) {
            if (spider.getTarget() != null && !SpiderGoals.mayAttack(spider, spider.getTarget())) spider.setTarget(null);
            if (spider.onGround() && !spider.isInWater() && !SpiderCompanion.isSitting(spider)) {
                var path = spider.getNavigation().getPath();
                if (path != null && !path.isDone() && path.getNextNode().y - spider.getY() > spider.maxUpStep()) spider.getJumpControl().jump();
            }
        }
    }

    @SubscribeEvent
    public static void afterTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Spider spider && !spider.level().isClientSide && spider.isInWater()) {
            var velocity = spider.getDeltaMovement();
            spider.setDeltaMovement(velocity.x, Math.min(velocity.y, -0.04), velocity.z);
        }
    }

    @SubscribeEvent
    public static void explosion(ExplosionEvent.Detonate event) {
        if (event.getExplosion().getDirectSourceEntity() instanceof Creeper) {
            event.getAffectedBlocks().clear();
            if (!event.getLevel().isClientSide) CreeperVegetation.grow(event.getLevel(), event.getExplosion().center(), event.getExplosion().radius());
        }
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        LivingEntity mob = event.getEntity();
        if (!mob.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;
        if (mob instanceof Horse horse && !horse.isBaby())
            event.getDrops().add(new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(),
                    new ItemStack(FantasyFeudalismMobs.RAW_HORSE.get(), 1 + mob.getRandom().nextInt(2))));
        if (mob instanceof Creeper) event.getDrops().add(new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(),
                new ItemStack(Items.MOSS_BLOCK, 1 + mob.getRandom().nextInt(2))));
        if (mob instanceof Ghast) {
            event.getDrops().removeIf(item -> item.getItem().is(Items.GHAST_TEAR));
            event.getDrops().add(new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), new ItemStack(Items.GHAST_TEAR, 2)));
        }
    }

    @SubscribeEvent
    public static void spawns(LevelEvent.PotentialSpawns event) {
        if (event.getMobCategory() != MobCategory.MONSTER) return;
        for (var original : new ArrayList<>(event.getSpawnerDataList())) {
            int weight = original.getWeight().asInt();
            int min = original.minCount, max = original.maxCount;
            if (original.type == EntityType.SPIDER || original.type == EntityType.CAVE_SPIDER) weight = Math.max(1, Math.round(weight * 0.5F));
            else if (original.type == EntityType.CREEPER) weight = Math.max(1, Math.round(weight * 0.4F));
            else if (original.type == EntityType.GHAST) { weight = Math.max(1, Math.round(weight * 0.8F)); min = max = 3; }
            else if (isZombie(original.type)) { min = 4; max = 9; }
            else continue;
            event.removeSpawnerData(original);
            event.addSpawnerData(spawnEntry(original.type, weight, min, max));
        }
        if (event.getLevel() instanceof ServerLevel level && inMineshaft(level, event.getPos())
                && event.getSpawnerDataList().stream().noneMatch(s -> s.type == EntityType.SILVERFISH)) {
            event.addSpawnerData(spawnEntry(EntityType.SILVERFISH, 120, 4, 9));
        }
    }

    public static boolean inMineshaft(ServerLevel level, BlockPos pos) {
        return level.structureManager().getStructureWithPieceAt(pos, StructureTags.MINESHAFT).isValid();
    }

    private static boolean isZombie(EntityType<?> type) {
        return type == EntityType.ZOMBIE || type == EntityType.HUSK || type == EntityType.DROWNED
                || type == EntityType.ZOMBIE_VILLAGER || type == EntityType.ZOMBIFIED_PIGLIN;
    }
    private FeudalEvents() {}
}
