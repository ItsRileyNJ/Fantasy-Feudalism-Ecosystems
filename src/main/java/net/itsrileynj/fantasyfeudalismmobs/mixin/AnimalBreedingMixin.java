package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.HorseLife;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public abstract class AnimalBreedingMixin {
    @Inject(method="finalizeSpawnChildFromBreeding", at=@At("TAIL"))
    private void ffm$horseBirth(ServerLevel level, Animal mate, AgeableMob child, CallbackInfo ci) {
        HorseLife.born((Animal)(Object)this, mate, child);
    }
}
