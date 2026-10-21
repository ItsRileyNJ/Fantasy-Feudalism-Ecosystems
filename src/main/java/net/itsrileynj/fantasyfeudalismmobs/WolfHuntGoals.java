package net.itsrileynj.fantasyfeudalismmobs;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.*;

public final class WolfHuntGoals {
    public static void install(Wolf wolf,GoalSelector goals,GoalSelector targets) {
        for(var entry:List.copyOf(goals.getAvailableGoals())) {
            Goal old=entry.getGoal();Goal replacement=null;
            if(old.getClass()==MeleeAttackGoal.class)replacement=new MeleeAttackGoal(wolf,1.0,true) {
                @Override public boolean canUse(){return !WolfPacks.handles(wolf,wolf.getTarget())&&super.canUse();}
                @Override public boolean canContinueToUse(){return !WolfPacks.handles(wolf,wolf.getTarget())&&super.canContinueToUse();}
            };
            if(old.getClass()==LeapAtTargetGoal.class)replacement=new LeapAtTargetGoal(wolf,0.4F) {
                @Override public boolean canUse(){return !WolfPacks.handles(wolf,wolf.getTarget())&&super.canUse();}
            };
            if(replacement!=null){goals.removeGoal(old);goals.addGoal(entry.getPriority(),replacement);}
        }
        for(var entry:List.copyOf(targets.getAvailableGoals()))
            if(entry.getGoal() instanceof NonTameRandomTargetGoal<?>)targets.removeGoal(entry.getGoal());
        targets.addGoal(5,new SelectSheep(wolf));
        targets.addGoal(5,new NonTameRandomTargetGoal<>(wolf,Animal.class,false,
                prey->!(prey instanceof Sheep)&&Wolf.PREY_SELECTOR.test(prey)&&WolfPacks.hungry(wolf)));
        targets.addGoal(6,new NonTameRandomTargetGoal<>(wolf,Turtle.class,false,
                prey->Turtle.BABY_ON_LAND_SELECTOR.test(prey)&&WolfPacks.hungry(wolf)));
        goals.addGoal(5,new Hunt(wolf));
    }

    public static final class SelectSheep extends Goal {
        private final Wolf wolf;
        private Sheep sheep;
        private int scan;
        public SelectSheep(Wolf wolf){this.wolf=wolf;setFlags(EnumSet.of(Flag.TARGET));}
        @Override public boolean canUse(){
            if(!WolfPacks.canHunt(wolf))return false;
            if(scan-- >0)return false;scan=reducedTickDelay(10);
            sheep=WolfPacks.prey(wolf);return sheep!=null;
        }
        @Override public void start(){sheep=WolfPacks.joinHunt(wolf,sheep);wolf.setTarget(sheep);}
        @Override public boolean canContinueToUse(){
            return WolfPacks.canHunt(wolf)&&sheep!=null&&sheep.isAlive()&&wolf.getTarget()==sheep&&wolf.distanceToSqr(sheep)<=1024;
        }
        @Override public void stop(){if(wolf.getTarget()==sheep)wolf.setTarget(null);sheep=null;}
    }

    public static final class Hunt extends MeleeAttackGoal {
        private final Wolf wolf;
        public Hunt(Wolf wolf){super(wolf,1.0,true);this.wolf=wolf;}
        @Override public boolean canUse(){return WolfPacks.canHunt(wolf)&&wolf.getTarget() instanceof Sheep&&super.canUse();}
        @Override public boolean canContinueToUse(){return WolfPacks.canHunt(wolf)&&wolf.getTarget() instanceof Sheep&&super.canContinueToUse();}
        @Override public void start(){super.start();((WolfState)wolf).ffm$setHunting(true);}
        @Override public void tick(){
            if(wolf.getTarget() instanceof Sheep sheep) {
                boolean charge=WolfPacks.charging(wolf,sheep);
                ((WolfState)wolf).ffm$setStalking(!charge);
                super.tick();wolf.getNavigation().setSpeedModifier(charge?1.0:0.5);
            }
        }
        @Override protected void checkAndPerformAttack(LivingEntity target){
            if(target instanceof Sheep sheep&&WolfPacks.charging(wolf,sheep))super.checkAndPerformAttack(target);
        }
        @Override public void stop(){
            super.stop();((WolfState)wolf).ffm$setStalking(false);((WolfState)wolf).ffm$setHunting(false);
        }
    }
    private WolfHuntGoals(){}
}
