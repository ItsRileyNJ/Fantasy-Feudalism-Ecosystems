package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.SpiderCompanion;
import net.itsrileynj.fantasyfeudalismmobs.SpiderGoals;
import net.itsrileynj.fantasyfeudalismmobs.SpiderState;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.syncher.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Spider.class)
public abstract class SpiderMixin extends Monster implements SpiderState {
    @Unique private static final EntityDataAccessor<Optional<UUID>> FFM_OWNER = SynchedEntityData.defineId(Spider.class, EntityDataSerializers.OPTIONAL_UUID);
    @Unique private static final EntityDataAccessor<Boolean> FFM_SITTING = SynchedEntityData.defineId(Spider.class, EntityDataSerializers.BOOLEAN);
    @Unique private double ffm$landingDistance = Double.NaN;
    protected SpiderMixin(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ffm$goals(CallbackInfo ci) {
        SpiderGoals.install((Spider)(Object)this, goalSelector, targetSelector);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void ffm$trackedState(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(FFM_OWNER, Optional.empty());
        builder.define(FFM_SITTING, false);
    }
    @Override protected boolean shouldDespawnInPeaceful() {
        return !SpiderCompanion.isTamed((Spider)(Object)this) && super.shouldDespawnInPeaceful();
    }
    @Override public Optional<UUID> ffm$owner() { return entityData.get(FFM_OWNER); }
    @Override public boolean ffm$sitting() { return entityData.get(FFM_SITTING); }
    @Override public double ffm$landingDistance() { return ffm$landingDistance; }
    @Override protected void checkFallDamage(double verticalMovement, boolean grounded, BlockState state, BlockPos pos) {
        double previous = ffm$landingDistance;
        // Entity.checkFallDamage omits the final downward displacement from fallDistance.
        // Include it for our immunity threshold, leaving vanilla damage amounts unchanged.
        ffm$landingDistance = grounded && fallDistance > 0
                ? fallDistance - Math.min(0, verticalMovement) : Double.NaN;
        try { super.checkFallDamage(verticalMovement, grounded, state, pos); }
        finally { ffm$landingDistance = previous; }
    }
    @Override public void ffm$setState(Optional<UUID> owner, boolean sitting) {
        entityData.set(FFM_OWNER, owner);
        entityData.set(FFM_SITTING, sitting);
    }
}
