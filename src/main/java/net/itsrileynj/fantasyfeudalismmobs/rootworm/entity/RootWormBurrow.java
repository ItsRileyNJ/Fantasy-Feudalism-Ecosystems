package net.itsrileynj.fantasyfeudalismmobs.rootworm.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A short, bounded retreat through verified soft ground; never loads chunks or edits terrain. */
public final class RootWormBurrow {
    public static final TagKey<Block> DIGGABLE=TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("fantasy_feudalism_mobs","root_worm_burrowable"));
    public static final int DURATION=60, COOLDOWN=400;
    private static final double DEPTH=1.15;
    private final RootWorm worm;
    private Vec3 start, end, surface;
    private int elapsed, readyAt;

    RootWormBurrow(RootWorm worm) { this.worm=worm; }
    public boolean active() { return start!=null; }
    public Vec3 surface() { return surface; }
    public void tryStart(DamageSource source) {
        if(worm.level().isClientSide || active() || !worm.isAlive() || worm.isNoAi() || !worm.onGround()
                || worm.isPassenger() || worm.isVehicle() || worm.isLeashed() || worm.isInWaterOrBubble() || worm.isInLava()
                || worm.getHealth()>=worm.getMaxHealth()*0.5F || worm.tickCount<readyAt) return;
        var from=worm.position();
        if(!safeColumn(from))return;
        Vec3 threat=source.getSourcePosition();
        Vec3 away=threat==null?Vec3.ZERO:from.subtract(threat).multiply(1,0,1).normalize();
        double angle=away.lengthSqr()<0.01?worm.getRandom().nextDouble()*Math.PI*2:Math.atan2(away.z,away.x);
        // Prefer directly away, then fan out. Route sampling is only done on accepted damage.
        for(double turn:new double[]{0,.45,-.45,.9,-.9,1.35,-1.35}) {
            for(int distance=6;distance>=4;distance--) {
                var candidate=from.add(Math.cos(angle+turn)*distance,0,Math.sin(angle+turn)*distance);
                if(!safeRoute(from,candidate))continue;
                start=surface=from;end=candidate;elapsed=0;readyAt=worm.tickCount+COOLDOWN;
                worm.goalSelector.getAvailableGoals().forEach(goal->{if(goal.isRunning())goal.stop();});
                worm.targetSelector.getAvailableGoals().forEach(goal->{if(goal.isRunning())goal.stop();});
                worm.setTarget(null);worm.setLastHurtByMob(null);worm.clearBurrowActions();worm.setBurrowing(true);
                float yaw=(float)(Mth.atan2(end.z-start.z,end.x-start.x)*Mth.RAD_TO_DEG)-90;
                worm.setYRot(yaw);worm.setYHeadRot(yaw);worm.setYBodyRot(yaw);
                particles(14);return;
            }
        }
    }
    private boolean safeRoute(Vec3 from,Vec3 to) {
        int samples=Mth.ceil(from.distanceTo(to)*2);
        for(int i=1;i<=samples;i++)if(!safeColumn(from.lerp(to,(double)i/samples)))return false;
        return true;
    }
    /** The whole body footprint needs two layers of soil and an unobstructed dry surface exit. */
    public boolean safeColumn(Vec3 at) {
        double radius=1.65*worm.getScale();
        int ground=Mth.floor(at.y-.05);
        for(int x=Mth.floor(at.x-radius);x<=Mth.floor(at.x+radius);x++)for(int z=Mth.floor(at.z-radius);z<=Mth.floor(at.z+radius);z++) {
            for(int y=ground-Mth.ceil(DEPTH*worm.getScale()+.1)+1;y<=ground;y++) {
                var pos=new BlockPos(x,y,z);
                if(!worm.level().hasChunkAt(pos))return false;
                var state=worm.level().getBlockState(pos);
                if(!state.is(DIGGABLE)||!state.getFluidState().isEmpty())return false;
            }
            var top=new BlockPos(x,Mth.floor(at.y),z);
            if(!worm.level().hasChunkAt(top)||!worm.level().getFluidState(top).isEmpty())return false;
        }
        return worm.level().noCollision(worm,new AABB(at.x-radius,at.y+.01,at.z-radius,at.x+radius,at.y+.8*worm.getScale(),at.z+radius));
    }
    public void tick() {
        if(!active())return;
        if(!worm.isAlive()) { finish();return; }
        elapsed++;
        double travel=Mth.clamp((elapsed-12)/36.0,0,1);
        Vec3 next=start.lerp(end,travel);
        // Recheck the next occupied column: changing terrain cannot make the worm tunnel into stone.
        if(!safeColumn(next)) { finish();return; }
        surface=next;
        double depth=DEPTH*worm.getScale()*Math.min(1,Math.min(elapsed/12.0,(DURATION-elapsed)/12.0));
        worm.setPos(surface.x,surface.y-Math.max(0,depth),surface.z);
        worm.setDeltaMovement(Vec3.ZERO);worm.fallDistance=0;
        if(elapsed%2==0)particles(5);
        if(elapsed>=DURATION)finish();
    }
    private void particles(int count) {
        if(!(worm.level() instanceof ServerLevel level)||surface==null)return;
        var soil=level.getBlockState(BlockPos.containing(surface.add(0,-.05,0)));
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,soil),surface.x,surface.y+.08,surface.z,count,.22,.06,.22,.02);
    }
    private void finish() {
        // Prefer the last validated surface. Fall back along the original route if that exit changed.
        if(!safeColumn(surface)) {
            for(int i=0;i<=12;i++) {
                var fallback=start.lerp(end,i/12.0);
                if(safeColumn(fallback)) {surface=fallback;break;}
            }
        }
        worm.setPos(surface.x,surface.y,surface.z);worm.setDeltaMovement(Vec3.ZERO);worm.fallDistance=0;
        particles(14);worm.setTarget(null);worm.setLastHurtByMob(null);worm.setBurrowing(false);start=end=null;worm.setOnGround(true);
    }
    public void save(net.minecraft.nbt.CompoundTag tag) {
        tag.putInt("RootWormBurrowCooldown",Math.max(0,readyAt-worm.tickCount));
        if(active())tag.putDouble("RootWormBurrowSurfaceY",surface.y);
    }
    public void load(net.minecraft.nbt.CompoundTag tag) {
        readyAt=worm.tickCount+Mth.clamp(tag.getInt("RootWormBurrowCooldown"),0,COOLDOWN);
        if(tag.contains("RootWormBurrowSurfaceY",net.minecraft.nbt.Tag.TAG_ANY_NUMERIC))worm.setPos(worm.getX(),tag.getDouble("RootWormBurrowSurfaceY"),worm.getZ());
    }
}
