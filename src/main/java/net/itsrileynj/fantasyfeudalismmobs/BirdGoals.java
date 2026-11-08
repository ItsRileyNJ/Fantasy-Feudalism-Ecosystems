package net.itsrileynj.fantasyfeudalismmobs;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

public final class BirdGoals {
    private BirdGoals() {}
    public static boolean landingSpot(Level level,BlockPos pos) {
        if(!level.hasChunkAt(pos)||!level.getFluidState(pos).isEmpty())return false;
        var at=level.getBlockState(pos);var below=level.getBlockState(pos.below());
        // Passable leaves are traversable, but the interior of a canopy is not a perch.
        if(at.is(BlockTags.LEAVES)||level.getBlockState(pos.above()).is(BlockTags.LEAVES))return false;
        return at.getCollisionShape(level,pos).isEmpty()&&level.getBlockState(pos.above()).getCollisionShape(level,pos.above()).isEmpty()
                &&(below.is(BlockTags.LEAVES)||below.isFaceSturdy(level,pos.below(),Direction.UP));
    }
    public static final class Flight extends Goal {
        private final ForestBird bird;
        private Vec3 threat,target;
        private long nextThreat,nextPath,fleeUntil,endAt,nextLanding,takeoffUntil,landingDeadline,progressAt;
        private Vec3 progressPosition;
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
            return now<fleeUntil||bird.flightActive()||(!bird.onGround()&&bird.tickCount>10&&!bird.settling())||ambientReady(now);
        }
        private boolean ambientReady(long now) {
            if(bird.kind()!=BirdKind.ROBIN)return now>=bird.nextFlight();
            if(bird.settling()||!bird.pairCanFly())return false;
            var other=bird.partner();
            if(other.landingFlight())return false;
            if(bird.onGround()&&bird.landingDestination()!=null&&other.flightActive()&&bird.cruiseUntil()==other.cruiseUntil())return false;
            return other.flightActive()||now>=Math.min(bird.nextFlight(),other.nextFlight());
        }
        @Override public boolean canContinueToUse() { return !finished&&bird.isAlive()&&!bird.isPassenger()&&!bird.isVehicle()&&!bird.isLeashed()&&bird.level().getGameTime()<endAt; }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() {
            finished=false;landing=false;bird.setLandingFlight(false);bird.setLandingDestination(null);target=null;nextPath=nextLanding=0;long now=bird.level().getGameTime();var partner=bird.partner();
            takeoffUntil=partner!=null&&partner.flightActive()?Math.max(now+20,partner.cruiseUntil()):now+400+bird.getRandom().nextInt(301);
            bird.setCruiseUntil(takeoffUntil);endAt=takeoffUntil+200;
            if(bird.kind()==BirdKind.ROBIN&&bird.pairCanFly()&&!bird.partner().flightActive())bird.partner().flyToward(bird.position());
            bird.setFlying(true);if(bird.onGround())bird.setDeltaMovement(bird.getDeltaMovement().add(0,.25,0));
        }
        private Vec3 airTarget() {
            var partner=bird.partner();
            if(partner!=null&&partner.flightActive()&&(bird.followsPartner()||bird.distanceToSqr(partner)>36)) {
                Vec3 nearby=partner.position().add(1.2,0,0);
                nearby=new Vec3(nearby.x,Math.min(nearby.y,BirdAltitude.maxFeet(bird,nearby.x,nearby.z)-.2),nearby.z);
                var path=bird.getNavigation().createPath(BlockPos.containing(nearby),0);
                if(path!=null&&path.canReach()) { bird.getNavigation().moveTo(path,1.1);return nearby; }
                if(BirdAltitude.segmentAllowed(bird,bird.position(),nearby)
                        &&bird.level().clip(new ClipContext(bird.getEyePosition(),nearby,ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,bird)).getType()==HitResult.Type.MISS) {
                    bird.getMoveControl().setWantedPosition(nearby.x,nearby.y,nearby.z,1.1);return nearby;
                }
            }
            Vec3 away=threat!=null&&bird.level().getGameTime()<fleeUntil?bird.position().subtract(threat).multiply(1,0,1):bird.flightHint()!=null?bird.flightHint().subtract(bird.position()).multiply(1,0,1):new Vec3(bird.getRandom().nextDouble()-.5,0,bird.getRandom().nextDouble()-.5);
            if(away.lengthSqr()<.01)away=new Vec3(1,0,0);away=away.normalize();
            for(int i=0;i<4;i++) {
                double angle=(bird.getRandom().nextDouble()-.5)*1.2;
                Vec3 direction=away.yRot((float)angle);
                Vec3 horizontal=bird.position().add(direction.scale(8+bird.getRandom().nextInt(5)));
                double cruiseY=BirdAltitude.surfaceY(bird.level(),net.minecraft.util.Mth.floor(horizontal.x),net.minecraft.util.Mth.floor(horizontal.z))+6+bird.getRandom().nextInt(7);
                Vec3 candidate=new Vec3(horizontal.x,cruiseY,horizontal.z);
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
            var partner=bird.partner();
            Vec3 anchor=bird.followsPartner()&&partner.distanceToSqr(bird)<256
                    ?(partner.landingDestination()!=null?partner.landingDestination():partner.position()):bird.position();
            BlockPos base=BlockPos.containing(anchor);
            var spots=new ArrayList<BlockPos>();
            for(int n=0;n<12;n++) {
                int x=base.getX()+(n==0?(bird.followsPartner()?2:0):bird.getRandom().nextInt(21)-10),z=base.getZ()+(n==0?0:bird.getRandom().nextInt(21)-10);
                if(!bird.level().hasChunkAt(new BlockPos(x,base.getY(),z)))continue;
                int surface=BirdAltitude.surfaceY(bird.level(),x,z);
                int top=Math.min(bird.level().getMaxBuildHeight()-2,(int)Math.floor(BirdAltitude.maxFeet(bird,x+.5,z+.5)));
                for(int y=top;y>=Math.max(surface,bird.level().getMinBuildHeight()+1);y--) {
                    BlockPos pos=new BlockPos(x,y,z);
                    if(bird.followsPartner()&&Vec3.atBottomCenterOf(pos).subtract(anchor).multiply(1,0,1).lengthSqr()>36)continue;
                    if(partner!=null&&Vec3.atBottomCenterOf(pos).subtract(partner.position()).multiply(1,0,1).lengthSqr()<2.25)continue;
                    if(landingSpot(bird.level(),pos)) { spots.add(pos);break; }
                }
            }
            // Prefer the highest reachable support (including leaves) rather than immediately choosing bare ground.
            spots.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).reversed()
                    .thenComparingDouble(pos->Vec3.atBottomCenterOf(pos).distanceToSqr(bird.position())));
            for(int i=0;i<Math.min(6,spots.size());i++) {
                BlockPos pos=spots.get(i);var path=bird.getNavigation().createPath(pos,0);
                if(path!=null&&path.canReach()) { bird.getNavigation().moveTo(path,.8);return Vec3.atBottomCenterOf(pos); }
            }
            return null;
        }
        @Override public void tick() {
            long now=bird.level().getGameTime();threat();
            boolean escaping=now<fleeUntil;
            if(escaping)endAt=Math.max(endAt,now+240);
            if(escaping&&landing) { landing=false;bird.setLandingFlight(false);target=null; }
            if(!escaping&&now>=takeoffUntil&&now>=nextLanding&&!landing) {
                nextLanding=now+20;Vec3 spot=landingTarget();
                if(spot!=null) { landing=true;bird.setLandingFlight(true);bird.setLandingDestination(spot);target=spot;nextPath=now+20;landingDeadline=now+160;progressAt=now;progressPosition=bird.position(); }
            }
            if(landing&&target!=null) {
                if(!landingSpot(bird.level(),BlockPos.containing(target))||now>=landingDeadline) {
                    abandonLanding(now);return;
                }
                Vec3 delta=target.subtract(bird.position());
                // Native flight paths arrive at the center of the air block, half a block above the support.
                // Release gravity for the last clear descent rather than repeatedly repathing to that center.
                if(delta.x*delta.x+delta.z*delta.z<.65*.65&&delta.y<=.15&&delta.y>=-1.2
                        &&bird.level().clip(new ClipContext(bird.position(),target,ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,bird)).getType()==HitResult.Type.MISS) {
                    bird.setFlying(false);bird.setDeltaMovement(0,-.08,0);finished=true;return;
                }
                if(now-progressAt>=40) {
                    if(bird.position().distanceToSqr(progressPosition)<.04) { abandonLanding(now);return; }
                    progressAt=now;progressPosition=bird.position();
                }
                if(now>=nextPath) { nextPath=now+20;bird.getNavigation().moveTo(target.x,target.y,target.z,.8); }
                if(bird.getNavigation().isDone())bird.getMoveControl().setWantedPosition(target.x,target.y,target.z,.8);
            } else if(now>=nextPath&&(bird.followsPartner()||target==null||bird.position().distanceToSqr(target)<4||bird.getNavigation().isDone())) {
                nextPath=now+20;target=airTarget();
            } else if(target!=null&&bird.getNavigation().isDone()) {
                bird.getMoveControl().setWantedPosition(target.x,target.y,target.z,1);
            }
        }
        private void abandonLanding(long now) {
            landing=false;bird.setLandingFlight(false);bird.setLandingDestination(null);target=null;bird.getNavigation().stop();nextLanding=now+20;nextPath=now;
        }
        @Override public void stop() { bird.setLandingFlight(false);bird.getNavigation().stop();bird.setFlying(false);bird.scheduleFlight();target=threat=null; }
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
            BlockPos origin=bird.blockPosition();
            for(int i=0;i<4;i++) {
                int x=origin.getX()+bird.getRandom().nextInt(5)-2,z=origin.getZ()+bird.getRandom().nextInt(5)-2;
                for(int y=origin.getY()+1;y>=origin.getY()-1;y--) {
                    var pos=new BlockPos(x,y,z);Vec3 at=Vec3.atBottomCenterOf(pos);
                    double distance=at.subtract(bird.position()).multiply(1,0,1).lengthSqr();
                    if(distance<.8||distance>4.01||!landingSpot(bird.level(),pos))continue;
                    var path=bird.getNavigation().createPath(pos,0);
                    if(path!=null&&path.canReach())return at;
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
            if(bird.kind()==BirdKind.ROBIN||bird.flock()==null||!bird.onGround()||bird.isFlying()||bird.isLeashed()||bird.isPassenger()||now<scan)return false;
            scan=now+100+Math.floorMod(bird.getId()-now,100);
            var leader=bird.level().getEntitiesOfClass(ForestBird.class,bird.getBoundingBox().inflate(16,6,16),b->b.isAlive()&&b.getType()==bird.getType()&&bird.flock().equals(b.flock())&&!b.isFlying())
                    .stream().min(Comparator.comparing(ForestBird::getUUID)).orElse(bird);
            if(leader!=bird&&bird.distanceToSqr(leader)>36)bird.flyToward(leader.position());
            return false;
        }
    }
}
