package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.WitherCombat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherSkeleton.class)
public abstract class WitherSkeletonMixin extends AbstractSkeleton {
    protected WitherSkeletonMixin(EntityType<? extends AbstractSkeleton> type,Level level){super(type,level);}
    @Inject(method="registerGoals",at=@At("TAIL"))
    private void ffm$shieldTactics(CallbackInfo ci){goalSelector.addGoal(3,new WitherCombat.ShieldAdvance((WitherSkeleton)(Object)this));}
    @Override protected void blockUsingShield(LivingEntity attacker){
        super.blockUsingShield(attacker);
        if(attacker.canDisableShield()){
            getPersistentData().putLong(WitherCombat.SHIELD_DISABLED,level().getGameTime()+100);
            stopUsingItem();level().broadcastEntityEvent(this,(byte)30);
        }
    }
    @Override protected void hurtCurrentlyUsedShield(float amount){
        if(getUseItem().is(Items.SHIELD)&&amount>=3){
            getUseItem().hurtAndBreak(1+(int)Math.floor(amount),this,LivingEntity.getSlotForHand(getUsedItemHand()));
            if(getUseItem().isEmpty())stopUsingItem();
        }
    }
}
