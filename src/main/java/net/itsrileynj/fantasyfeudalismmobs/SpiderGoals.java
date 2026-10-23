package net.itsrileynj.fantasyfeudalismmobs;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public final class SpiderGoals {
    // Discrete vanilla gravity/drag: peaks just above a three-block landing, below four blocks.
    public static final float JUMP_POWER = 0.68745F;

    public static void install(Spider spider, GoalSelector goals, GoalSelector targets) {
        goals.removeAllGoals(g -> g instanceof FloatGoal || g instanceof LeapAtTargetGoal || g instanceof WaterAvoidingRandomStrollGoal
                || g.getClass().getName().endsWith("Spider$SpiderAttackGoal"));
        targets.removeAllGoals(g -> true);
        spider.getNavigation().setCanFloat(false);
        goals.addGoal(0, new Sit(spider));
        goals.addGoal(3, new ForwardLeap(spider));
        goals.addGoal(4, new Attack(spider));
        goals.addGoal(5, new Follow(spider));
        goals.addGoal(7, new WaterAvoidingRandomStrollGoal(spider, 0.8));
        targets.addGoal(1, new DefendOwner(spider, true));
        targets.addGoal(2, new DefendOwner(spider, false));
        targets.addGoal(3, new HurtByTargetGoal(spider));
        targets.addGoal(4, new WildTarget<>(spider, Player.class, true));
        targets.addGoal(5, new WildTarget<>(spider, IronGolem.class, true));
        if (spider instanceof CaveSpider) targets.addGoal(6, new WildTarget<>(spider, Silverfish.class, false));
        else targets.addGoal(6, new WildTarget<>(spider, Bee.class, false));
    }

    /** A low forward pounce on level ground; retain height when clearing a ledge. */
    private static class ForwardLeap extends LeapAtTargetGoal {
        private final Spider spider;
        ForwardLeap(Spider spider) { super(spider, JUMP_POWER); this.spider = spider; }
        private boolean inRange() {
            LivingEntity target = spider.getTarget();
            if (target == null) return false;
            double distance = spider.distanceToSqr(target);
            return distance >= 2.75 * 2.75 && distance <= 3.25 * 3.25;
        }
        @Override public boolean canUse() {
            return inRange() && mayAttack(spider, spider.getTarget()) && super.canUse();
        }
        @Override public void start() {
            LivingEntity target = spider.getTarget();
            if (!inRange() || !mayAttack(spider, target)) return;
            Vec3 direction = new Vec3(target.getX() - spider.getX(), 0, target.getZ() - spider.getZ());
            if (direction.lengthSqr() < 1.0E-7) return;
            direction = direction.normalize();
            boolean elevated = target.getY() > spider.getY() + 0.5
                    || !spider.level().noCollision(spider, spider.getBoundingBox().move(direction.scale(0.8)));
            spider.getNavigation().stop();
            spider.setDeltaMovement(direction.scale(elevated ? 0.4 : 0.82).add(0, elevated ? JUMP_POWER : 0.32, 0));
            spider.hasImpulse = true;
        }
    }

    public static boolean mayAttack(Spider spider, LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        if (!SpiderCompanion.isTamed(spider)) return !(target instanceof Player player && SpiderCompanion.protectedByFollower(player));
        if (SpiderCompanion.isSitting(spider)) return false;
        Player owner = SpiderCompanion.owner(spider);
        if (target == owner || spider.isAlliedTo(target) || target.isAlliedTo(spider)) return false;
        if (target instanceof Spider other && SpiderCompanion.isTamed(other)
                && SpiderCompanion.data(other).getUUID("owner").equals(SpiderCompanion.data(spider).getUUID("owner"))) return false;
        if (target instanceof TamableAnimal pet && pet.isTame()
                && SpiderCompanion.data(spider).getUUID("owner").equals(pet.getOwnerUUID())) return false;
        return !(target instanceof Player player
                && (!spider.level().getServer().isPvpAllowed() || owner != null && !owner.canHarmPlayer(player)));
    }

    private static class WildTarget<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
        private final Spider spider;
        private final boolean needsDark;
        WildTarget(Spider spider, Class<T> type, boolean needsDark) {
            super(spider, type, 10, true, false, target -> mayAttack(spider, target));
            this.spider = spider;
            this.needsDark = needsDark;
        }
        @Override public boolean canUse() {
            return !SpiderCompanion.isTamed(spider) && (!needsDark || spider.getLightLevelDependentMagicValue() < 0.5F) && super.canUse();
        }
        @Override public boolean canContinueToUse() { return !SpiderCompanion.isTamed(spider) && super.canContinueToUse(); }
    }

    private static class Attack extends MeleeAttackGoal {
        private final Spider spider;
        Attack(Spider spider) { super(spider, 1.0, true); this.spider = spider; }
        @Override public boolean canUse() {
            return !spider.isVehicle() && mayAttack(spider, spider.getTarget()) && super.canUse();
        }
        @Override public boolean canContinueToUse() {
            if (!mayAttack(spider, spider.getTarget())) return false;
            if (!SpiderCompanion.isTamed(spider) && spider.getLightLevelDependentMagicValue() >= 0.5F && spider.getRandom().nextInt(100) == 0) {
                spider.setTarget(null);
                return false;
            }
            return super.canContinueToUse();
        }
    }

    private static class Sit extends Goal {
        private final Spider spider;
        Sit(Spider spider) { this.spider = spider; setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP)); }
        @Override public boolean canUse() { return SpiderCompanion.isTamed(spider) && SpiderCompanion.isSitting(spider); }
        @Override public void tick() { spider.getNavigation().stop(); spider.setJumping(false); }
    }

    private static class DefendOwner extends TargetGoal {
        private final Spider spider;
        private final boolean defend;
        private LivingEntity target;
        private int timestamp;
        DefendOwner(Spider spider, boolean defend) {
            super(spider, false); this.spider = spider; this.defend = defend;
            setFlags(EnumSet.of(Flag.TARGET));
        }
        @Override public boolean canUse() {
            Player owner = SpiderCompanion.owner(spider);
            if (owner == null || SpiderCompanion.isSitting(spider)) return false;
            target = defend ? owner.getLastHurtByMob() : owner.getLastHurtMob();
            int time = defend ? owner.getLastHurtByMobTimestamp() : owner.getLastHurtMobTimestamp();
            return time != timestamp && mayAttack(spider, target) && canAttack(target, TargetingConditions.DEFAULT);
        }
        @Override public void start() {
            spider.setTarget(target);
            Player owner = SpiderCompanion.owner(spider);
            if (owner != null) timestamp = defend ? owner.getLastHurtByMobTimestamp() : owner.getLastHurtMobTimestamp();
            super.start();
        }
    }

    /** Wolf follow thresholds and safe-teleport search, keeping spiders' forbidden terrain. */
    private static class Follow extends Goal {
        private final Spider spider;
        private Player owner;
        private int recalc;
        Follow(Spider spider) { this.spider = spider; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        private boolean available() {
            owner = SpiderCompanion.owner(spider);
            return owner != null && !owner.isSpectator() && !SpiderCompanion.isSitting(spider)
                    && !spider.isPassenger() && !spider.isLeashed() && spider.getTarget() == null;
        }
        @Override public boolean canUse() { return available() && spider.distanceToSqr(owner) >= 100; }
        @Override public boolean canContinueToUse() { return available() && spider.distanceToSqr(owner) > 4; }
        @Override public void start() { recalc = 0; }
        @Override public void stop() { spider.getNavigation().stop(); }
        @Override public void tick() {
            spider.getLookControl().setLookAt(owner, 10, spider.getMaxHeadXRot());
            if (--recalc > 0) return;
            recalc = adjustedTickDelay(10);
            if (spider.distanceToSqr(owner) < 144) { spider.getNavigation().moveTo(owner, 1.0); return; }
            for (int attempt = 0; attempt < 10; attempt++) {
                int x = spider.getRandom().nextIntBetweenInclusive(-3, 3);
                int z = spider.getRandom().nextIntBetweenInclusive(-3, 3);
                if (Math.abs(x) < 2 && Math.abs(z) < 2) continue;
                BlockPos pos = owner.blockPosition().offset(x, spider.getRandom().nextIntBetweenInclusive(-1, 1), z);
                if (!spider.level().hasChunkAt(pos)) continue;
                PathType type = WalkNodeEvaluator.getPathTypeStatic(spider, pos);
                if (type != PathType.WALKABLE || !spider.level().getFluidState(pos).isEmpty()) continue;
                Vec3 delta = Vec3.atBottomCenterOf(pos).subtract(spider.position());
                if (spider.level().noCollision(spider, spider.getBoundingBox().move(delta))) {
                    spider.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, spider.getYRot(), spider.getXRot());
                    spider.getNavigation().stop();
                    return;
                }
            }
        }
    }
    private SpiderGoals() {}
}
