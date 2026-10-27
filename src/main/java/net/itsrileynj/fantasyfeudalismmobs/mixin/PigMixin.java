package net.itsrileynj.fantasyfeudalismmobs.mixin;

import java.util.List;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.network.syncher.*;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Pig.class)
public abstract class PigMixin extends Animal implements PigState {
    @Unique private static final EntityDataAccessor<Boolean> FFM_DIGGING=SynchedEntityData.defineId(Pig.class,EntityDataSerializers.BOOLEAN);
    @Unique private float ffm$oldDig,ffm$dig;
    protected PigMixin(EntityType<? extends Animal> type,Level level) { super(type,level); }
    @Inject(method="defineSynchedData",at=@At("TAIL"))
    private void ffm$defineDig(SynchedEntityData.Builder builder,CallbackInfo ci) { builder.define(FFM_DIGGING,false); }
    @Override public boolean ffm$digging() { return entityData.get(FFM_DIGGING); }
    @Override public void ffm$setDigging(boolean digging) { entityData.set(FFM_DIGGING,digging); }
    @Override public float ffm$digAmount(float partialTick) { return Mth.lerp(partialTick,ffm$oldDig,ffm$dig); }
    @Override public void aiStep() {
        // Passenger-controlled movement may skip ordinary server goal ticking.
        if(!level().isClientSide&&ffm$digging()&&(isVehicle()||isPassenger()||isInWater()))ffm$setDigging(false);
        super.aiStep();
        if(level().isClientSide) {
            ffm$oldDig=ffm$dig;
            ffm$dig+=Mth.clamp((ffm$digging()?1:0)-ffm$dig,-.2F,.2F);
        }
    }
    @Inject(method="registerGoals",at=@At("TAIL"))
    private void ffm$pigGoals(CallbackInfo ci) {
        Pig pig=(Pig)(Object)this;
        for(var wrapped:List.copyOf(goalSelector.getAvailableGoals())) {
            if(wrapped.getGoal() instanceof FollowParentGoal)goalSelector.removeGoal(wrapped.getGoal());
            else if(wrapped.getGoal() instanceof WaterAvoidingRandomStrollGoal) {
                goalSelector.removeGoal(wrapped.getGoal());goalSelector.addGoal(8,wrapped.getGoal());
            }
        }
        goalSelector.addGoal(5,new PigGoals.FollowFamily(pig));
        goalSelector.addGoal(6,new PigGoals.Dig(pig));
        goalSelector.addGoal(6,new PigGoals.EatMushroom(pig));
        goalSelector.addGoal(7,new PigGoals.LooseHerd(pig));
    }
    @Inject(method="mobInteract",at=@At("HEAD"),cancellable=true)
    private void ffm$feedMushroom(Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> cir) {
        var stack=player.getItemInHand(hand);
        if(!player.isSpectator()&&isAlive()&&isBaby()&&PigLife.mushroom(stack)) {
            if(!level().isClientSide&&PigLife.grow((Pig)(Object)this))stack.consume(1,player);
            cir.setReturnValue(InteractionResult.sidedSuccess(level().isClientSide));
        }
    }
}
