package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.network.syncher.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Wolf.class)
public abstract class WolfMixin extends TamableAnimal implements WolfState {
    @Unique private static final EntityDataAccessor<Boolean> FFM_STALKING=SynchedEntityData.defineId(Wolf.class,EntityDataSerializers.BOOLEAN);
    @Unique private boolean ffm$hunting;
    @Unique private float ffm$previousCrouch,ffm$crouch;
    protected WolfMixin(EntityType<? extends TamableAnimal> type,Level level){super(type,level);}

    @Inject(method="defineSynchedData",at=@At("TAIL"))
    private void ffm$state(SynchedEntityData.Builder builder,CallbackInfo ci){builder.define(FFM_STALKING,false);}
    @Inject(method="registerGoals",at=@At("TAIL"))
    private void ffm$goals(CallbackInfo ci){WolfHuntGoals.install((Wolf)(Object)this,goalSelector,targetSelector);}
    @Inject(method="aiStep",at=@At("TAIL"))
    private void ffm$blendPose(CallbackInfo ci){
        if(!level().isClientSide)return;
        ffm$previousCrouch=ffm$crouch;
        float desired=isTame()?0:ffm$stalking()?1:0;
        ffm$crouch+=Mth.clamp(desired-ffm$crouch,-0.15F,0.15F);
    }
    @Override public boolean ffm$stalking(){return entityData.get(FFM_STALKING);}
    @Override public void ffm$setStalking(boolean stalking){entityData.set(FFM_STALKING,stalking);}
    @Override public boolean ffm$hunting(){return ffm$hunting;}
    @Override public void ffm$setHunting(boolean hunting){ffm$hunting=hunting;}
    @Override public float ffm$crouch(float partialTick){return Mth.lerp(partialTick,ffm$previousCrouch,ffm$crouch);}
}
