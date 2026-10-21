package net.itsrileynj.fantasyfeudalismmobs;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

public final class WolfPacks {
    public static final long MEAL_COOLDOWN=24000;
    private static final String PACK="ffm_wolf_pack";
    private static final Map<SpawnGroupData,UUID> SPAWN_GROUPS=new WeakHashMap<>();
    private static final TargetingConditions PREY=TargetingConditions.forNonCombat().range(15);
    private WolfPacks() {}

    public static void spawned(Mob mob,SpawnGroupData group,MobSpawnType reason) {
        if(!(mob instanceof Wolf wolf) || wolf.isTame())return;
        if(group!=null && (reason==MobSpawnType.NATURAL || reason==MobSpawnType.CHUNK_GENERATION))
            wolf.getPersistentData().putUUID(PACK,SPAWN_GROUPS.computeIfAbsent(group,g->UUID.randomUUID()));
        else pack(wolf); // Eggs, commands and older ungrouped wolves join nearby wild companions.
    }

    public static UUID pack(Wolf wolf) {
        if(!wolf.getPersistentData().hasUUID(PACK)) {
            Wolf neighbour=wolf.level().getEntitiesOfClass(Wolf.class,wolf.getBoundingBox().inflate(24),
                    other->other!=wolf && other.isAlive() && !other.isTame() && other.getPersistentData().hasUUID(PACK))
                    .stream().min(Comparator.comparingDouble(wolf::distanceToSqr)).orElse(null);
            wolf.getPersistentData().putUUID(PACK,neighbour==null?UUID.randomUUID():neighbour.getPersistentData().getUUID(PACK));
        }
        return wolf.getPersistentData().getUUID(PACK);
    }

    public static void initialize(Wolf wolf) {
        if(!wolf.isTame())pack(wolf);
        ((WolfState)wolf).ffm$setStalking(false);
    }

    public static boolean hungry(Wolf wolf) {
        return !wolf.isTame() && WolfPackData.get((ServerLevel)wolf.level()).deadline(pack(wolf))<=wolf.level().getGameTime();
    }
    public static long deadline(Wolf wolf) { return WolfPackData.get((ServerLevel)wolf.level()).deadline(pack(wolf)); }
    public static boolean handles(Wolf wolf,LivingEntity target) { return !wolf.isTame() && target instanceof Sheep; }
    public static boolean food(LivingEntity target) { return target!=null && (Wolf.PREY_SELECTOR.test(target)||target instanceof Turtle); }
    public static boolean canHunt(Wolf wolf) { return !wolf.isBaby() && (!wolf.isAngry() || wolf.getTarget() instanceof Sheep) && hungry(wolf); }

    private static String huntKey(Wolf wolf) { return wolf.level().dimension().location()+"/"+pack(wolf); }

    public static Sheep prey(Wolf wolf) {
        if(!canHunt(wolf))return null;
        var level=(ServerLevel)wolf.level();var data=WolfPackData.get(level);var hunt=data.hunts.get(huntKey(wolf));
        if(hunt!=null && level.getEntity(hunt.prey) instanceof Sheep sheep && sheep.isAlive()
                && wolf.distanceToSqr(sheep)<=32*32) {
            hunt.touched=level.getGameTime();return sheep;
        }
        return level.getNearestEntity(Sheep.class,PREY,wolf,wolf.getX(),wolf.getY(),wolf.getZ(),wolf.getBoundingBox().inflate(15));
    }

    public static Sheep joinHunt(Wolf wolf,Sheep candidate) {
        var data=WolfPackData.get((ServerLevel)wolf.level());String id=huntKey(wolf);
        var existing=data.hunts.get(id);
        if(existing!=null && wolf.level() instanceof ServerLevel level && level.getEntity(existing.prey) instanceof Sheep sheep
                && sheep.isAlive() && wolf.distanceToSqr(sheep)<=1024)return sheep;
        var hunt=new WolfPackData.Hunt(candidate.getUUID(),wolf.getUUID(),wolf.level().getGameTime());
        data.hunts.put(id,hunt);
        // Recruit in one server operation rather than waiting for independent target scans.
        for(var member:members(wolf,32)) {
            if(canHunt(member) && !member.isNoAi() && !member.isLeashed() && !member.isPassenger()
                    && (member.getTarget()==null || member.getTarget() instanceof Sheep)) {
                hunt.members.add(member.getUUID());member.setTarget(candidate);
            }
        }
        return candidate;
    }

    public static List<Wolf> members(Wolf wolf,double radius) {
        UUID id=pack(wolf);
        return wolf.level().getEntitiesOfClass(Wolf.class,wolf.getBoundingBox().inflate(radius),
                other->other.isAlive()&&!other.isTame()&&other.getPersistentData().hasUUID(PACK)
                        &&other.getPersistentData().getUUID(PACK).equals(id));
    }

    /** A common deadline prevents entity tick order and goal scan cadence from splitting the launch. */
    public static boolean ready(Wolf wolf,Sheep prey) {
        var level=(ServerLevel)wolf.level();var hunt=WolfPackData.get(level).hunts.get(huntKey(wolf));
        if(hunt==null || !hunt.prey.equals(prey.getUUID()))return false;
        long now=level.getGameTime();hunt.touched=now;
        if(hunt.startsAt==Long.MAX_VALUE && now>=hunt.assembledAt+200) {
            // An unreachable companion must not leave the pack stuck in assembly forever.
            WolfPackData.get(level).hunts.remove(huntKey(wolf));
            for(UUID id:hunt.members)if(level.getEntity(id) instanceof Wolf member && member.getTarget()==prey)clearHunt(member);
            return false;
        }
        if(hunt.startsAt!=Long.MAX_VALUE)return now>=hunt.startsAt;
        Wolf leader=level.getEntity(hunt.leader) instanceof Wolf w&&w.isAlive()&&!w.isTame()?w:wolf;
        boolean together=true;
        for(UUID id:hunt.members) {
            if(level.getEntity(id) instanceof Wolf member && member.isAlive() && !member.isTame()
                    && canHunt(member) && member.getTarget()==prey && !member.isNoAi()
                    && !member.isLeashed() && !member.isPassenger()) {
                if(member.distanceToSqr(leader)>64 || !((WolfState)member).ffm$hunting())together=false;
            }
        }
        if(together && now>=hunt.assembledAt+20)hunt.startsAt=now+4;
        return false;
    }

    public static Wolf assemblyLeader(Wolf wolf) {
        var level=(ServerLevel)wolf.level();var hunt=WolfPackData.get(level).hunts.get(huntKey(wolf));
        return hunt!=null && level.getEntity(hunt.leader) instanceof Wolf leader && leader.isAlive()&&!leader.isTame()?leader:wolf;
    }

    public static boolean charging(Wolf wolf,Sheep prey) {
        var data=WolfPackData.get((ServerLevel)wolf.level());var hunt=data.hunts.get(huntKey(wolf));
        if(hunt==null || !hunt.prey.equals(prey.getUUID())) {
            joinHunt(wolf,prey);hunt=data.hunts.get(huntKey(wolf));
        }
        if(!hunt.prey.equals(prey.getUUID()))return true;
        hunt.touched=wolf.level().getGameTime();
        if(wolf.distanceToSqr(prey)<=49)hunt.charging=true;
        return hunt.charging;
    }

    public static void ateSheep(Wolf wolf) {
        if(wolf.isTame())return;
        var data=WolfPackData.get((ServerLevel)wolf.level());UUID id=pack(wolf);
        data.feed(id,wolf.level().getGameTime()+MEAL_COOLDOWN);
        for(var member:wolf.level().getEntitiesOfClass(Wolf.class,wolf.getBoundingBox().inflate(48),
                other->!other.isTame()&&other.getPersistentData().hasUUID(PACK)&&other.getPersistentData().getUUID(PACK).equals(id)))
            clearHunt(member);
    }

    public static void beforeTick(Wolf wolf) {
        if(wolf.isTame()) { ((WolfState)wolf).ffm$setStalking(false);return; }
        if(!hungry(wolf) && food(wolf.getTarget()) && wolf.getLastHurtByMob()!=wolf.getTarget())clearHunt(wolf);
    }

    public static void clearHunt(Wolf wolf) {
        if(food(wolf.getTarget()))wolf.setTarget(null);
        if(((WolfState)wolf).ffm$hunting())wolf.stopInPlace();
        ((WolfState)wolf).ffm$setStalking(false);
    }

    public static void tamed(Wolf wolf) {
        if(((WolfState)wolf).ffm$hunting())clearHunt(wolf);
        ((WolfState)wolf).ffm$setHunting(false);((WolfState)wolf).ffm$setStalking(false);
    }
}
