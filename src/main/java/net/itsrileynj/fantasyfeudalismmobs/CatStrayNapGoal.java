package net.itsrileynj.fantasyfeudalismmobs;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.tags.BlockTags;

/** Strays can join a sleeper too; owned cats retain vanilla resting and morning gifts. */
public final class CatStrayNapGoal extends Goal {
    private final Cat cat;
    private Player sleeper;
    private BlockPos bed;
    private int settled;
    public CatStrayNapGoal(Cat cat){this.cat=cat;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
    @Override public boolean canUse(){
        if(cat.isTame()||cat.isOrderedToSit()||cat.getTarget()!=null)return false;
        sleeper=cat.level().getEntitiesOfClass(Player.class,cat.getBoundingBox().inflate(4),
                p->CatRest.candidate(cat,p)&&cat.distanceToSqr(p)<=16).stream().min(Comparator.comparingDouble(cat::distanceToSqr)).orElse(null);
        if(sleeper==null)return false;
        bed=sleeper.getSleepingPos().orElse(null);if(bed==null)return false;
        var state=cat.level().getBlockState(bed);
        if(!state.is(BlockTags.BEDS))return false;
        bed=bed.relative(state.getValue(BedBlock.FACING).getOpposite());
        return !occupied();
    }
    private boolean occupied(){return !cat.level().getEntitiesOfClass(Cat.class,new net.minecraft.world.phys.AABB(bed).inflate(1),
            other->other!=cat&&other.isLying()).isEmpty();}
    @Override public boolean canContinueToUse(){return !cat.isTame()&&sleeper!=null&&CatRest.candidate(cat,sleeper)
            &&cat.distanceToSqr(sleeper)<=36&&!occupied();}
    @Override public void start(){settled=0;cat.setInSittingPose(false);}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){
        cat.getLookControl().setLookAt(sleeper,45,45);
        cat.getNavigation().moveTo(bed.getX()+.5,bed.getY()+.5625,bed.getZ()+.5,1.1);
        if(cat.distanceToSqr(sleeper)<2.5&&cat.getY()>=bed.getY()+.35){
            if(++settled>=16){cat.setLying(true);cat.getNavigation().stop();}
        }else{settled=0;cat.setLying(false);}
    }
    @Override public void stop(){cat.setLying(false);cat.getNavigation().stop();sleeper=null;}
}
