package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.HorseGrazingGoal;
import net.itsrileynj.fantasyfeudalismmobs.SpawnHerd;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHorse.class)
public abstract class HorseMixin extends Animal {
    protected HorseMixin(EntityType<? extends Animal> type, Level level) { super(type, level); }

    @Inject(method="registerGoals", at=@At("TAIL"))
    private void ffm$goals(CallbackInfo ci) {
        if ((Object)this instanceof Horse horse) {
            // A player holding golden horse food must not starve grazing indefinitely.
            // Panic and riding still take priority; love mode ends grazing before breeding.
            goalSelector.addGoal(2, new HorseGrazingGoal(horse));
            goalSelector.addGoal(5, new SpawnHerd.FollowHerdGoal(horse));
        }
    }

    @Redirect(method="aiStep", at=@At(value="INVOKE",
            target="Lnet/minecraft/world/entity/animal/horse/AbstractHorse;canEatGrass()Z"))
    private boolean ffm$useGrazingGoal(AbstractHorse horse) {
        return !(horse instanceof Horse) && horse.canEatGrass();
    }

    @Redirect(method="isImmobile", at=@At(value="INVOKE",
            target="Lnet/minecraft/world/entity/animal/horse/AbstractHorse;isEating()Z"))
    private boolean ffm$keepGrazingAiTicking(AbstractHorse horse) {
        // The grazing goal holds MOVE/LOOK/JUMP itself. Native eating immobility
        // would skip serverAiStep entirely and freeze that goal's countdown.
        return !(horse instanceof Horse) && horse.isEating();
    }

}
