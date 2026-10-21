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
        goals.addGoal(7,new Regroup(wolf));
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
        private int assemblePath;
        public Hunt(Wolf wolf){super(wolf,1.0,true);this.wolf=wolf;}
        @Override public boolean canUse(){return WolfPacks.canHunt(wolf)&&wolf.getTarget() instanceof Sheep&&super.canUse();}
        @Override public boolean canContinueToUse(){return WolfPacks.canHunt(wolf)&&wolf.getTarget() instanceof Sheep&&super.canContinueToUse();}
        @Override public void start(){super.start();wolf.getNavigation().stop();assemblePath=0;((WolfState)wolf).ffm$setHunting(true);}
        @Override public void tick(){
            if(wolf.getTarget() instanceof Sheep sheep) {
                if(!WolfPacks.ready(wolf,sheep)) {
                    ((WolfState)wolf).ffm$setStalking(false);
                    Wolf leader=WolfPacks.assemblyLeader(wolf);
                    if(leader!=wolf && wolf.distanceToSqr(leader)>16) {
                        if(--assemblePath<=0){assemblePath=10;wolf.getNavigation().moveTo(leader,1.0);}
                    }
                    else wolf.getNavigation().stop();
                    return;
                }
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
    /** Wild companions follow a stable leader; higher-priority danger and combat goals win. */
    public static final class Regroup extends Goal {
        private final Wolf wolf;
        private Wolf leader;
        private int scan,repath;
        public Regroup(Wolf wolf){this.wolf=wolf;setFlags(EnumSet.of(Flag.MOVE));}
        private boolean free(){return !wolf.isTame()&&!wolf.isLeashed()&&!wolf.isPassenger()
                &&!wolf.isVehicle()&&wolf.getTarget()==null&&!wolf.isAngry();}
        @Override public boolean canUse(){
            if(!free()||scan-- >0)return false;scan=reducedTickDelay(20);
            leader=WolfPacks.members(wolf,48).stream().filter(w->!w.isNoAi()&&!w.isLeashed()&&!w.isPassenger()&&!w.isVehicle())
                    .min(java.util.Comparator.comparing(Wolf::getUUID)).orElse(wolf);
            return leader!=wolf&&wolf.distanceToSqr(leader)>36;
        }
        @Override public boolean canContinueToUse(){return free()&&leader!=null&&leader.isAlive()&&!leader.isTame()
                &&wolf.distanceToSqr(leader)>9&&wolf.distanceToSqr(leader)<48*48;}
        @Override public void start(){repath=0;}
        @Override public void tick(){if(--repath<=0){repath=adjustedTickDelay(10);wolf.getNavigation().moveTo(leader,1.0);}}
        @Override public void stop(){leader=null;wolf.getNavigation().stop();}
    }
    private WolfHuntGoals(){}
}
