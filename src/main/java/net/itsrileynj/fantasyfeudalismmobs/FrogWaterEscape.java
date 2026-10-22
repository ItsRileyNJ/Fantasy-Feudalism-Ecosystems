package net.itsrileynj.fantasyfeudalismmobs;

import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.LongJumpUtil;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Short, server-owned escape; native brain activity resumes after landing or entering water. */
public final class FrogWaterEscape {
    public static final double APPROACH_RANGE=6;
    public static final int WATER_RANGE=12;
    private BlockPos water;
    private long nextScan,until,launchedAt;
    private boolean jumping;
    private int repath;

    private static boolean busy(Frog frog){return frog.isLeashed()||frog.isPassenger()||frog.isVehicle()
            ||frog.isInLove()||frog.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET)
            ||frog.getBrain().hasMemoryValue(MemoryModuleType.IS_PREGNANT);}
    public static Player threat(Frog frog){
        return frog.level().players().stream().filter(p->p.isAlive()&&!p.isSpectator()
                &&frog.distanceToSqr(p)<=APPROACH_RANGE*APPROACH_RANGE&&frog.hasLineOfSight(p))
                .min(Comparator.comparingDouble(frog::distanceToSqr)).orElse(null);
    }
    private static boolean openWater(ServerLevel level,Frog frog,BlockPos at){
        return level.hasChunkAt(at)&&level.hasChunkAt(at.above())
                &&level.getFluidState(at).is(FluidTags.WATER)&&!level.getFluidState(at.above()).is(FluidTags.WATER)
                &&level.getBlockState(at).getCollisionShape(level,at).isEmpty()
                &&level.getBlockState(at.above()).getCollisionShape(level,at.above()).isEmpty()
                &&level.noCollision(frog,frog.getDimensions(Pose.STANDING).makeBoundingBox(Vec3.atBottomCenterOf(at).add(0,.2,0)));
    }
    private static Vec3 jump(Frog frog,BlockPos at){
        Vec3 target=Vec3.atBottomCenterOf(at).add(0,1.05,0);
        if(frog.position().distanceToSqr(target)>25||Math.abs(target.y-frog.getY())>2.5)return null;
        float maximum=(float)(frog.getAttributeValue(Attributes.JUMP_STRENGTH)*3.5714288);
        for(int angle:new int[]{45,55,65}) {
            var vector=LongJumpUtil.calculateJumpVectorForAngle(frog,target,maximum,angle,true);
            if(vector.isPresent())return vector.get();
        }
        return null;
    }
    private BlockPos findWater(ServerLevel level,Frog frog,Player player){
        var candidates=new ArrayList<BlockPos>();BlockPos origin=frog.blockPosition();
        for(var at:BlockPos.betweenClosed(origin.offset(-WATER_RANGE,-3,-WATER_RANGE),origin.offset(WATER_RANGE,2,WATER_RANGE))) {
            if(at.distSqr(origin)<=WATER_RANGE*WATER_RANGE&&openWater(level,frog,at))candidates.add(at.immutable());
        }
        candidates.sort(Comparator.comparingDouble(at->frog.distanceToSqr(Vec3.atCenterOf(at))
                +(player.distanceToSqr(Vec3.atCenterOf(at))<frog.distanceToSqr(player)?16:0)));
        for(var at:candidates.stream().limit(16).toList()) {
            if(jump(frog,at)!=null)return at;
            // Aim for a launch spot on the shore, rather than requiring a land path into water.
            Path path=frog.getNavigation().createPath(at.above(),4);
            if(path!=null&&path.canReach())return at;
        }
        return null;
    }
    public boolean tick(Frog frog){
        if(!(frog.level() instanceof ServerLevel level))return false;
        long now=level.getGameTime();
        if(water==null) {
            if(now<nextScan||frog.isInWaterOrBubble()||!frog.onGround()||busy(frog)
                    ||frog.getPose()!=Pose.STANDING||frog.getBrain().hasMemoryValue(MemoryModuleType.LONG_JUMP_MID_JUMP))return false;
            nextScan=now+10;Player player=threat(frog);if(player==null)return false;
            water=findWater(level,frog,player);if(water==null){nextScan=now+80;return false;}
            frog.getBrain().stopAll(level,frog);
            frog.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            frog.getBrain().eraseMemory(MemoryModuleType.PATH);
            frog.getNavigation().stop();until=now+200;repath=0;
        }
        if(jumping) {
            if(frog.isInWaterOrBubble()||(now>launchedAt+2&&frog.onGround())||now>=until)finish(frog,now);
            return true;
        }
        if(frog.isInWaterOrBubble()||now>=until||busy(frog)||!openWater(level,frog,water)) {
            finish(frog,now);return true;
        }
        if(frog.onGround()) {
            Vec3 velocity=jump(frog,water);
            if(velocity!=null) {
                frog.getNavigation().stop();frog.setPose(Pose.LONG_JUMPING);frog.setDiscardFriction(true);
                float yaw=(float)(Math.toDegrees(Math.atan2(velocity.z,velocity.x))-90);
                frog.setYRot(yaw);frog.setYBodyRot(yaw);frog.setYHeadRot(yaw);
                frog.setDeltaMovement(velocity);frog.hasImpulse=true;jumping=true;launchedAt=now;
                level.playSound(null,frog,SoundEvents.FROG_LONG_JUMP,SoundSource.NEUTRAL,1,1);return true;
            }
        }
        if(--repath<=0) {
            repath=10;Path path=frog.getNavigation().createPath(water.above(),4);
            if(path==null||!path.canReach()){finish(frog,now);return true;}
            if(!frog.getNavigation().moveTo(path,1.5))finish(frog,now);
        }
        return true;
    }
    private void finish(Frog frog,long now){
        frog.getNavigation().stop();frog.setDiscardFriction(false);
        if(jumping)frog.setPose(Pose.STANDING);
        jumping=false;water=null;nextScan=now+40;
    }
}
