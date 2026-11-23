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
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Native arthropod monster; intentionally separate from vanilla spider companion behavior. */
public final class Scorpion extends Monster {
    public static final int STING_DURATION=18, STING_WINDUP=8;
    private static final EntityDataAccessor<Long> STING_STARTED=SynchedEntityData.defineId(Scorpion.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Byte> ATTACK_KIND=SynchedEntityData.defineId(Scorpion.class,EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> EATING_STARTED=SynchedEntityData.defineId(Scorpion.class,EntityDataSerializers.LONG);
    private UUID stingTarget;
    private boolean nextPincer;
    public Scorpion(EntityType<? extends Scorpion> type,Level level) {super(type,level);xpReward=5;net.itsrileynj.fantasyfeudalismmobs.AvoidWaterGoal.configure(this);}
    public static AttributeSupplier.Builder createAttributes() {return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,16).add(Attributes.MOVEMENT_SPEED,.26).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.FOLLOW_RANGE,16);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {super.defineSynchedData(b);b.define(STING_STARTED,-1000L);b.define(ATTACK_KIND,(byte)0);b.define(EATING_STARTED,-1000L);}
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new net.itsrileynj.fantasyfeudalismmobs.AvoidWaterGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.1,true) {
            @Override protected void checkAndPerformAttack(LivingEntity target) {
                if(!isAttacking()&&!isEating()&&canPerformAttack(target)) {
                    resetAttackCooldown();
                    if(!isLava()&&!(target instanceof Spider)&&nextPincer)beginPincer(target);else beginSting(target);
                    nextPincer=!nextPincer;
                }
            }
            @Override public void tick() {super.tick();if(isAttacking()||isEating())getNavigation().stop();}
        });
        goalSelector.addGoal(3,new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(4,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(5,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Spider.class,10,true,false,e->!isLava()));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    public void beginSting(LivingEntity target) {beginAttack(target,false);}
    public void beginPincer(LivingEntity target) {if(!isLava())beginAttack(target,true);}
    private void beginAttack(LivingEntity target,boolean pincer) {
        if(level().isClientSide||!validTarget(target)||isAttacking()||isEating())return;
        stingTarget=target.getUUID();entityData.set(ATTACK_KIND,(byte)(pincer?1:0));entityData.set(STING_STARTED,level().getGameTime());getNavigation().stop();
    }
    public boolean isEating() {long elapsed=level().getGameTime()-entityData.get(EATING_STARTED);return elapsed>=0&&elapsed<40;}
    public float getEatAmount(float partialTick) {float elapsed=level().getGameTime()-entityData.get(EATING_STARTED)+partialTick;return elapsed<0||elapsed>=40?0:(float)Math.sin(Math.PI*elapsed/40);}
    private boolean isPincerAttack() {return entityData.get(ATTACK_KIND)==1;}
    public boolean isAttacking() {long elapsed=level().getGameTime()-entityData.get(STING_STARTED);return elapsed>=0&&elapsed<(isPincerAttack()?10:STING_DURATION);}
    private boolean validTarget(LivingEntity target) {return target!=null&&target.isAlive()&&target.level()==level()&&canAttack(target)&&!isAlliedTo(target)&&!(target instanceof Player p&&(p.isCreative()||p.isSpectator()));}
    public boolean isStinging() {return !isPincerAttack()&&isAttacking();}
    public float getStingAmount(float partialTick) {
        if(isPincerAttack())return 0;
        float elapsed=level().getGameTime()-entityData.get(STING_STARTED)+partialTick;
        return elapsed<0||elapsed>=STING_DURATION?0:(float)Math.sin(Math.PI*elapsed/STING_DURATION);
    }
    public float getPincerOpenAmount(float partialTick) {
        float elapsed=level().getGameTime()-entityData.get(STING_STARTED)+partialTick;
        int windup=isPincerAttack()?4:STING_WINDUP;
        return elapsed<0||elapsed>=windup?0:(float)Math.sin(Math.PI*elapsed/windup);
    }
    public float getAttackBlend(float partialTick) {
        float elapsed=level().getGameTime()-entityData.get(STING_STARTED)+partialTick;
        float duration=isPincerAttack()?10:STING_DURATION;
        if(elapsed<0||elapsed>=duration)return 0;
        float blend=net.minecraft.util.Mth.clamp(Math.min(elapsed,duration-elapsed)/3,0,1);
        return blend*blend*(3-2*blend);
    }
    private boolean canHunt() {return isLava()||net.itsrileynj.fantasyfeudalismmobs.NightCycle.isNight(level());}
    @Override public boolean canAttack(LivingEntity target) {return canHunt()&&!(isLava()&&target instanceof Player)&&super.canAttack(target);}
    @Override public void setTarget(@javax.annotation.Nullable LivingEntity target) {super.setTarget(canHunt()&&!(isLava()&&target instanceof Player)?target:null);}
    @Override public void aiStep() {
        if(!level().isClientSide&&!canHunt()) {
            if(getTarget()!=null||stingTarget!=null||isAttacking())getNavigation().stop();
            super.setTarget(null);stingTarget=null;
            if(isAttacking())entityData.set(STING_STARTED,-1000L);
        }
        super.aiStep();
        if(level() instanceof ServerLevel server) {
            long elapsed=level().getGameTime()-entityData.get(STING_STARTED);
            if(isEating())getNavigation().stop();
            if(stingTarget!=null&&elapsed==(isPincerAttack()?4:STING_WINDUP)) {
                var entity=server.getEntity(stingTarget);
                if(entity instanceof LivingEntity victim&&validTarget(victim)&&isWithinMeleeAttackRange(victim)&&hasLineOfSight(victim)) {
                    if(isPincerAttack())doHurtTarget(victim);else if(sting(victim))playSound(ScorpionModule.STING.get(),.8F,1);
                }
                stingTarget=null;
            }
            if(elapsed>=STING_DURATION)stingTarget=null;
        }
    }
    @Override public boolean doHurtTarget(Entity victim) {
        if(!(victim instanceof LivingEntity living)||!validTarget(living))return false;
        if(!super.doHurtTarget(victim))return false;
        if(isLava())living.addEffect(new MobEffectInstance(MobEffects.POISON,100,0),this);return true;
    }
    private boolean sting(LivingEntity victim) {
        if(!isLava()&&victim instanceof Spider spider) {
            if(!spider.hurt(damageSources().mobAttack(this),Math.max(1000,spider.getHealth()+spider.getAbsorptionAmount()*2)))return false;
            if(!spider.isAlive()){entityData.set(EATING_STARTED,level().getGameTime());getNavigation().stop();setTarget(null);}
            return true;
        }
        if(!doHurtTarget(victim))return false;
        if(!isLava())victim.addEffect(new MobEffectInstance(victim instanceof Player?MobEffects.CONFUSION:MobEffects.POISON,victim instanceof Player?200:100,0),this);
        return true;
    }
    @Override public void playerTouch(Player player) {
        super.playerTouch(player);
        if(!level().isClientSide&&isLava()&&isAlive()&&!player.isCreative()&&!player.isSpectator()
                &&!player.isSteppingCarefully()&&getBoundingBox().inflate(.02).intersects(player.getBoundingBox()))
            player.hurt(damageSources().hotFloor(),1);
    }
    @Override public boolean isInvulnerableTo(DamageSource source) {
        return !isLava()&&source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow
                &&!(source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.ThrownTrident)
                ||super.isInvulnerableTo(source);
    }
    @Override public boolean canBeAffected(MobEffectInstance effect) {return !effect.is(MobEffects.POISON)&&super.canBeAffected(effect);}
    @Override public int getMaxSpawnClusterSize() {return isLava()?5:2;}
    @Override protected SoundEvent getAmbientSound() {return ScorpionModule.AMBIENT.get();}
    @Override protected SoundEvent getHurtSound(DamageSource source) {return ScorpionModule.HURT.get();}
    @Override protected SoundEvent getDeathSound() {return ScorpionModule.DEATH.get();}
    @Override protected void playStepSound(BlockPos pos,BlockState state) {playSound(ScorpionModule.STEP.get(),.15F,1);}
    public boolean isLava() {return getType()==ScorpionModule.LAVA_SCORPION.get();}
    public float visualScale() {return isLava()?.25F:1;}
    @Override public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {float scale=visualScale();return getBoundingBox().inflate(1.4*scale,scale,1.4*scale);}
}
