package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EnderMan.class)
public interface EndermanAccessor {
    @Invoker("teleport") boolean ffm$teleport(double x, double y, double z);
}
