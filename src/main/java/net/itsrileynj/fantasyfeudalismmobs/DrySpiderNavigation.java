package net.itsrileynj.fantasyfeudalismmobs;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/** Retain wall climbing, but do not use its straight-line fallback across water. */
public final class DrySpiderNavigation extends WallClimberNavigation {
    private final Spider spider;
    private BlockPos destination;
    private long nextCheck;
    private boolean dryFallback;
    public DrySpiderNavigation(Spider spider,Level level) {super(spider,level);this.spider=spider;}
    @Override public Path createPath(BlockPos pos,int accuracy) {
        if(!pos.equals(destination))nextCheck=0;destination=pos.immutable();return super.createPath(pos,accuracy);
    }
    @Override public Path createPath(Entity entity,int accuracy) {
        BlockPos pos=entity.blockPosition();if(!pos.equals(destination))nextCheck=0;destination=pos;return super.createPath(entity,accuracy);
    }
    @Override public void tick() {
        if(isDone()&&destination!=null) {
            double close=Math.max(spider.getBbWidth(),1);
            if(destination.closerToCenterThan(spider.position(),close)
                    ||spider.getY()>destination.getY()&&BlockPos.containing(destination.getX(),spider.getY(),destination.getZ()).closerToCenterThan(spider.position(),close))destination=null;
        }
        if(isDone()&&destination!=null&&!spider.isInWater()&&spider.getPathfindingMalus(PathType.WATER)<0) {
            if(spider.level().getGameTime()>=nextCheck) {
                nextCheck=spider.level().getGameTime()+20;dryFallback=true;
                Vec3 end=Vec3.atBottomCenterOf(destination);
                int samples=Math.min(128,(int)Math.ceil(spider.position().distanceTo(end)*2));
                for(int i=1;i<=samples;i++) {
                    BlockPos pos=BlockPos.containing(spider.position().lerp(end,(double)i/samples));
                    if(!spider.level().hasChunkAt(pos)||!spider.level().getFluidState(pos).isEmpty()
                            ||!spider.level().getFluidState(pos.below()).isEmpty()){dryFallback=false;break;}
                }
            }
            if(!dryFallback) {
                spider.getMoveControl().setWantedPosition(spider.getX(),spider.getY(),spider.getZ(),0);return;
            }
        }
        super.tick();
    }
}
