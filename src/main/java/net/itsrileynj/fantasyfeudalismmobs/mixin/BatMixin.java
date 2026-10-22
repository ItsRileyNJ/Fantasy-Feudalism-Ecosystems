package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.BatResidents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bat.class)
public abstract class BatMixin extends AmbientCreature {
    @Unique private final BatResidents ffm$resident=new BatResidents();
    protected BatMixin(EntityType<? extends AmbientCreature> type,Level level){super(type,level);}
    @Inject(method="customServerAiStep",at=@At("HEAD"),cancellable=true)
    private void ffm$homeFlight(CallbackInfo ci){if(ffm$resident.tick((Bat)(Object)this))ci.cancel();}
    @Override public boolean requiresCustomPersistence(){return BatResidents.housed((Bat)(Object)this)||super.requiresCustomPersistence();}
}
