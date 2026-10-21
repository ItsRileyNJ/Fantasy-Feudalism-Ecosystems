package net.itsrileynj.fantasyfeudalismmobs;

import java.util.Comparator;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.schedule.Activity;

/** A rare, harmless PLAY activity; normal village navigation and panic remain in control. */
public final class BabyCatPlay extends Behavior<Villager> {
    public static final int DURATION=160;
    private Cat cat;
    private long nextCheck,until;
    private WalkTarget walk;
    private EntityTracker look;

    public BabyCatPlay() {
        super(Map.of(MemoryModuleType.WALK_TARGET,MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.LOOK_TARGET,MemoryStatus.REGISTERED),DURATION);
    }

    public static boolean safe(Villager child) {
        var brain=child.getBrain();
        return child.isBaby() && child.isAlive() && !child.isSleeping() && !child.isTrading()
                && brain.isActive(Activity.PLAY) && !brain.isActive(Activity.PANIC)
                && !brain.hasMemoryValue(MemoryModuleType.HURT_BY)
                && !brain.hasMemoryValue(MemoryModuleType.NEAREST_HOSTILE);
    }
    public static boolean chasing(Villager child,Cat cat) {
        var state=(VillagerPlayState)child;
        return !cat.isTame() && safe(child) && cat.getUUID().equals(state.ffm$playCat())
                && child.level().getGameTime()<state.ffm$playUntil();
    }

    @Override protected boolean checkExtraStartConditions(ServerLevel level,Villager child) {
        long now=level.getGameTime();
        if(now<nextCheck || !safe(child) || !level.isVillage(child.blockPosition()))return false;
        nextCheck=now+200;
        if(child.getRandom().nextInt(12)!=0)return false;
        cat=level.getEntitiesOfClass(Cat.class,child.getBoundingBox().inflate(12),
                candidate->candidate.isAlive()&&!candidate.isTame()&&level.isVillage(candidate.blockPosition())&&child.hasLineOfSight(candidate))
                .stream().min(Comparator.comparingDouble(child::distanceToSqr)).orElse(null);
        return cat!=null;
    }
    @Override protected void start(ServerLevel level,Villager child,long now) {
        until=now+DURATION;nextCheck=until+2400;
        ((VillagerPlayState)child).ffm$playCat(cat.getUUID(),until);follow(child);
    }
    private void follow(Villager child) {
        walk=new WalkTarget(new EntityTracker(cat,false),0.6F,1);
        look=new EntityTracker(cat,true);
        child.getBrain().setMemory(MemoryModuleType.WALK_TARGET,walk);
        child.getBrain().setMemory(MemoryModuleType.LOOK_TARGET,look);
    }
    @Override protected boolean canStillUse(ServerLevel level,Villager child,long now) {
        return now<until&&safe(child)&&cat!=null&&cat.isAlive()&&!cat.isTame()&&child.distanceToSqr(cat)<256;
    }
    @Override protected void tick(ServerLevel level,Villager child,long now) { follow(child); }
    @Override protected void stop(ServerLevel level,Villager child,long now) {
        var brain=child.getBrain();
        if(brain.getMemory(MemoryModuleType.WALK_TARGET).orElse(null)==walk)brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        if(brain.getMemory(MemoryModuleType.LOOK_TARGET).orElse(null)==look)brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
        ((VillagerPlayState)child).ffm$playCat(null,0);
        cat=null;walk=null;look=null;
    }
}
