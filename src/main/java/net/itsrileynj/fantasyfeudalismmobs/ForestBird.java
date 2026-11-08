package net.itsrileynj.fantasyfeudalismmobs;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Server-owned walking/flying wildlife; all four species share the same bounded AI. */
public final class ForestBird extends PathfinderMob implements FlyingAnimal {
    private static final EntityDataAccessor<Boolean> FLYING=SynchedEntityData.defineId(ForestBird.class,EntityDataSerializers.BOOLEAN);
    private final BirdKind kind;
    private final PathNavigation groundNavigation,airNavigation;
    private final MoveControl groundControl,airControl;
    private float oldFlight,flight;
    private UUID flock,partnerId;
    private ForestBird cachedPartner;
    private long nextPairSearch,missingPartnerSince;
    private long nextFlight,landingGrace,cruiseUntil;
    public long cruiseUntil() { return cruiseUntil; }
    public void setCruiseUntil(long value) { cruiseUntil=value; }
    private Vec3 flightHint,landingDestination;
    public Vec3 landingDestination() { return landingDestination; }
    public void setLandingDestination(Vec3 value) { landingDestination=value; }
    private boolean landingFlight;
    public boolean landingFlight() { return landingFlight; }
    public void setLandingFlight(boolean value) { landingFlight=value; }
    public ForestBird(EntityType<? extends ForestBird> type,Level level,BirdKind kind) {
        super(type,level);this.kind=kind;
        groundNavigation=navigation;groundControl=moveControl;
        var flying=new BirdFlightNavigation(this,level);flying.setCanOpenDoors(false);flying.setCanPassDoors(true);flying.setCanFloat(true);
        airNavigation=flying;airControl=new FlyingMoveControl(this,30,true);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER,-1);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.DANGER_FIRE,-1);
        scheduleFlight();
    }
    public static AttributeSupplier.Builder attributes(BirdKind kind) {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,kind==BirdKind.CROW?8:6)
                .add(Attributes.MOVEMENT_SPEED,.18).add(Attributes.FLYING_SPEED,.6).add(Attributes.FOLLOW_RANGE,24);
    }
    public BirdKind kind() { return kind; }
    @Override public int getMaxSpawnClusterSize() { return kind==BirdKind.ROBIN?2:kind.flocking?6:1; }
    public UUID flock() { return flock; }
    /** Only loaded partners participate; pairing never loads another chunk. */
    public ForestBird partner() {
        if(kind!=BirdKind.ROBIN||partnerId==null||!(level() instanceof ServerLevel server))return null;
        if(cachedPartner!=null&&(cachedPartner.getHealth()<=0||(cachedPartner.getRemovalReason()!=null&&cachedPartner.getRemovalReason().shouldDestroy()))) {
            partnerId=null;cachedPartner=null;return null;
        }
        var found=server.getEntity(partnerId);
        if(found instanceof ForestBird other&&other.isAlive()&&other.kind==BirdKind.ROBIN
                &&getUUID().equals(other.partnerId)) {
            cachedPartner=other;missingPartnerSince=0;return other;
        }
        long now=level().getGameTime();
        if(missingPartnerSince==0)missingPartnerSince=now;
        if(now-missingPartnerSince>=600) { partnerId=null;cachedPartner=null;missingPartnerSince=0; }
        return null;
    }
    public void findPartner() {
        if(kind!=BirdKind.ROBIN||level().isClientSide||partner()!=null||partnerId!=null)return;
        var candidates=level().getEntitiesOfClass(ForestBird.class,getBoundingBox().inflate(16,6,16),
                b->b!=this&&b.kind==BirdKind.ROBIN&&b.isAlive()&&!b.isNoAi()&&b.partnerId==null);
        candidates.sort(java.util.Comparator.<ForestBird>comparingInt(b->flock!=null&&flock.equals(b.flock)?0:1)
                .thenComparingDouble(this::distanceToSqr).thenComparing(ForestBird::getUUID));
        if(!candidates.isEmpty()) {
            var other=candidates.getFirst();partnerId=other.getUUID();other.partnerId=getUUID();
            cachedPartner=other;other.cachedPartner=this;missingPartnerSince=other.missingPartnerSince=0;
        }
    }
    public boolean pairCanFly() {
        var other=partner();return other!=null&&!other.isNoAi()&&!other.isLeashed()&&!other.isPassenger()&&!other.isVehicle()
                &&distanceToSqr(other)<=16*16;
    }
    public boolean followsPartner() { var other=partner();return other!=null&&getUUID().compareTo(other.getUUID())>0; }
    public long nextFlight() { return nextFlight; }
    public boolean settling() { return level().getGameTime()<landingGrace; }
    public void scheduleFlight() { nextFlight=level().getGameTime()+80+getRandom().nextInt(81);flightHint=null; }
    public Vec3 flightHint() { return flightHint; }
    public void flyToward(Vec3 hint) { flightHint=hint;nextFlight=level().getGameTime(); }
    @Override protected PathNavigation createNavigation(Level level) { return new GroundPathNavigation(this,level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder);builder.define(FLYING,false); }
    public boolean flightActive() { return entityData.get(FLYING); }
    public void setFlying(boolean flying) {
        if(level().isClientSide)return;
        if(flightActive()!=flying) {
            if(!flying)landingGrace=level().getGameTime()+10;
            navigation.stop();entityData.set(FLYING,flying);
            navigation=flying?airNavigation:groundNavigation;moveControl=flying?airControl:groundControl;
        }
        setNoGravity(flying);if(!flying)setXRot(0);
    }
    @Override public boolean isFlying() { return flightActive(); }
    public float flightAmount(float partial) { return Mth.lerp(partial,oldFlight,flight); }
    @Override public void aiStep() {
        super.aiStep();
        if(!level().isClientSide&&kind==BirdKind.ROBIN&&!isNoAi()&&level().getGameTime()>=nextPairSearch) {
            nextPairSearch=level().getGameTime()+100;findPartner();
        }
        if(level().isClientSide) { oldFlight=flight;flight+=Mth.clamp((isFlying()?1:0)-flight,-.15F,.15F); }
    }
    @Override public void travel(Vec3 input) {
        Vec3 before=position();super.travel(input);
        if(level().isClientSide||!flightActive())return;
        double limit=BirdAltitude.maxFeet(this,getX(),getZ());
        if(getY()<=limit)return;
        double oldLimit=BirdAltitude.maxFeet(this,before.x,before.z);
        if(before.y<=oldLimit+1e-5) {
            // Over a drop, hold the last safe column and descend before crossing.
            if(before.y>limit) { setPos(before.x,Math.min(getY(),oldLimit),before.z);setDeltaMovement(0,Math.min(getDeltaMovement().y,-.08),0); }
            else { setPos(getX(),limit,getZ());setDeltaMovement(getDeltaMovement().multiply(1,0,1)); }
        } else {
            // External placement above the ceiling recovers by descending, without teleporting through trees.
            if(getY()>before.y)setPos(before.x,before.y,before.z);
            setDeltaMovement(0,Math.min(getDeltaMovement().y,-.2),0);
            getMoveControl().setWantedPosition(getX(),limit-.25,getZ(),.8);
        }
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new BirdGoals.Flight(this));
        goalSelector.addGoal(3,new BirdGoals.QuickLook(this));
        goalSelector.addGoal(5,new BirdGoals.Flock(this));
        goalSelector.addGoal(7,new BirdGoals.ShortWalk(this));
    }
    public static boolean scares(Player player) { return player.isAlive()&&!player.isSpectator()&&!player.isCreative(); }
    @Nullable public Player nearbyThreat() { return level().getNearestPlayer(getX(),getY(),getZ(),4,e->e instanceof Player p&&scares(p)); }
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source) { return false; }
    @Override protected void checkFallDamage(double distance,boolean ground,BlockState state,BlockPos pos) { }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.PARROT_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PARROT_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PARROT_DEATH; }
    @Override public int getAmbientSoundInterval() { return 180; }
    @Override public float getVoicePitch() { return super.getVoicePitch()*(kind==BirdKind.CROW?.7F:1.15F); }
    @Override protected float getSoundVolume() { return .35F; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    public static final class FlockData implements SpawnGroupData {
        final BirdKind kind;final UUID id=UUID.randomUUID();int size;
        FlockData(BirdKind kind) { this.kind=kind; }
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,@Nullable SpawnGroupData data) {
        var result=super.finalizeSpawn(level,difficulty,reason,data);
        if(kind.flocking) {
            FlockData group=data instanceof FlockData f&&f.kind==kind&&f.size<getMaxSpawnClusterSize()?f:new FlockData(kind);
            flock=group.id;group.size++;return group;
        }
        return result;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);if(flock!=null)tag.putUUID("BirdFlock",flock);tag.putLong("NextBirdFlight",nextFlight);if(partnerId!=null)tag.putUUID("BirdPartner",partnerId);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);partnerId=tag.hasUUID("BirdPartner")?tag.getUUID("BirdPartner"):null;cachedPartner=null;missingPartnerSince=0;flock=tag.hasUUID("BirdFlock")?tag.getUUID("BirdFlock"):null;
        if(tag.contains("NextBirdFlight"))nextFlight=Math.min(tag.getLong("NextBirdFlight"),level().getGameTime()+160);else scheduleFlight();
        // An unloaded flight must resume through the flight goal, never remain saved in a hovering pose.
        setFlying(false);setNoGravity(false);
    }
}
