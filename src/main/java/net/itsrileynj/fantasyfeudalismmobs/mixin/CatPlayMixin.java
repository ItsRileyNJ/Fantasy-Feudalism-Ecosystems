package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.BabyCatPlay;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Cat.class)
public abstract class CatPlayMixin extends TamableAnimal {
    protected CatPlayMixin(EntityType<? extends TamableAnimal> type,Level level){super(type,level);}
    @Inject(method="registerGoals",at=@At("TAIL"))
    private void ffm$play(CallbackInfo ci){
        Cat cat=(Cat)(Object)this;
        goalSelector.addGoal(5,new AvoidEntityGoal<>(cat,Villager.class,
                child->child instanceof Villager villager&&BabyCatPlay.chasing(villager,cat),6,0.9,1.1,
                child->!cat.isTame()) {
            @Override public boolean canContinueToUse() {
                return !cat.isTame() && toAvoid!=null && BabyCatPlay.chasing(toAvoid,cat) && super.canContinueToUse();
            }
            @Override public void stop(){super.stop();cat.getNavigation().stop();}
        });
    }
}
