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
    private static final int STARE_TICKS = 44;
    private static final int FLARE_TICKS = 34;
    // Keep the opening ramp active through the windup. The delayed samples
    // let the tentacle tips finish their outward sweep after the lunge starts.
    private static final float FLARE_OPEN_STEP = 1F / 36F;
    private static final float FLARE_CLOSE_STEP = 1F / 22F;
    private static final int CIRCLE_TICKS = 80;
    private static final double ORBIT_RADIUS = 20;
    private static final double CRUISE_SPEED = .17;
    private static final double LUNGE_SPEED = 1.24;
    private static final double MOUTH_OFFSET = 3.9;
    private static final float CRUISE_TURN_SPEED = 3.2F;
    private static final float AIM_TURN_SPEED = 4.5F;
    private static final float TURN_ACCELERATION = .48F;
    private static final EntityDataAccessor<Byte> PHASE =
            SynchedEntityData.defineId(Leviathan.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> FLARING =
            SynchedEntityData.defineId(Leviathan.class, EntityDataSerializers.BOOLEAN);

    private int phaseTicks;
    private int scanDelay;
    private Vec3 charge = Vec3.ZERO;
    private Vec3 idleHeading = Vec3.ZERO;
    private int orbitDirection = 1;
    private float yawVelocity;
    private float previousGape;
    private float gape;
    private final float[] flareProgress = new float[14];
    private int flareIndex;
    private final float[] swimCurves = new float[13];
    private int curveIndex;
    private boolean hasSwimHeading;
    private float previousSwimHeading;
    private float swimHeading;
    private float swimStroke;
    private float previousSwimStroke;
    private float swimStrokeSpeed = .14F;
    private float swimAmplitude = .19F;
    private float previousSwimAmplitude = .19F;

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
        builder.define(FLARING, false);
    }

    public byte huntPhase() {
        return entityData.get(PHASE);
    }

    public boolean isFlaring() {
        return entityData.get(FLARING);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            updateSwimAnimation();
            previousGape = gape;
            boolean lunging = huntPhase() == LUNGE;
            gape = Mth.approach(gape, lunging ? .78F : 0F, lunging ? .30F : .42F);
            boolean flaring = isFlaring();
            float progress = Mth.approach(flareProgress[flareIndex], flaring ? 1F : 0F,
                    flaring ? FLARE_OPEN_STEP : FLARE_CLOSE_STEP);
            flareIndex = (flareIndex + 1) % flareProgress.length;
            flareProgress[flareIndex] = progress;
        }
    }

    private void updateSwimAnimation() {
        if (!hasSwimHeading) {
            swimHeading = getYRot();
            hasSwimHeading = true;
        }
        // Filter the entity yaw before rendering it so movement packets and
        // vanilla's separate body-yaw controller cannot kick the pose sideways.
        previousSwimHeading = swimHeading;
        float turn = Mth.wrapDegrees(getYRot() - swimHeading) * .42F;
        swimHeading = Mth.wrapDegrees(swimHeading + turn);

        float targetCurve = Mth.clamp(turn / CRUISE_TURN_SPEED, -1F, 1F);
        float previousCurve = swimCurves[curveIndex];
        float response = Math.abs(targetCurve) > Math.abs(previousCurve) ? .22F : .10F;
        float curve = Mth.lerp(response, previousCurve, targetCurve);
        curveIndex = (curveIndex + 1) % swimCurves.length;
        swimCurves[curveIndex] = curve;

        byte phase = huntPhase();
        swimStrokeSpeed = Mth.approach(swimStrokeSpeed, phase == LUNGE ? .44F : .14F, .06F);
        previousSwimStroke = swimStroke;
        swimStroke += swimStrokeSpeed;
        if (swimStroke >= Mth.TWO_PI) {
            swimStroke -= Mth.TWO_PI;
            previousSwimStroke -= Mth.TWO_PI;
        }
        previousSwimAmplitude = swimAmplitude;
        float amplitude = phase == STARE ? .018F : phase == LUNGE ? .31F : .19F;
        swimAmplitude = Mth.approach(swimAmplitude, amplitude, .045F);
    }

    public float swimHeading(float partialTick, float fallback) {
        return hasSwimHeading ? Mth.rotLerp(partialTick, previousSwimHeading, swimHeading) : fallback;
    }

    public float swimCurve(int delay, float partialTick) {
        int index = Math.floorMod(curveIndex - delay, swimCurves.length);
        int previous = Math.floorMod(index - 1, swimCurves.length);
        return Mth.lerp(partialTick, swimCurves[previous], swimCurves[index]);
    }

    public float swimStroke(float partialTick) {
        return Mth.lerp(partialTick, previousSwimStroke, swimStroke);
    }

    public float swimAmplitude(float partialTick) {
        return Mth.lerp(partialTick, previousSwimAmplitude, swimAmplitude);
    }

    public float gape(float partialTick) {
        return Mth.lerp(partialTick, previousGape, gape);
    }

    public float flare(int delay, float partialTick) {
        int index = Math.floorMod(flareIndex - delay, flareProgress.length);
        int previous = Math.floorMod(index - 1, flareProgress.length);
        float progress = Mth.lerp(partialTick, flareProgress[previous], flareProgress[index]);
        return progress * progress * progress * (progress * (progress * 6F - 15F) + 10F);
    }

    private void enter(byte phase) {
        entityData.set(PHASE, phase);
        entityData.set(FLARING, false);
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
        if (aim.lengthSqr() > 25 * 25 || !hasLineOfSight(prey)) {
            enter(CIRCLE);
            return;
        }
        float headingError = pointAlong(aim, AIM_TURN_SPEED);
        // Finish turning before counting the pause that telegraphs the lunge.
        if (headingError > 5F) phaseTicks = 0;
        if (phaseTicks < STARE_TICKS) {
            entityData.set(FLARING, phaseTicks >= STARE_TICKS - FLARE_TICKS);
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
        pointAlong(charge, AIM_TURN_SPEED);
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
        float headingError = pointAlong(step, CRUISE_TURN_SPEED);
        if (step.horizontalDistanceSqr() > 1.0e-5) {
            // Curve along the current heading and ease off the thrust through sharp turns.
            double thrust = Mth.clamp(1 - headingError / 100F, .13F, 1F);
            step = Vec3.directionFromRotation(getXRot(), getYRot()).scale(speed * thrust);
        }
        Vec3 motion = getDeltaMovement().lerp(step, .34);
        setDeltaMovement(canSwim(motion) ? motion : Vec3.ZERO);
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

    private float pointAlong(Vec3 course, float maxTurn) {
        float headingError = 0;
        if (course.horizontalDistanceSqr() > 1.0e-5) {
            float yaw = (float)(Mth.atan2(-course.x, course.z) * Mth.RAD_TO_DEG);
            float remaining = Mth.wrapDegrees(yaw - getYRot());
            float desiredTurn = Mth.clamp(remaining * .3F, -maxTurn, maxTurn);
            yawVelocity = Mth.approach(yawVelocity, desiredTurn, TURN_ACCELERATION);
            float turn = Mth.clamp(yawVelocity, -Math.abs(remaining), Math.abs(remaining));
            setYRot(getYRot() + turn);
            setYHeadRot(getYRot());
            yBodyRot = getYRot();
            headingError = Math.abs(Mth.wrapDegrees(yaw - getYRot()));
        } else {
            yawVelocity = Mth.approach(yawVelocity, 0F, TURN_ACCELERATION);
        }
        float pitch = (float)(-Mth.atan2(course.y, course.horizontalDistance()) * Mth.RAD_TO_DEG);
        setXRot(Mth.approach(getXRot(), Mth.clamp(pitch, -45, 45), maxTurn));
        return headingError;
    }
}
