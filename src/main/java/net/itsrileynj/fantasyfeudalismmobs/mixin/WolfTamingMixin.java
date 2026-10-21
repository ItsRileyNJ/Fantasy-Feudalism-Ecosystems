package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.WolfPacks;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TamableAnimal.class)
public abstract class WolfTamingMixin {
    @Inject(method="setTame",at=@At("TAIL"))
    private void ffm$endWildHunt(boolean tame,boolean attributes,CallbackInfo ci){
        if(tame&&(Object)this instanceof Wolf wolf&&!wolf.level().isClientSide)WolfPacks.tamed(wolf);
    }
}
