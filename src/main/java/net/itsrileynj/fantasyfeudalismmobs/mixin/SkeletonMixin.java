package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(AbstractSkeleton.class)
public abstract class SkeletonMixin extends Monster implements CrossbowAttackMob {
    @Shadow @Final private RangedBowAttackGoal<AbstractSkeleton> bowGoal;
    @Shadow @Final private MeleeAttackGoal meleeGoal;
    @Shadow protected abstract int getHardAttackInterval();
    @Shadow protected abstract int getAttackInterval();
    @Unique private Goal ffm$crossbow;
    @Unique private RangedBowAttackGoal<AbstractSkeleton> ffm$bow;

    protected SkeletonMixin(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Inject(method = "createAttributes", at = @At("RETURN"))
    private static void ffm$range(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.getReturnValue().add(Attributes.FOLLOW_RANGE, 35);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "reassessWeaponGoal", at = @At("HEAD"), cancellable = true)
    private void ffm$weapons(CallbackInfo ci) {
        if (level() == null || level().isClientSide) return;
        goalSelector.removeGoal(bowGoal);
        goalSelector.removeGoal(meleeGoal);
        if (ffm$crossbow != null) goalSelector.removeGoal(ffm$crossbow);
        if (ffm$bow != null) goalSelector.removeGoal(ffm$bow);
        if (isHolding(stack -> stack.getItem() instanceof CrossbowItem)) {
            if (ffm$crossbow == null) ffm$crossbow = new RangedCrossbowAttackGoal((Monster & CrossbowAttackMob)(Object)this, 1.0, 35.0F);
            goalSelector.addGoal(4, ffm$crossbow);
        } else if (isHolding(stack -> stack.getItem() instanceof BowItem)) {
            int interval = level().getDifficulty() == Difficulty.HARD ? getHardAttackInterval() : getAttackInterval();
            if (ffm$bow == null) ffm$bow = new RangedBowAttackGoal<>((AbstractSkeleton)(Object)this, 1.0, interval, 35.0F);
            ffm$bow.setMinAttackInterval(interval);
            goalSelector.addGoal(4, ffm$bow);
        } else goalSelector.addGoal(4, meleeGoal);
        ci.cancel();
    }

    @Inject(method = "performRangedAttack", at = @At("HEAD"), cancellable = true)
    private void ffm$crossbowShot(LivingEntity target, float power, CallbackInfo ci) {
        if (distanceToSqr(target) > 35 * 35) { ci.cancel(); return; }
        if (isHolding(stack -> stack.getItem() instanceof CrossbowItem)) {
            performCrossbowAttack(this, 3.15F);
            ci.cancel();
        }
    }

    @Inject(method = "canFireProjectileWeapon", at = @At("HEAD"), cancellable = true)
    private void ffm$crossbowSupported(ProjectileWeaponItem item, CallbackInfoReturnable<Boolean> cir) {
        if (item instanceof CrossbowItem) cir.setReturnValue(true);
    }

    @Override public void setChargingCrossbow(boolean charging) { setAggressive(charging || getTarget() != null); }
    @Override public void onCrossbowAttackPerformed() { setNoActionTime(0); }
}
