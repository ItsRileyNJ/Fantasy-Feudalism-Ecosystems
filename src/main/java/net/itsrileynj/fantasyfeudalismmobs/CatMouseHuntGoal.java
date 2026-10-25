package net.itsrileynj.fantasyfeudalismmobs;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cat;

public final class CatMouseHuntGoal extends Goal {
    public static final int DETECTION_RANGE=16;
    private final Cat cat;private FieldMouse mouse;private int scan,repath;
    public CatMouseHuntGoal(Cat cat){this.cat=cat;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    private boolean free(){return !cat.isOrderedToSit()&&!cat.isInLove()
            &&(cat.getTarget()==null||cat.getTarget() instanceof FieldMouse);}
    @Override public boolean canUse(){
        if(!free()||scan-- >0)return false;scan=reducedTickDelay(10);
        mouse=cat.level().getEntitiesOfClass(FieldMouse.class,cat.getBoundingBox().inflate(DETECTION_RANGE),
                prey->prey.isAlive()&&cat.distanceToSqr(prey)<=DETECTION_RANGE*DETECTION_RANGE&&cat.hasLineOfSight(prey)).stream()
                .min(Comparator.comparingDouble(cat::distanceToSqr)).orElse(null);
        return mouse!=null;
    }
    @Override public boolean canContinueToUse(){return free()&&mouse!=null&&mouse.isAlive()&&cat.distanceToSqr(mouse)<=DETECTION_RANGE*DETECTION_RANGE;}
    @Override public void start(){repath=0;cat.setLying(false);cat.setInSittingPose(false);cat.setTarget(mouse);cat.setSprinting(true);}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){
        cat.getLookControl().setLookAt(mouse,30,30);
        if(repath--<=0){repath=10;cat.getNavigation().moveTo(mouse,FieldMouse.CAT_CHASE_SPEED);}
        if(cat.canAttack(mouse)&&cat.hasLineOfSight(mouse)&&cat.distanceToSqr(mouse)<=Math.pow(cat.getBbWidth()+mouse.getBbWidth()+.3,2))cat.doHurtTarget(mouse);
    }
    @Override public void stop(){if(cat.getTarget()==mouse)cat.setTarget(null);cat.setSprinting(false);cat.getNavigation().stop();mouse=null;}
}
