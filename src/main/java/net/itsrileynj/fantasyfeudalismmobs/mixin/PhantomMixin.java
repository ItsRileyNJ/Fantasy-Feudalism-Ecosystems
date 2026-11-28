package net.itsrileynj.fantasyfeudalismmobs.mixin;

import java.util.Optional;
import java.util.UUID;
import net.itsrileynj.fantasyfeudalismmobs.PhantomPact;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Phantom.class)
public abstract class PhantomMixin extends FlyingMob {
    @Unique private static final EntityDataAccessor<Optional<UUID>> FFM_PACT_OWNER =
            SynchedEntityData.defineId(Phantom.class, EntityDataSerializers.OPTIONAL_UUID);
    @Shadow BlockPos anchorPoint;
    @Shadow Vec3 moveTargetPoint;
    protected PhantomMixin(EntityType<? extends FlyingMob> type,Level level){super(type,level);}
    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void ffm$definePactOwner(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(FFM_PACT_OWNER, Optional.empty());
    }
    @Inject(method="customServerAiStep",at=@At("TAIL"))
    private void ffm$protectOwner(CallbackInfo ci){
        Phantom phantom = (Phantom)(Object)this;
        var ownerAnchor = PhantomPact.tick(phantom);
        entityData.set(FFM_PACT_OWNER, Optional.ofNullable(PhantomPact.ownerId(phantom)));
        if(ownerAnchor!=null){anchorPoint=ownerAnchor;
            if(getTarget()==null&&position().distanceToSqr(Vec3.atCenterOf(ownerAnchor))>24*24)moveTargetPoint=Vec3.atCenterOf(ownerAnchor);}
    }
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void ffm$showPactTrail(Level level, ParticleOptions particle, double x, double y, double z,
                                 double vx, double vy, double vz) {
        var owner = entityData.get(FFM_PACT_OWNER).map(level::getPlayerByUUID).orElse(null);
        if (owner == null || !owner.isLocalPlayer()) {
            level.addParticle(particle, x, y, z, vx, vy, vz);
        } else if (!onGround() && tickCount % 5 == 0) {
            level.addParticle(ParticleTypes.END_ROD, x, y, z, vx, vy, vz);
        }
    }
    @Inject(method="shouldDespawnInPeaceful",at=@At("HEAD"),cancellable=true)
    private void ffm$temporaryAlly(CallbackInfoReturnable<Boolean> result){if(PhantomPact.bound(this))result.setReturnValue(false);}
    @Override public boolean requiresCustomPersistence(){return PhantomPact.bound(this)||super.requiresCustomPersistence();}
    @Override public boolean isAlliedTo(Entity other){return PhantomPact.allied((Phantom)(Object)this,other)||super.isAlliedTo(other);}
}
