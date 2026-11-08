package net.itsrileynj.fantasyfeudalismmobs;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

public final class BirdGoals {
    private BirdGoals() {}
    public static boolean landingSpot(Level level,BlockPos pos) {
        if(!level.hasChunkAt(pos)||!level.getFluidState(pos).isEmpty())return false;
        var at=level.getBlockState(pos);var below=level.getBlockState(pos.below());
        return at.getCollisionShape(level,pos).isEmpty()&&level.getBlockState(pos.above()).getCollisionShape(level,pos.above()).isEmpty()
                &&(below.is(BlockTags.LEAVES)||below.isFaceSturdy(level,pos.below(),Direction.UP));
    }
    public static final class Flight extends Goal {
        private final ForestBird bird;
        private Vec3 threat,target;
        private long nextThreat,nextPath,fleeUntil,endAt,nextLanding,takeoffUntil;
        private boolean landing,finished;
        public Flight(ForestBird bird) { this.bird=bird;setFlags(EnumSet.of(Flag.MOVE)); }
        private void threat() {
            long now=bird.level().getGameTime();if(now<nextThreat)return;nextThreat=now+5;
            Player player=bird.nearbyThreat();
            if(player!=null) { threat=player.position();fleeUntil=now+60; }
            else if(bird.getLastHurtByMob()!=null&&bird.tickCount-bird.getLastHurtByMobTimestamp()<60) {
                threat=bird.getLastHurtByMob().position();fleeUntil=now+60;
            }
        }
        @Override public boolean canUse() {
            if(bird.isPassenger()||bird.isVehicle()||bird.isLeashed())return false;
            if(bird.onGround()&&!BirdAltitude.allowed(bird,bird.getX(),bird.getY(),bird.getZ()))return false;
            threat();long now=bird.level().getGameTime();
            return now<fleeUntil||bird.flightActive()||(!bird.onGround()&&bird.tickCount>10&&!bird.settling())||now>=bird.nextFlight();
        }
        @Override public boolean canContinueToUse() { return !finished&&bird.isAlive()&&!bird.isPassenger()&&!bird.isVehicle()&&!bird.isLeashed()&&bird.level().getGameTime()<endAt; }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() {
            finished=false;landing=false;target=null;nextPath=nextLanding=0;endAt=bird.level().getGameTime()+240;takeoffUntil=bird.level().getGameTime()+40;
            bird.setFlying(true);if(bird.onGround())bird.setDeltaMovement(bird.getDeltaMovement().add(0,.25,0));
        }
        private Vec3 airTarget() {
            Vec3 away=threat!=null&&bird.level().getGameTime()<fleeUntil?bird.position().subtract(threat).multiply(1,0,1):bird.flightHint()!=null?bird.flightHint().subtract(bird.position()).multiply(1,0,1):new Vec3(bird.getRandom().nextDouble()-.5,0,bird.getRandom().nextDouble()-.5);
            if(away.lengthSqr()<.01)away=new Vec3(1,0,0);away=away.normalize();
            for(int i=0;i<4;i++) {
                double angle=(bird.getRandom().nextDouble()-.5)*1.2;
                Vec3 direction=away.yRot((float)angle);
                Vec3 candidate=bird.position().add(direction.scale(7+bird.getRandom().nextInt(5))).add(0,2+bird.getRandom().nextInt(3),0);
                candidate=new Vec3(candidate.x,Math.min(candidate.y,BirdAltitude.maxFeet(bird,candidate.x,candidate.z)-.2),candidate.z);
                BlockPos pos=BlockPos.containing(candidate);
                if(pos.getY()>=bird.level().getMaxBuildHeight()-2||!bird.level().hasChunkAt(pos)||!bird.level().isEmptyBlock(pos)||!bird.level().isEmptyBlock(pos.above()))continue;
                var path=bird.getNavigation().createPath(pos,0);
                if(path!=null&&path.canReach()) { bird.getNavigation().moveTo(path,1);return candidate; }
                var hit=bird.level().clip(new ClipContext(bird.getEyePosition(),candidate,ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,bird));
                if(hit.getType()==HitResult.Type.MISS&&BirdAltitude.segmentAllowed(bird,bird.position(),candidate)) { bird.getMoveControl().setWantedPosition(candidate.x,candidate.y,candidate.z,1);return candidate; }
            }
            return null;
        }
        private Vec3 landingTarget() {
            BlockPos base=bird.blockPosition();
            for(int n=0;n<10;n++) {
                int x=base.getX()+(n==0?0:bird.getRandom().nextInt(13)-6),z=base.getZ()+(n==0?0:bird.getRandom().nextInt(13)-6);
                for(int y=Math.min(base.getY()+1,bird.level().getMaxBuildHeight()-2);y>=Math.max(base.getY()-16,bird.level().getMinBuildHeight()+1);y--) {
                    BlockPos pos=new BlockPos(x,y,z);
                    if(!bird.level().hasChunkAt(pos))break;
                    if(landingSpot(bird.level(),pos)&&BirdAltitude.allowed(bird,pos.getX()+.5,pos.getY(),pos.getZ()+.5)) {
                        var path=bird.getNavigation().createPath(pos,0);
                        if(path!=null&&path.canReach()) { bird.getNavigation().moveTo(path,.8);return Vec3.atBottomCenterOf(pos); }
                    }
                }
            }
            return null;
        }
        @Override public void tick() {
            long now=bird.level().getGameTime();threat();
            boolean escaping=now<fleeUntil;
            if(escaping)endAt=now+240;
            if(escaping&&landing) { landing=false;target=null; }
            if(!escaping&&now>=takeoffUntil&&now>=nextLanding&&!landing) {
                nextLanding=now+20;Vec3 spot=landingTarget();
                if(spot!=null) { landing=true;target=spot;nextPath=now+20; }
            }
            if(landing&&target!=null) {
                if(bird.position().distanceToSqr(target)<.35*.35&&landingSpot(bird.level(),BlockPos.containing(target))) {
                    bird.setFlying(false);bird.setDeltaMovement(0,-.08,0);finished=true;return;
                }
                if(now>=nextPath) { nextPath=now+20;bird.getNavigation().moveTo(target.x,target.y,target.z,.8); }
            } else if(now>=nextPath&&(target==null||bird.position().distanceToSqr(target)<4||bird.getNavigation().isDone())) {
                nextPath=now+20;target=airTarget();
            } else if(target!=null&&bird.getNavigation().isDone()) {
                bird.getMoveControl().setWantedPosition(target.x,target.y,target.z,1);
            }
        }
        @Override public void stop() { bird.getNavigation().stop();bird.setFlying(false);bird.scheduleFlight();target=threat=null; }
    }
    public static final class QuickLook extends Goal {
        private final ForestBird bird;
        private int hold;
        private double x,y,z;
        public QuickLook(ForestBird bird) { this.bird=bird;setFlags(EnumSet.of(Flag.LOOK)); }
        @Override public boolean canUse() { return !bird.isPassenger(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            if(--hold<=0) {
                hold=6+bird.getRandom().nextInt(13);
                double yaw=Math.toRadians(bird.getYRot()+bird.getRandom().nextInt(141)-70);
                x=bird.getX()-Math.sin(yaw)*4;z=bird.getZ()+Math.cos(yaw)*4;y=bird.getEyeY()+(bird.getRandom().nextDouble()-.5)*1.2;
            }
            bird.getLookControl().setLookAt(x,y,z,90,70);
        }
    }
    /** Walk only a couple of blocks between the more frequent flights. */
    public static final class ShortWalk extends WaterAvoidingRandomStrollGoal {
        private final ForestBird bird;
        private long nextWalk;
        public ShortWalk(ForestBird bird) { super(bird,.8);this.bird=bird;nextWalk=bird.level().getGameTime()+20; }
        @Override public boolean canUse() {
            long now=bird.level().getGameTime();
            if(!bird.onGround()||bird.flightActive()||bird.isLeashed()||bird.isPassenger()||now<nextWalk)return false;
            trigger();boolean ready=super.canUse();if(!ready)nextWalk=now+20;return ready;
        }
        @Override protected Vec3 getPosition() {
            for(int i=0;i<4;i++) {
                Vec3 at=DefaultRandomPos.getPos(bird,2,1);
                if(at!=null) {
                    double distance=at.subtract(bird.position()).multiply(1,0,1).lengthSqr();
                    if(distance>=.8&&distance<=4.01)return at;
                }
            }
            return null;
        }
        @Override public void start() { super.start();nextWalk=bird.level().getGameTime()+80+bird.getRandom().nextInt(41); }
        @Override public void stop() { super.stop();nextWalk=Math.max(nextWalk,bird.level().getGameTime()+20); }
    }
    /** Low-frequency flock planning requests a flight rather than a long ground walk. */
    public static final class Flock extends Goal {
        private final ForestBird bird;
        private long scan;
        public Flock(ForestBird bird) { this.bird=bird; }
        @Override public boolean canUse() {
            long now=bird.level().getGameTime();
            if(bird.flock()==null||!bird.onGround()||bird.isFlying()||bird.isLeashed()||bird.isPassenger()||now<scan)return false;
            scan=now+100+Math.floorMod(bird.getId()-now,100);
            var leader=bird.level().getEntitiesOfClass(ForestBird.class,bird.getBoundingBox().inflate(16,6,16),b->b.isAlive()&&b.getType()==bird.getType()&&bird.flock().equals(b.flock())&&!b.isFlying())
                    .stream().min(Comparator.comparing(ForestBird::getUUID)).orElse(bird);
            if(leader!=bird&&bird.distanceToSqr(leader)>36)bird.flyToward(leader.position());
            return false;
        }
    }
}
