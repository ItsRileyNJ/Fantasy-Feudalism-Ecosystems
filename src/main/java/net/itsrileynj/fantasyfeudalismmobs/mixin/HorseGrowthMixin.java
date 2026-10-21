package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.horse.Horse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AgeableMob.class)
public abstract class HorseGrowthMixin {
    @Redirect(method="aiStep", at=@At(value="INVOKE",
            target="Lnet/minecraft/world/entity/AgeableMob;setAge(I)V"))
    private void ffm$slowerFoalGrowth(AgeableMob animal, int age) {
        if (animal instanceof Horse && animal.getAge() < 0) {
            // Only natural baby aging slows down. Keep vanilla feeding and adult
            // breeding cooldowns, and persist the partial tick across save/load.
            var data = animal.getPersistentData();
            int phase = (data.getInt("ffm_horse_growth_phase") + 1) % 3;
            data.putInt("ffm_horse_growth_phase", phase);
            if (phase != 0) return;
        }
        animal.setAge(age);
    }
}
