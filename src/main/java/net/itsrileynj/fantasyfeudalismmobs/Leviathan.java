package net.itsrileynj.fantasyfeudalismmobs;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Leviathan extends WaterAnimal implements Enemy {
    public static final byte CIRCLE = 0, STARE = 1, LUNGE = 2, SNAP = 3;
    private static final int STARE_TICKS = 20;
    private static final int CIRCLE_TICKS = 80;
    private static final double ORBIT_RADIUS = 20;
    private static final double CRUISE_SPEED = .17;
    private static final double LUNGE_SPEED = 1.24;
    private static final double MOUTH_OFFSET = 3.9;
    private static final EntityDataAccessor<Byte> PHASE =
            SynchedEntityData.defineId(Leviathan.class, EntityDataSerializers.BYTE);

    private int phaseTicks;
    private int scanDelay;
    private Vec3 charge = Vec3.ZERO;
    private Vec3 idleHeading = Vec3.ZERO;
    private int orbitDirection = 1;
    private float previousGape;
    private float gape;

    public Leviathan(EntityType<? extends Leviathan> type, Level level) {
        super(type, level);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 80)
                .add(Attributes.ATTACK_DAMAGE, 12).add(Attributes.KNOCKBACK_RESISTANCE, .8)
                .add(Attributes.FOLLOW_RANGE, 44);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, CIRCLE);
    }

    public byte huntPhase() {
        return entityData.get(PHASE);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            previousGape = gape;
            boolean lunging = huntPhase() == LUNGE;
            gape = Mth.approach(gape, lunging ? .78F : 0F, lunging ? .30F : .42F);
        }
    }

    public float gape(float partialTick) {
        return Mth.lerp(partialTick, previousGape, gape);
    }

    private void enter(byte phase) {
        entityData.set(PHASE, phase);
        phaseTicks = 0;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(4.5, 1, 4.5);
    }

    @Override
    public void travel(Vec3 input) {
        if (isEffectiveAi() && isInWater()) {
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(.91));
        } else {
            super.travel(input);
        }
    }

    @Override
    protected void customServerAiStep() {
        if (!isInWater() || level().getDifficulty() == Difficulty.PEACEFUL) {
            if (huntPhase() != CIRCLE) enter(CIRCLE);
            return;
        }
        Player prey = getTarget() instanceof Player player && validPrey(player) ? player : null;
        if (prey == null) {
            if (getTarget() != null) setTarget(null);
            if (huntPhase() != CIRCLE) enter(CIRCLE);
            if (--scanDelay <= 0) {
                scanDelay = 12;
                prey = level().getNearestPlayer(getX(), getY(), getZ(), 44,
                        entity -> entity instanceof Player player && validPrey(player));
                if (prey != null) {
                    setTarget(prey);
                    enter(CIRCLE);
                }
            }
            if (prey == null) {
                patrol();
                return;
            }
        }

        phaseTicks++;
        switch (huntPhase()) {
            case STARE -> stare(prey);
            case LUNGE -> lunge(prey);
            case SNAP -> {
                setDeltaMovement(getDeltaMovement().scale(.55));
                if (phaseTicks >= 8) enter(CIRCLE);
            }
            default -> circle(prey);
        }
    }

    private boolean validPrey(Player player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator()
                && distanceToSqr(player) <= 44 * 44
                && level().getFluidState(BlockPos.containing(player.getX(), player.getY(.55), player.getZ())).is(FluidTags.WATER);
    }

    private Vec3 aimAt(Player prey) {
        return prey.position().add(0, prey.getBbHeight() * .55, 0).subtract(position().add(0, 1.1, 0));
    }

    private void circle(Player prey) {
        Vec3 aim = aimAt(prey);
        double distance = aim.length();
        if (phaseTicks >= CIRCLE_TICKS && distance >= 17 && distance <= 22 && hasLineOfSight(prey)) {
            enter(STARE);
            stare(prey);
            return;
        }
        Vec3 outward = new Vec3(-aim.x, 0, -aim.z).normalize();
        if (outward.lengthSqr() == 0) outward = new Vec3(1, 0, 0);
        Vec3 tangent = new Vec3(-outward.z, 0, outward.x).scale(orbitDirection);
        double correction = Mth.clamp((ORBIT_RADIUS - aim.horizontalDistance()) / 6, -1, 1);
        Vec3 course = tangent.scale(1 - .78 * Math.abs(correction)).add(outward.scale(correction))
                .add(0, Mth.clamp(aim.y * .14, -.48, .48), 0);
        if (!swim(course, CRUISE_SPEED) && phaseTicks % 20 == 0) orbitDirection = -orbitDirection;
    }

    private void stare(Player prey) {
        Vec3 aim = aimAt(prey);
        setDeltaMovement(Vec3.ZERO);
        pointAlong(aim, 35);
        if (phaseTicks < STARE_TICKS) return;
        if (aim.lengthSqr() > 25 * 25 || !hasLineOfSight(prey)) {
            enter(CIRCLE);
            return;
        }
        charge = aim.normalize();
        enter(LUNGE);
    }

    private void lunge(Player prey) {
        double speed = Math.min(LUNGE_SPEED, .20 + .21 * phaseTicks);
        Vec3 step = charge.scale(speed);
        Vec3 mouth = position().add(0, 1.1, 0).add(charge.scale(MOUTH_OFFSET));
        var hitbox = prey.getBoundingBox().inflate(.57);
        if ((hitbox.contains(mouth) || hitbox.clip(mouth, mouth.add(step)).isPresent()) && hasLineOfSight(prey)) {
            if (doHurtTarget(prey)) playSound(SoundEvents.GENERIC_EAT, 1.15F, .65F);
            enter(SNAP);
            return;
        }
        if (phaseTicks > 27 || !canSwim(step) || (phaseTicks > 2 && horizontalCollision)) {
            setDeltaMovement(Vec3.ZERO);
            enter(CIRCLE);
            return;
        }
        setDeltaMovement(step);
        pointAlong(charge, 28);
    }

    private void patrol() {
        if (idleHeading.lengthSqr() == 0 || tickCount % 100 == 0) {
            idleHeading = Vec3.directionFromRotation(8 * (random.nextFloat() - .5F), random.nextFloat() * 360);
        }
        if (!swim(idleHeading, .09)) idleHeading = Vec3.ZERO;
    }

    private boolean swim(Vec3 course, double speed) {
        Vec3 direct = course.normalize();
        Vec3 step = direct.scale(speed);
        if (!canSwim(direct.scale(1.4))) {
            Vec3 left = direct.yRot(.95F);
            Vec3 right = direct.yRot(-.95F);
            if (canSwim(left.scale(1.2))) step = left.scale(speed);
            else if (canSwim(right.scale(1.2))) step = right.scale(speed);
            else if (canSwim(new Vec3(0, 1, 0))) step = new Vec3(0, speed, 0);
            else if (canSwim(new Vec3(0, -1, 0))) step = new Vec3(0, -speed, 0);
            else {
                setDeltaMovement(Vec3.ZERO);
                return false;
            }
        }
        setDeltaMovement(getDeltaMovement().lerp(step, .34));
        pointAlong(step, 8);
        return true;
    }

    private boolean canSwim(Vec3 step) {
        Vec3 next = position().add(step);
        BlockPos bottom = BlockPos.containing(next.add(0, .15, 0));
        BlockPos top = BlockPos.containing(next.add(0, getBbHeight() - .1, 0));
        return level().hasChunkAt(bottom) && level().getFluidState(bottom).is(FluidTags.WATER)
                && level().getFluidState(top).is(FluidTags.WATER)
                && level().noCollision(this, getBoundingBox().move(step));
    }

    private void pointAlong(Vec3 course, float speed) {
        if (course.horizontalDistanceSqr() > 1.0e-5) {
            float yaw = (float)(Mth.atan2(-course.x, course.z) * Mth.RAD_TO_DEG);
            setYRot(Mth.approachDegrees(getYRot(), yaw, speed));
            setYHeadRot(getYRot());
            yBodyRot = getYRot();
        }
        float pitch = (float)(-Mth.atan2(course.y, course.horizontalDistance()) * Mth.RAD_TO_DEG);
        setXRot(Mth.approach(getXRot(), Mth.clamp(pitch, -45, 45), speed));
    }
}
