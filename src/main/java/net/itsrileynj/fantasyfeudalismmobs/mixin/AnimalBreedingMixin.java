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
    @org.spongepowered.asm.mixin.Unique private boolean ffm$automaticBirth;
    @Inject(method="finalizeSpawnChildFromBreeding",at=@At("HEAD"))
    private void ffm$automaticParents(ServerLevel level,Animal mate,AgeableMob child,CallbackInfo ci){
        ffm$automaticBirth=(Object)this instanceof net.minecraft.world.entity.animal.horse.Horse horse
                &&mate instanceof net.minecraft.world.entity.animal.horse.Horse other&&child instanceof net.minecraft.world.entity.animal.horse.Horse
                &&HorseLife.automaticPair(horse,other);
    }
    @Inject(method="finalizeSpawnChildFromBreeding", at=@At("TAIL"))
    private void ffm$horseBirth(ServerLevel level, Animal mate, AgeableMob child, CallbackInfo ci) {
        if(ffm$automaticBirth){
            var first=(net.minecraft.world.entity.animal.horse.Horse)(Object)this;
            var second=(net.minecraft.world.entity.animal.horse.Horse)mate;
            net.itsrileynj.fantasyfeudalismmobs.EcosystemAchievements.owner(first,first.getOwnerUUID(),"new_arrival");
            net.itsrileynj.fantasyfeudalismmobs.EcosystemAchievements.owner(second,second.getOwnerUUID(),"new_arrival");
        }
        ffm$automaticBirth=false;
        HorseLife.born((Animal)(Object)this, mate, child);
    }
}
