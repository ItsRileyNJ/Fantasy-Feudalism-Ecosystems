package net.itsrileynj.fantasyfeudalismmobs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets="net.minecraft.world.entity.animal.Cat$CatRelaxOnOwnerGoal")
public abstract class CatOwnerNapMixin {
    @ModifyConstant(method="canUse",constant=@Constant(doubleValue=100.0))
    private double ffm$fourBlocks(double vanilla){return 16;}
}
