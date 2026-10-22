package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.PhantomPact;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Phantom.class)
public abstract class PhantomMixin extends FlyingMob {
    @Shadow BlockPos anchorPoint;
    @Shadow Vec3 moveTargetPoint;
    protected PhantomMixin(EntityType<? extends FlyingMob> type,Level level){super(type,level);}
    @Inject(method="customServerAiStep",at=@At("TAIL"))
    private void ffm$protectOwner(CallbackInfo ci){
        var ownerAnchor=PhantomPact.tick((Phantom)(Object)this);
        if(ownerAnchor!=null){anchorPoint=ownerAnchor;
            if(getTarget()==null&&position().distanceToSqr(Vec3.atCenterOf(ownerAnchor))>24*24)moveTargetPoint=Vec3.atCenterOf(ownerAnchor);}
    }
    @Inject(method="shouldDespawnInPeaceful",at=@At("HEAD"),cancellable=true)
    private void ffm$temporaryAlly(CallbackInfoReturnable<Boolean> result){if(PhantomPact.bound(this))result.setReturnValue(false);}
    @Override public boolean requiresCustomPersistence(){return PhantomPact.bound(this)||super.requiresCustomPersistence();}
    @Override public boolean isAlliedTo(Entity other){return PhantomPact.allied((Phantom)(Object)this,other)||super.isAlliedTo(other);}
}
