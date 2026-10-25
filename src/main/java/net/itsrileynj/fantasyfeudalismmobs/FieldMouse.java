package net.itsrileynj.fantasyfeudalismmobs;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.Path;
import net.neoforged.neoforge.event.EventHooks;

/** A crop-seeking wildlife mob, not a silverfish replacement. All food/flee decisions are server-owned. */
public final class FieldMouse extends PathfinderMob {
    public static final int WHITE=0,AMBER=1;
    private static final EntityDataAccessor<Integer> VARIANT=SynchedEntityData.defineId(FieldMouse.class,EntityDataSerializers.INT);
    public static final int EAT_COOLDOWN=90*20;
    public static final int CROP_CHECK_INTERVAL=20*20;
    public static final double CAT_CHASE_SPEED=1.33;
    // LivingEntity applies an additional 30% attribute modifier to a sprinting cat.
    public static final double RUN_SPEED=(double).3F*CAT_CHASE_SPEED*(1+(double).3F)*.9;
    private static final ResourceLocation MERCENARY_TYPE=ResourceLocation.fromNamespaceAndPath("villagerscontract","defender");
    private long nextMeal;
    private BlockPos cropHome;
    private BlockPos villageHome;
    public FieldMouse(EntityType<? extends FieldMouse> type,Level level){super(type,level);setPersistenceRequired();}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
        super.defineSynchedData(builder);builder.define(VARIANT,WHITE);
    }
    public int getVariant(){return entityData.get(VARIANT);}
    public void setVariant(int variant){entityData.set(VARIANT,variant==AMBER?AMBER:WHITE);}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group){
        var result=super.finalizeSpawn(level,difficulty,reason,group);
        setVariant(getRandom().nextBoolean()?AMBER:WHITE);return result;
    }
    public static AttributeSupplier.Builder createAttributes(){return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH,4).add(Attributes.MOVEMENT_SPEED,RUN_SPEED).add(Attributes.FOLLOW_RANGE,24);}
    // Use Mob's random ambient scheduler with the same minimum interval as vanilla Animal.
    @Override public int getAmbientSoundInterval(){return 120;}
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return FantasyFeudalismMobs.MOUSE_AMBIENT.get();}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return FantasyFeudalismMobs.MOUSE_HURT.get();}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return FantasyFeudalismMobs.MOUSE_HURT.get();}
    public long nextMeal(){return nextMeal;}
    public BlockPos cropHome(){return cropHome;}
    public BlockPos villageHome(){return villageHome;}
    public void setVillageHome(BlockPos home){villageHome=home.immutable();}
    public void setCropHome(BlockPos home){cropHome=home.immutable();}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new FleeGoal());
        goalSelector.addGoal(3,new EatCropsGoal());
        goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,.55){
            @Override protected net.minecraft.world.phys.Vec3 getPosition(){
                if(cropHome==null)return super.getPosition();
                // Ordinary idle movement stays near the remembered food, without searching for more crops.
                var pos=DefaultRandomPos.getPos(FieldMouse.this,3,2);
                return pos!=null&&pos.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(cropHome))<=16?pos:null;
            }
        });
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
    }
    public static boolean threat(LivingEntity entity){
        if(entity instanceof FieldMouse||!entity.isAlive()||entity.isSpectator())return false;
        if(entity instanceof Player player)return !player.isCreative();
        if(entity instanceof Cat||entity instanceof Villager)return true;
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(MERCENARY_TYPE);
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(source.getDirectEntity() instanceof Cat)amount=Math.max(amount,getHealth()+getAbsorptionAmount());
        return super.hurt(source,amount);
    }
    public static IntegerProperty cropAge(BlockState state){
        Block block=state.getBlock();
        if(!(block instanceof CropBlock||block instanceof StemBlock||block instanceof NetherWartBlock
                ||block instanceof CocoaBlock||block instanceof SweetBerryBushBlock||block instanceof PitcherCropBlock))return null;
        if(block instanceof PitcherCropBlock&&state.getValue(PitcherCropBlock.HALF)==DoubleBlockHalf.UPPER)return null;
        return block.getStateDefinition().getProperty("age") instanceof IntegerProperty age?age:null;
    }
    public boolean eatCrop(BlockPos pos){
        if(!(level() instanceof ServerLevel server)||server.getGameTime()<nextMeal
                ||!server.hasChunkAt(pos)||distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>1.6*1.6)return false;
        var state=server.getBlockState(pos);var age=cropAge(state);
        if(age==null||state.getValue(age)<=0)return false;
        if(!EventHooks.canEntityGrief(server,this))return false;
        var sight=server.clip(new net.minecraft.world.level.ClipContext(getEyePosition(),net.minecraft.world.phys.Vec3.atCenterOf(pos),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,this));
        if(sight.getType()!=net.minecraft.world.phys.HitResult.Type.MISS&&!sight.getBlockPos().equals(pos))return false;
        int reduced=state.getValue(age)-1;
        server.setBlock(pos,state.setValue(age,reduced),3);
        if(state.is(Blocks.PITCHER_CROP)&&server.getBlockState(pos.above()).is(Blocks.PITCHER_CROP)){
            // Change the lower half first: removing the upper half from a still-double
            // plant would trigger native break/drop logic. Keep both ages synchronized.
            server.setBlock(pos.above(),reduced<3?Blocks.AIR.defaultBlockState():
                    server.getBlockState(pos.above()).setValue(PitcherCropBlock.AGE,reduced),3);
        }
        nextMeal=server.getGameTime()+EAT_COOLDOWN;cropHome=pos.immutable();
        return true;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);tag.putLong("NextMeal",nextMeal);tag.putInt("MealInterval",EAT_COOLDOWN);tag.putInt("Variant",getVariant());
        if(cropHome!=null)tag.putLong("CropHome",cropHome.asLong());
        if(villageHome!=null)tag.putLong("VillageHome",villageHome.asLong());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);nextMeal=tag.getLong("NextMeal");setVariant(tag.getInt("Variant"));
        // Preserve elapsed time when upgrading the previous two-minute feeding timer.
        if(nextMeal>0&&!tag.contains("MealInterval"))nextMeal-=30*20;
        cropHome=tag.contains("CropHome")?BlockPos.of(tag.getLong("CropHome")):null;
        villageHome=tag.contains("VillageHome")?BlockPos.of(tag.getLong("VillageHome")):null;
    }
    private final class FleeGoal extends Goal {
        private LivingEntity danger;private Path path;private long nextScan;
        FleeGoal(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){
            long now=level().getGameTime();
            if(now<nextScan)return false;nextScan=now+10;
            danger=null;double nearest=9;
            for(var candidate:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(3),FieldMouse::threat)){
                double distance=distanceToSqr(candidate);if(distance<=nearest){nearest=distance;danger=candidate;}
            }
            if(danger==null)return false;
            var away=DefaultRandomPos.getPosAway(FieldMouse.this,12,4,danger.position());
            if(away==null||danger.distanceToSqr(away)<=danger.distanceToSqr(FieldMouse.this))return false;
            path=getNavigation().createPath(away.x,away.y,away.z,0);return path!=null;
        }
        @Override public boolean canContinueToUse(){return danger!=null&&threat(danger)&&!getNavigation().isDone();}
        @Override public void start(){getNavigation().moveTo(path,1);}
        @Override public void stop(){danger=null;getNavigation().stop();}
    }
    private final class EatCropsGoal extends MoveToBlockGoal {
        private boolean edibleOnly,waitingForBudget,settled;
        private BlockPos youngFallback;
        private long nextSearch=-1,nextHomeCheck,nextTargetCheck,nextBite=-1;
        EatCropsGoal(){super(FieldMouse.this,.65,16,5);}
        @Override public boolean canUse(){
            long now=level().getGameTime();
            if(nextSearch<0){nextSearch=now+Math.floorMod(getId()*31,20);nextHomeCheck=nextSearch;}
            if(now<nextSearch){
                if(now<nextHomeCheck)return false;nextHomeCheck=now+CROP_CHECK_INTERVAL;
                return rememberedCrop();
            }
            waitingForBudget=false;boolean found=findNearestBlock();
            nextSearch=now+(waitingForBudget?10+Math.floorMod(getId(),10):CROP_CHECK_INTERVAL);
            nextHomeCheck=now+CROP_CHECK_INTERVAL;
            return found;
        }
        @Override public double acceptedDistance(){return 1.6;}
        @Override protected BlockPos getMoveToTarget(){return blockPos;}
        @Override protected void moveMobToBlock(){getNavigation().moveTo(blockPos.getX()+.5,blockPos.getY(),blockPos.getZ()+.5,.65);}
        @Override protected boolean isValidTarget(LevelReader world,BlockPos pos){
            if(!world.hasChunkAt(pos))return false;
            var state=world.getBlockState(pos);var age=cropAge(state);
            if(age==null||!world.getFluidState(pos).isEmpty())return false;
            if(edibleOnly&&state.getValue(age)==0){if(youngFallback==null)youngFallback=pos.immutable();return false;}
            return true;
        }
        private boolean rememberedCrop(){
            if(cropHome!=null&&isValidTarget(level(),cropHome)&&distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(cropHome))<48*48){
                var state=level().getBlockState(cropHome);var age=FieldMouse.cropAge(state);
                if(level().getGameTime()<nextMeal||state.getValue(age)>0){blockPos=cropHome;return true;}
            }
            return false;
        }
        @Override protected boolean findNearestBlock(){
            if(rememberedCrop())return true;
            if(!MouseWorkBudget.tryCropSearch((ServerLevel)level())){waitingForBudget=true;return false;}
            youngFallback=null;edibleOnly=level().getGameTime()>=nextMeal;
            boolean found=super.findNearestBlock();edibleOnly=false;
            if(found){cropHome=blockPos.immutable();return true;}
            if(youngFallback!=null){blockPos=youngFallback;cropHome=youngFallback;return true;}
            cropHome=null;return false;
        }
        @Override public void start(){settled=false;super.start();nextTargetCheck=level().getGameTime()+CROP_CHECK_INTERVAL;}
        @Override public boolean canContinueToUse(){
            if(settled)return false;
            long now=level().getGameTime();
            if(now<nextTargetCheck)return true;
            nextTargetCheck=now+CROP_CHECK_INTERVAL;
            var state=level().getBlockState(blockPos);var age=FieldMouse.cropAge(state);
            if(age==null||!super.canContinueToUse())return false;
            return now<nextMeal||state.getValue(age)>0||now<nextSearch;
        }
        @Override public void tick(){
            super.tick();
            if(isReachedTarget()){
                if(!getNavigation().isDone())getNavigation().stop();
                long now=level().getGameTime();
                if(nextBite<0)nextBite=now+Math.floorMod(getId(),40);
                if(now<nextMeal)settled=true;
                else if(now>=nextBite){
                    nextBite=eatCrop(blockPos)?nextMeal:now+CROP_CHECK_INTERVAL;
                    settled=true;
                }
                // A settled visit releases movement/look control for idle wandering and head turns.
            }
        }
        @Override public void stop(){super.stop();getNavigation().stop();}
    }
}
