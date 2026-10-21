package net.itsrileynj.fantasyfeudalismmobs.mixin;

import java.util.List;
import net.itsrileynj.fantasyfeudalismmobs.SpawnHerd;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Cow.class)
public abstract class CowMixin extends Animal {
    protected CowMixin(EntityType<? extends Animal> type, Level level) { super(type, level); }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ffm$herd(CallbackInfo ci) {
        // Keep panic, breeding, food temptation and calf/parent following ahead of
        // herd movement, but let separated members interrupt ordinary wandering.
        for (var goal : List.copyOf(goalSelector.getAvailableGoals())) {
            if (goal.getGoal() instanceof WaterAvoidingRandomStrollGoal) {
                goalSelector.removeGoal(goal.getGoal());
                goalSelector.addGoal(6, goal.getGoal());
            }
        }
        goalSelector.addGoal(5, new SpawnHerd.FollowHerdGoal((Cow)(Object)this));
    }
}
