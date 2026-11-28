package net.itsrileynj.fantasyfeudalismmobs;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public final class VariantSlime extends Slime {
    public VariantSlime(EntityType<? extends Slime> type, Level level) {
        super(type, level);
        setSize(isFlame() ? 2 : 1, true);
    }

    public boolean isFlame() {
        return getType() == SlimeVariants.FLAME.get();
    }

    @Override
    public void setSize(int size, boolean resetHealth) {
        super.setSize(isFlame() ? Math.min(size, 2) : 1, resetHealth);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData group) {
        var data = super.finalizeSpawn(level, difficulty, reason, group);
        setSize(isFlame() ? 2 : 1, true);
        if (isFlame()) {
            var pos = blockPosition();
            var fluid = level.getFluidState(pos);
            if (fluid.is(FluidTags.LAVA) && !level.getFluidState(pos.above()).is(FluidTags.LAVA)) {
                setPos(getX(), pos.getY() + fluid.getHeight(level, pos), getZ());
            }
        }
        return data;
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        if (isFlame() && level.getFluidState(blockPosition()).is(FluidTags.LAVA)) {
            return level.isUnobstructed(this);
        }
        return super.checkSpawnObstruction(level);
    }

    @Override
    public boolean canStandOnFluid(FluidState fluid) {
        return isFlame() && fluid.is(FluidTags.LAVA);
    }

    @Override
    public void travel(Vec3 input) {
        if (isFlame() && isInLava()) {
            var pos = blockPosition();
            var fluid = level().getFluidState(pos);
            if (fluid.is(FluidTags.LAVA) && getY() < pos.getY() + fluid.getHeight(level(), pos) - 1.0E-5) {
                var motion = getDeltaMovement();
                setDeltaMovement(motion.x, Math.max(motion.y, .18), motion.z);
            }
        }
        super.travel(input);
    }

    @Override
    protected boolean isDealsDamage() {
        return isFlame() ? super.isDealsDamage() : isEffectiveAi();
    }

    @Override
    public boolean canFreeze() {
        return isFlame() && super.canFreeze();
    }

    @Override
    protected ParticleOptions getParticleType() {
        return isFlame() ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && isAlive() && tickCount % 5 == 0) {
            level().addParticle(getParticleType(), getRandomX(.9), getRandomY(), getRandomZ(.9), 0, .015, 0);
        }
    }
}
