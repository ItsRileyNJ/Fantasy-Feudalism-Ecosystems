package net.itsrileynj.fantasyfeudalismmobs.rootworm.entity;

import net.itsrileynj.fantasyfeudalismmobs.rootworm.RootWormModule;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.RootWormConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/** A neutral creature with no biome or custom-block requirements. Feeding is purely cosmetic and never changes a block. */
public final class RootWorm extends PathfinderMob {
    private static final EntityDataAccessor<Integer> BITE_TICKS = SynchedEntityData.defineId(RootWorm.class, EntityDataSerializers.INT);
    private static final int BITE_DURATION = 9;
    private static final EntityDataAccessor<Boolean> SEARCHING = SynchedEntityData.defineId(RootWorm.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SEARCH_BLEND_START = SynchedEntityData.defineId(RootWorm.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> SEARCH_CHANGED_AT = SynchedEntityData.defineId(RootWorm.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> SEARCH_STARTED_AT = SynchedEntityData.defineId(RootWorm.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> FEEDING = SynchedEntityData.defineId(RootWorm.class, EntityDataSerializers.BOOLEAN);

    private float mouthOpen, previousMouthOpen;
    private final net.minecraft.world.level.gameevent.DynamicGameEventListener<RootWormHearing> hearing;
    private java.util.UUID heardActor;
    private Vec3 heardPosition;
    private int heardAt = -100000;
    private final RootWormPart[] bodyParts;
    private final Vec3[] previousPartPositions;

    public RootWorm(EntityType<? extends RootWorm> type, Level level) {
        super(type, level);
        this.xpReward = 2;
        hearing = new net.minecraft.world.level.gameevent.DynamicGameEventListener<>(new RootWormHearing(this));
        setPathfindingMalus(PathType.DAMAGE_OTHER, -1.0F);
        bodyParts = new RootWormPart[RootWormShape.SEGMENTS.size()];
        previousPartPositions = new Vec3[bodyParts.length];
        for (int i = 0; i < bodyParts.length; i++) bodyParts[i] = new RootWormPart(this, i);
        // Reserve consecutive IDs, as NeoForge does for the dragon, so spawn packets resolve every part.
        setId(ENTITY_COUNTER.getAndAdd(bodyParts.length + 1) + 1);
        positionBodyParts();
    }

    @Override public boolean isMultipartEntity() { return true; }
    @Override public RootWormPart[] getParts() { return bodyParts; }

    @Override public void setId(int id) {
        super.setId(id);
        if (bodyParts != null) for (int i = 0; i < bodyParts.length; i++) bodyParts[i].setId(id + i + 1);
    }

    @Override public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        if (bodyParts != null) positionBodyParts();
    }

    @Override public void setYBodyRot(float yaw) {
        super.setYBodyRot(yaw);
        if (bodyParts != null) positionBodyParts();
    }

    @Override public void tick() {
        for (int i = 0; i < bodyParts.length; i++) previousPartPositions[i] = bodyParts[i].position();
        super.tick();
        positionBodyParts();
        for (int i = 0; i < bodyParts.length; i++) {
            var part = bodyParts[i];
            var old = previousPartPositions[i];
            part.xo = part.xOld = old.x;
            part.yo = part.yOld = old.y;
            part.zo = part.zOld = old.z;
        }
    }

    private void positionBodyParts() {
        for (int i = 0; i < bodyParts.length; i++) {
            var part = bodyParts[i];
            var pose = getSearchPose(i, 0);
            part.refreshDimensions();
            Vec3 position = searchPartPosition(i, pose);
            part.setPos(position.x, position.y, position.z);
            // Direct teleports start with matching old/current poses; tick restores its captured old poses.
            part.xo = part.xOld = part.getX();
            part.yo = part.yOld = part.getY();
            part.zo = part.zOld = part.getZ();
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.ATTACK_DAMAGE, 2.0).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.8) {
            @Override protected boolean shouldPanic() { return RootWormConfig.WORM_FEAR_HEAT.get() && RootWorm.this.isOnFire(); }
        });
        goalSelector.addGoal(2, new AvoidHeatGoal(this));
        goalSelector.addGoal(3, new RootWormAttackGoal(this));
        goalSelector.addGoal(4, new InvestigateSoundGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        // No nearest-player targeting goal: retaliation is the only source of an attack target.
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FEEDING, false);
        builder.define(BITE_TICKS, 0);
        builder.define(SEARCHING, false);
        builder.define(SEARCH_BLEND_START, 0.0F);
        builder.define(SEARCH_CHANGED_AT, 0L);
        builder.define(SEARCH_STARTED_AT, 0L);
    }

    @Override public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            if (isSearching() && !canSearchHere()) setSearching(false);
            int biteTicks = entityData.get(BITE_TICKS);
            if (biteTicks > 0) entityData.set(BITE_TICKS, biteTicks - 1);
            boolean feeding = !isSearching() && getSearchAmount(0) == 0 && getTarget() == null && !isOnFire() && getNavigation().isDone()
                    && tickCount % 100 < 30;
            entityData.set(FEEDING, feeding);
        } else {
            // Tracking updates may arrive every few ticks; interpolate plate motion between them.
            previousMouthOpen = mouthOpen;
            mouthOpen = Mth.approach(mouthOpen, biteOpenAmount(0), 0.4F);
        }
    }

    public boolean isFeeding() { return entityData.get(FEEDING); }

    public boolean isBiting() { return entityData.get(BITE_TICKS) > 0; }

    /** Closed while idle, feeding or pursuing; the four mouth plates spread only for an actual bite. */
    public float getMouthOpenAmount(float partialTick) {
        return level().isClientSide ? Mth.lerp(Mth.clamp(partialTick, 0, 1), previousMouthOpen, mouthOpen) : biteOpenAmount(partialTick);
    }

    private float biteOpenAmount(float partialTick) {
        int remaining = entityData.get(BITE_TICKS);
        if (remaining == 0) return 0;
        float elapsed = BITE_DURATION - remaining + partialTick;
        return Math.max(0, Math.min(1, Math.min(elapsed / 2.0F, (BITE_DURATION - elapsed) / 4.0F)));
    }

    @Override public boolean hurt(DamageSource source, float damage) {
        if (RootWormConfig.WORM_FIRE_WEAKNESS.get()) {
            if (source.is(DamageTypes.LAVA)) damage *= RootWormConfig.WORM_LAVA_MULTIPLIER.get().floatValue();
            else if (source.is(DamageTypeTags.IS_FIRE)) damage *= RootWormConfig.WORM_FIRE_MULTIPLIER.get().floatValue();
        }
        boolean accepted = super.hurt(source, damage);
        if (accepted && source.getEntity() instanceof LivingEntity attacker) hearSound(attacker, attacker.position(), 1.0);
        return accepted;
    }

    @Override public void updateDynamicGameEventListener(java.util.function.BiConsumer<net.minecraft.world.level.gameevent.DynamicGameEventListener<?>, net.minecraft.server.level.ServerLevel> registration) {
        if (level() instanceof net.minecraft.server.level.ServerLevel server) registration.accept(hearing, server);
    }

    public boolean hearSound(LivingEntity actor, Vec3 position, double volume) {
        double radius = RootWormConfig.HEARING_RADIUS.get() * volume;
        if (actor == this || actor instanceof RootWorm || !actor.isAlive() || actor.isSpectator()
                || actor.level() != level() || distanceToSqr(position) > radius * radius) return false;
        heardActor = actor.getUUID();
        heardPosition = position;
        heardAt = tickCount;
        return true; // Hearing never assigns an attack target: the creature remains neutral until hurt.
    }
    public boolean hasRecentSound() { return heardPosition != null && tickCount - heardAt <= RootWormConfig.SOUND_MEMORY_TICKS.get(); }
    public boolean hasRecentSoundFrom(LivingEntity actor) { return hasRecentSound() && actor.getUUID().equals(heardActor); }
    public Vec3 lastHeardPosition() { return heardPosition; }
    public void beginBite() { setSearching(false); entityData.set(BITE_TICKS, BITE_DURATION); }
    public boolean sensesCloseTarget(LivingEntity actor) {
        for (var part : bodyParts) if (part.getBoundingBox().inflate(0.35, 0.35, 0.35).intersects(actor.getBoundingBox())) return true;
        return false;
    }

    public void faceSound(Vec3 position) {
        Vec3 offset = position.subtract(position());
        float desired = (float) (Mth.atan2(offset.z, offset.x) * Mth.RAD_TO_DEG) - 90;
        float yaw = Mth.approachDegrees(getYRot(), desired, 25);
        setYRot(yaw);
        setYHeadRot(yaw);
        setYBodyRot(yaw);
    }

    public boolean canBite(LivingEntity actor) {
        var head = getParts()[0].getBoundingBox();
        var target = actor.getBoundingBox();
        if (!head.inflate(0.22, 0.15, 0.22).intersects(target)) return false;
        Vec3 from = head.getCenter();
        Vec3 to = new Vec3(Mth.clamp(from.x, target.minX + 0.001, target.maxX - 0.001),
                Mth.clamp(from.y, target.minY + 0.1, target.maxY - 0.1),
                Mth.clamp(from.z, target.minZ + 0.001, target.maxZ - 0.001));
        return level().clip(new net.minecraft.world.level.ClipContext(from, to,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this))
                .getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    public boolean isSearching() { return entityData.get(SEARCHING); }

    @Override public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        var bounds = super.getBoundingBoxForCulling();
        if (bodyParts != null) for (var part : bodyParts) bounds = bounds.minmax(part.getBoundingBox());
        return bounds;
    }

    /** Transition-only synced data and world time keep late-tracking clients on the same pose as the server. */
    private float searchLinearAmount(float partialTick) {
        float elapsed = Math.max(0, level().getGameTime() - entityData.get(SEARCH_CHANGED_AT) + partialTick);
        return Mth.clamp(entityData.get(SEARCH_BLEND_START) + (isSearching() ? elapsed : -elapsed) / 10.0F, 0, 1);
    }
    public float getSearchAmount(float partialTick) {
        float amount = searchLinearAmount(partialTick);
        return amount * amount * (3 - 2 * amount);
    }
    public RootWormSearchPose.Pose getSearchPose(int segment, float partialTick) {
        float age = level().getGameTime() - entityData.get(SEARCH_STARTED_AT) + partialTick;
        return RootWormSearchPose.sample(segment, getSearchAmount(partialTick), age);
    }
    public void setSearching(boolean searching) {
        if (level().isClientSide || searching == isSearching() || searching && !canSearchHere()) return;
        float amount = searchLinearAmount(0);
        entityData.set(SEARCH_BLEND_START, amount);
        entityData.set(SEARCH_CHANGED_AT, level().getGameTime());
        if (searching) entityData.set(SEARCH_STARTED_AT, level().getGameTime());
        entityData.set(SEARCHING, searching);
    }
    private Vec3 searchPartPosition(int segment, RootWormSearchPose.Pose pose) {
        float yaw = yBodyRot * Mth.DEG_TO_RAD, scale = getScale();
        float nose = segment == 0 ? 1.5F : 0;
        float side = (pose.x() - Mth.sin(pose.yaw()) * nose) / 16 * scale;
        float forward = (-pose.z() + Mth.cos(pose.yaw()) * nose) / 16 * scale;
        return new Vec3(getX() - Mth.sin(yaw) * forward - Mth.cos(yaw) * side,
                getY() + RootWormSearchPose.bottom(segment, pose) * scale,
                getZ() + Mth.cos(yaw) * forward - Mth.sin(yaw) * side);
    }
    public boolean canSearchHere() {
        if (!onGround() || isNoAi() || !isAlive() || isOnFire() || isInWaterOrBubble() || isInLava()
                || isBiting() || !getNavigation().isDone()) return false;
        // Check the complete lifting envelope, including side-to-side sweep; never raise through a ceiling.
        float scale = getScale();
        for (int i = 0; i < bodyParts.length; i++) {
            net.minecraft.world.phys.AABB envelope = null;
            for (float amount : new float[]{0, 1}) {
                var pose = RootWormSearchPose.sample(i, amount, 0);
                Vec3 position = searchPartPosition(i, pose);
                double width = RootWormSearchPose.width(i, pose) * scale;
                var box = new net.minecraft.world.phys.AABB(position.x - width / 2, position.y, position.z - width / 2,
                        position.x + width / 2, position.y + RootWormSearchPose.height(i, pose) * scale, position.z + width / 2);
                envelope = envelope == null ? box : envelope.minmax(box);
            }
            envelope = new net.minecraft.world.phys.AABB(envelope.minX, Math.max(getY() + 0.03 * scale, envelope.minY), envelope.minZ,
                    envelope.maxX, envelope.maxY, envelope.maxZ).inflate(0.35 * scale, 0, 0.35 * scale);
            if (!level().noCollision(this, envelope)) return false;
        }
        return true;
    }

    /** Vanilla fire and lava only; no custom blocks, block IDs or habitat tags are needed. */
    public static boolean isHeat(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.FIRE) || level.getFluidState(pos).is(FluidTags.LAVA);
    }
}
