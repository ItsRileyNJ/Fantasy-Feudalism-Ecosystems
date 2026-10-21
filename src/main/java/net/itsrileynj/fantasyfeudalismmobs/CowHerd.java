package net.itsrileynj.fantasyfeudalismmobs;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cow;

/** Spawn-group identity is saved on each cow; no entity or chunk is kept loaded. */
public final class CowHerd {
    private static final String HERD = "ffm_cow_herd";
    private static final Map<SpawnGroupData, UUID> GROUPS = new WeakHashMap<>();

    private CowHerd() {}

    public static void spawned(Mob mob, SpawnGroupData group) {
        if (!(mob instanceof Cow) || group == null || mob.getPersistentData().hasUUID(HERD)) return;
        mob.getPersistentData().putUUID(HERD, GROUPS.computeIfAbsent(group, ignored -> UUID.randomUUID()));
    }

    public static boolean together(Cow first, Cow second) {
        return first.getPersistentData().hasUUID(HERD) && second.getPersistentData().hasUUID(HERD)
                && first.getPersistentData().getUUID(HERD).equals(second.getPersistentData().getUUID(HERD));
    }

    public static final class FollowHerdGoal extends Goal {
        private final Cow cow;
        private Cow leader;
        private int nextScan;
        private int nextPath;

        public FollowHerdGoal(Cow cow) {
            this.cow = cow;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override public boolean canUse() {
            if (cow.isLeashed() || cow.isPassenger() || !cow.getPersistentData().hasUUID(HERD)) return false;
            if (nextScan-- > 0) return false;
            nextScan = reducedTickDelay(20);
            // Every member chooses the same stable leader while in range. Re-elect
            // from loaded herd members if that cow dies or leaves the area.
            leader = cow.level().getEntitiesOfClass(Cow.class, cow.getBoundingBox().inflate(48),
                    other -> other.isAlive() && together(cow, other)).stream()
                    .min(Comparator.comparing(Cow::getUUID)).orElse(cow);
            return leader != cow && cow.distanceToSqr(leader) > 36;
        }

        @Override public boolean canContinueToUse() {
            return leader != null && leader.isAlive() && !cow.isLeashed() && !cow.isPassenger()
                    && together(cow, leader) && cow.distanceToSqr(leader) > 9 && cow.distanceToSqr(leader) < 2304;
        }

        @Override public void start() { nextPath = 0; }

        @Override public void tick() {
            if (--nextPath <= 0) {
                nextPath = adjustedTickDelay(10);
                cow.getNavigation().moveTo(leader, 1.0);
            }
        }

        @Override public void stop() { leader = null; cow.getNavigation().stop(); }
    }
}
