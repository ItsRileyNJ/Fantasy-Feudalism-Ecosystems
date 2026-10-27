package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.SheepState;
import net.itsrileynj.fantasyfeudalismmobs.SheepCare;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.animal.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Sheep.class)
public abstract class SheepMixin implements SheepState {
    @Unique private static final EntityDataAccessor<Boolean> FFM_COLD=SynchedEntityData.defineId(Sheep.class,EntityDataSerializers.BOOLEAN);
    @Unique private int ffm$grazes;
    @Unique private long ffm$warmAt=-1;

    @Override public int ffm$grazes() { return ffm$grazes; }
    @Override public boolean ffm$isCold() { return ((Sheep)(Object)this).getEntityData().get(FFM_COLD); }

    @Inject(method="defineSynchedData", at=@At("TAIL"))
    private void ffm$defineCold(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(FFM_COLD,false);
    }

    @Unique private void ffm$updateCold() {
        Sheep sheep=(Sheep)(Object)this;
        if (!sheep.level().isClientSide) {
            boolean cold=ffm$isCold();
            long now=sheep.level().getGameTime();
            if (!sheep.isSheared() || !sheep.isAlive()) {
                cold=false;ffm$warmAt=-1;
            } else if (SheepCare.exposedToCold(sheep)) {
                cold=true;ffm$warmAt=-1;
            } else if (cold) {
                if (ffm$warmAt<0) ffm$warmAt=now+600;
                if (now>=ffm$warmAt) { cold=false;ffm$warmAt=-1; }
            } else ffm$warmAt=-1;
            sheep.getEntityData().set(FFM_COLD,cold);
            SheepCare.updateSpeed(sheep,cold);
        }
    }

    @Inject(method="aiStep", at=@At("HEAD"))
    private void ffm$checkWeather(CallbackInfo ci) {
        Sheep sheep=(Sheep)(Object)this;
        // Stagger weather checks; no entity searches or forced chunk loads.
        if ((sheep.tickCount+sheep.getId())%20==0) ffm$updateCold();
    }

    @Inject(method="setSheared", at=@At("HEAD"))
    private void ffm$resetMealProgress(boolean sheared, CallbackInfo ci) {
        ffm$grazes=0;
    }

    @Inject(method="setSheared", at=@At("TAIL"))
    private void ffm$refreshInsulation(boolean sheared, CallbackInfo ci) { ffm$updateCold(); }

    @Redirect(method="ate", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/animal/Sheep;setSheared(Z)V"))
    private void ffm$threeMeals(Sheep sheep, boolean sheared) {
        // Leave native baby growth and other ate() hooks intact.
        if (!sheep.level().isClientSide && sheep.isSheared() && !ffm$isCold() && ++ffm$grazes>=3)
            sheep.setSheared(false);
    }

    @Inject(method="addAdditionalSaveData", at=@At("TAIL"))
    private void ffm$saveMeals(CompoundTag tag, CallbackInfo ci) {
        tag.putInt("ffm_wool_grazes",ffm$grazes);
        tag.putBoolean("ffm_sheep_cold",ffm$isCold());
        tag.putLong("ffm_sheep_warm_at",ffm$warmAt);
    }

    @Inject(method="readAdditionalSaveData", at=@At("TAIL"))
    private void ffm$loadMeals(CompoundTag tag, CallbackInfo ci) {
        ffm$grazes=((Sheep)(Object)this).isSheared()?Math.clamp(tag.getInt("ffm_wool_grazes"),0,2):0;
        Sheep sheep=(Sheep)(Object)this;
        if (!sheep.level().isClientSide) {
            sheep.getEntityData().set(FFM_COLD,sheep.isSheared()&&tag.getBoolean("ffm_sheep_cold"));
            ffm$warmAt=tag.contains("ffm_sheep_warm_at")?tag.getLong("ffm_sheep_warm_at"):-1;
            ffm$warmAt=Math.min(ffm$warmAt,sheep.level().getGameTime()+600);
        }
        ffm$updateCold();
    }
}
