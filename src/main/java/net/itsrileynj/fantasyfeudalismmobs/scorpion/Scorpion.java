package net.itsrileynj.fantasyfeudalismmobs.scorpion;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Native arthropod monster; intentionally separate from vanilla spider companion behavior. */
public final class Scorpion extends Monster {
    public static final int STING_DURATION=18, STING_WINDUP=8;
    private static final EntityDataAccessor<Long> STING_STARTED=SynchedEntityData.defineId(Scorpion.class,EntityDataSerializers.LONG);
    private UUID stingTarget;
    public Scorpion(EntityType<? extends Scorpion> type,Level level) {super(type,level);xpReward=5;}
    public static AttributeSupplier.Builder createAttributes() {return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,16).add(Attributes.MOVEMENT_SPEED,.26).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.FOLLOW_RANGE,16);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {super.defineSynchedData(b);b.define(STING_STARTED,-1000L);}
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new MeleeAttackGoal(this,1.1,true) {
            @Override protected void checkAndPerformAttack(LivingEntity target) {
                if(!isStinging()&&canPerformAttack(target)){resetAttackCooldown();beginSting(target);}
            }
            @Override public void tick() {super.tick();if(isStinging())getNavigation().stop();}
        });
        goalSelector.addGoal(2,new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(3,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(4,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    public void beginSting(LivingEntity target) {
        if(level().isClientSide||!validTarget(target)||isStinging())return;
        stingTarget=target.getUUID();entityData.set(STING_STARTED,level().getGameTime());getNavigation().stop();
    }
    private boolean validTarget(LivingEntity target) {return target!=null&&target.isAlive()&&target.level()==level()&&canAttack(target)&&!isAlliedTo(target)&&!(target instanceof Player p&&(p.isCreative()||p.isSpectator()));}
    public boolean isStinging() {long elapsed=level().getGameTime()-entityData.get(STING_STARTED);return elapsed>=0&&elapsed<STING_DURATION;}
    public float getStingAmount(float partialTick) {
        float elapsed=level().getGameTime()-entityData.get(STING_STARTED)+partialTick;
        return elapsed<0||elapsed>=STING_DURATION?0:(float)Math.sin(Math.PI*elapsed/STING_DURATION);
    }
    public float getPincerOpenAmount(float partialTick) {
        float elapsed=level().getGameTime()-entityData.get(STING_STARTED)+partialTick;
        return elapsed<0||elapsed>=STING_WINDUP?0:(float)Math.sin(Math.PI*elapsed/STING_WINDUP);
    }
    @Override public void aiStep() {
        super.aiStep();
        if(level() instanceof ServerLevel server) {
            long elapsed=level().getGameTime()-entityData.get(STING_STARTED);
            if(stingTarget!=null&&elapsed==STING_WINDUP) {
                var entity=server.getEntity(stingTarget);
                if(entity instanceof LivingEntity victim&&validTarget(victim)&&isWithinMeleeAttackRange(victim)&&hasLineOfSight(victim)) {
                    if(doHurtTarget(victim))playSound(ScorpionModule.STING.get(),.8F,1);
                }
                stingTarget=null;
            }
            if(elapsed>=STING_DURATION)stingTarget=null;
        }
    }
    @Override public boolean doHurtTarget(Entity victim) {
        if(!(victim instanceof LivingEntity living)||!validTarget(living))return false;
        if(!super.doHurtTarget(victim))return false;
        living.addEffect(new MobEffectInstance(MobEffects.POISON,100,0),this);return true;
    }
    @Override public boolean canBeAffected(MobEffectInstance effect) {return !effect.is(MobEffects.POISON)&&super.canBeAffected(effect);}
    @Override public int getMaxSpawnClusterSize() {return 2;}
    @Override protected SoundEvent getAmbientSound() {return ScorpionModule.AMBIENT.get();}
    @Override protected SoundEvent getHurtSound(DamageSource source) {return ScorpionModule.HURT.get();}
    @Override protected SoundEvent getDeathSound() {return ScorpionModule.DEATH.get();}
    @Override protected void playStepSound(BlockPos pos,BlockState state) {playSound(ScorpionModule.STEP.get(),.15F,1);}
    @Override public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {return getBoundingBox().inflate(1.4,1,1.4);}
}
