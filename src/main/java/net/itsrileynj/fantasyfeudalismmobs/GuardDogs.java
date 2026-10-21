package net.itsrileynj.fantasyfeudalismmobs;

import java.util.Comparator;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.*;

/** Server-side livestock protection; fatigue uses health without a damage transaction. */
public final class GuardDogs {
    public static final double RANGE=24;
    public static final float FATIGUE=2;
    public static final long FOX_COOLDOWN=24000;
    private static final String FOX_MEAL="ffm_fox_meal_until";

    public static void install(Mob mob){
        if(!(mob instanceof Wolf)&&!(mob instanceof Fox))return;
        if(mob.goalSelector.getAvailableGoals().stream().anyMatch(entry->entry.getGoal() instanceof AvoidProtected<?>))return;
        if(mob instanceof Wolf wolf)mob.goalSelector.addGoal(4,new AvoidProtected<>(wolf,Sheep.class));
        else mob.goalSelector.addGoal(4,new AvoidProtected<>((Fox)mob,Chicken.class));
    }

    public static final class AvoidProtected<T extends LivingEntity> extends AvoidEntityGoal<T> {
        private int scan;
        public AvoidProtected(PathfinderMob hunter,Class<T> prey){super(hunter,prey,8,1.2,1.4,target->guard(target)!=null);}
        private boolean idle(){return mob.getTarget()==null && (!(mob instanceof Wolf wolf)||!wolf.isTame());}
        @Override public boolean canUse(){
            if(!idle()||scan-- >0)return false;scan=reducedTickDelay(10);return super.canUse();
        }
        @Override public boolean canContinueToUse(){return idle()&&toAvoid!=null&&toAvoid.isAlive()
                &&guard(toAvoid)!=null&&super.canContinueToUse();}
        @Override public void stop(){super.stop();mob.getNavigation().stop();}
    }

    public static Wolf guard(LivingEntity prey) {
        return prey.level().getEntitiesOfClass(Wolf.class,prey.getBoundingBox().inflate(RANGE),
                dog->dog.isAlive()&&dog.isTame()&&dog.isOrderedToSit()&&dog.isInSittingPose()
                        &&dog.distanceToSqr(prey)<=RANGE*RANGE)
                .stream().min(Comparator.<Wolf>comparingDouble(prey::distanceToSqr).thenComparing(Wolf::getUUID)).orElse(null);
    }

    public static long foxDeadline(Fox fox){return fox.getPersistentData().getLong(FOX_MEAL);}
    public static boolean foxHungry(Fox fox){return foxDeadline(fox)<=fox.level().getGameTime();}
    public static boolean foxFood(LivingEntity prey){
        return prey instanceof Chicken||prey instanceof Rabbit||prey instanceof AbstractSchoolingFish
                ||prey instanceof Turtle&&Turtle.BABY_ON_LAND_SELECTOR.test(prey);
    }
    public static void feedFox(Fox fox){
        fox.getPersistentData().putLong(FOX_MEAL,fox.level().getGameTime()+FOX_COOLDOWN);
        clearFox(fox);
    }
    private static void clearFox(Fox fox){
        if(foxFood(fox.getTarget()))fox.setTarget(null);
        fox.getNavigation().stop();fox.setIsCrouching(false);fox.setIsInterested(false);fox.setIsPouncing(false);
    }

    /** Called only on an actual target/attack attempt, never by a visibility predicate. */
    public static boolean blocks(Mob hunter,LivingEntity prey){
        if(hunter.level().isClientSide||prey==null)return false;
        boolean wolfHunt=hunter instanceof Wolf wolf&&!wolf.isTame()&&prey instanceof Sheep;
        boolean foxHunt=hunter instanceof Fox&&prey instanceof Chicken;
        if(hunter instanceof Fox fox&&foxFood(prey)&&!foxHungry(fox))return true;
        if(!wolfHunt&&!foxHunt)return false;
        if(hunter instanceof Wolf wolf&&!WolfPacks.hungry(wolf))return true;
        Wolf dog=guard(prey);if(dog==null)return false;
        // Commit the hunter's cooldown first: recruiting the rest of a pack cannot charge the dog again.
        if(hunter instanceof Wolf wolf)WolfPacks.ateSheep(wolf);
        else feedFox((Fox)hunter);
        dog.setHealth(Math.max(1,dog.getHealth()-FATIGUE));
        return true;
    }

    public static void beforeTick(Mob hunter){
        LivingEntity prey=hunter.getTarget();
        if(blocks(hunter,prey)) {
            if(hunter instanceof Wolf wolf)WolfPacks.clearHunt(wolf);
            else if(hunter instanceof Fox fox)clearFox(fox);
        }
    }
    private GuardDogs(){}
}
