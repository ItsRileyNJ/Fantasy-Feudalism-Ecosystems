package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.MobRules;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Blaze.class)
public abstract class BlazeMixin extends Monster {
    protected BlazeMixin(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Override public void travel(Vec3 input) {
        boolean dashing = !level().isClientSide && MobRules.dashMovement((Blaze)(Object)this);
        super.travel(dashing ? new Vec3(0, input.y, 0) : input);
    }
}
