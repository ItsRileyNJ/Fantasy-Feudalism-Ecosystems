package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.CatRest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PlayerCatSleepMixin {
    @Inject(method="startSleeping",at=@At("TAIL"))
    private void ffm$nearbyCats(BlockPos bed,CallbackInfo ci){if((Object)this instanceof Player player)CatRest.begin(player);}
}
