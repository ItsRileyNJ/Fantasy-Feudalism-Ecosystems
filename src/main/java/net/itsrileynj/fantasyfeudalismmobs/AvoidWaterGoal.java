package net.itsrileynj.fantasyfeudalismmobs;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/** Prefer dry routes; temporarily permit water nodes only to get out after falling in. */
public final class AvoidWaterGoal extends Goal {
    private final Mob mob;
    private Path escape;
    private long nextSearch,started;
    private float originalWaterCost,originalBorderCost;

    public AvoidWaterGoal(Mob mob) {this.mob=mob;setFlags(EnumSet.of(Flag.MOVE));}

    public static void configure(Mob mob) {
        mob.setPathfindingMalus(PathType.WATER,-1);
        mob.setPathfindingMalus(PathType.WATER_BORDER,8);
    }

    @Override public boolean canUse() {
        if(!mob.isInWater()||mob.isPassenger()||mob.level().getGameTime()<nextSearch)return false;
        nextSearch=mob.level().getGameTime()+40;
        originalWaterCost=mob.getPathfindingMalus(PathType.WATER);
        originalBorderCost=mob.getPathfindingMalus(PathType.WATER_BORDER);
        mob.setPathfindingMalus(PathType.WATER,0);
        mob.setPathfindingMalus(PathType.WATER_BORDER,0);
        int paths=0;
        try {
            for(BlockPos pos:BlockPos.withinManhattan(mob.blockPosition(),6,3,6)) {
                if(pos.getY()<mob.getBlockY()||!mob.level().hasChunkAt(pos)||!mob.level().getFluidState(pos).isEmpty()
                        ||!mob.level().getFluidState(pos.below()).isEmpty()
                        ||mob.level().getBlockState(pos.below()).getCollisionShape(mob.level(),pos.below()).isEmpty())continue;
                var box=mob.getBoundingBox().move(Vec3.atBottomCenterOf(pos).subtract(mob.position()));
                if(mob.level().containsAnyLiquid(box.expandTowards(0,-.5,0))||!mob.level().noCollision(mob,box))continue;
                escape=mob.getNavigation().createPath(pos.immutable(),0);
                if(escape!=null&&escape.canReach())return true;
                if(++paths>=4)break;
            }
            return false;
        } finally {mob.setPathfindingMalus(PathType.WATER,originalWaterCost);mob.setPathfindingMalus(PathType.WATER_BORDER,originalBorderCost);}
    }

    @Override public void start() {
        started=mob.level().getGameTime();
        mob.setPathfindingMalus(PathType.WATER,0);
        mob.setPathfindingMalus(PathType.WATER_BORDER,0);
        mob.getNavigation().moveTo(escape,1);
    }
    @Override public boolean canContinueToUse() {
        return (!mob.getNavigation().isDone()||mob.isInWater()||!mob.onGround())&&mob.level().getGameTime()-started<200;
    }
    @Override public boolean requiresUpdateEveryTick() {return true;}
    @Override public void tick() {
        // Native waypoints can finish within a body-width of the bank; finish stepping clear.
        if((mob.isInWater()||!mob.onGround())&&escape!=null) {
            Vec3 end=Vec3.atBottomCenterOf(escape.getTarget());
            if(mob.getNavigation().isDone()||mob.distanceToSqr(end)<4) {
                mob.getNavigation().stop();
                mob.getMoveControl().setWantedPosition(end.x,end.y,end.z,1);
            }
        }
    }
    @Override public void stop() {
        nextSearch=Math.max(nextSearch,mob.level().getGameTime()+40);
        mob.setPathfindingMalus(PathType.WATER,originalWaterCost);
        mob.setPathfindingMalus(PathType.WATER_BORDER,originalBorderCost);
        mob.getNavigation().stop();escape=null;
    }
}
