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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.Horse;

/** Spawn-group identity is saved on each animal; no entity or chunk is kept loaded. */
public final class SpawnHerd {
    private static String key(Animal animal) {
        return animal instanceof Cow ? "ffm_cow_herd" : "ffm_horse_herd";
    }
    private static final Map<SpawnGroupData, UUID> GROUPS = new WeakHashMap<>();

    private SpawnHerd() {}

    public static void spawned(Mob mob, SpawnGroupData group) {
        if (!(mob instanceof Animal animal) || !(animal instanceof Cow || animal instanceof Horse)
                || group == null || mob.getPersistentData().hasUUID(key(animal))) return;
        mob.getPersistentData().putUUID(key(animal), GROUPS.computeIfAbsent(group, ignored -> UUID.randomUUID()));
    }

    public static boolean together(Animal first, Animal second) {
        return key(first).equals(key(second)) && first.getPersistentData().hasUUID(key(first)) && second.getPersistentData().hasUUID(key(second))
                && first.getPersistentData().getUUID(key(first)).equals(second.getPersistentData().getUUID(key(second)));
    }

    public static boolean inherit(Animal parent, Animal child) {
        if (!parent.getPersistentData().hasUUID(key(parent))) return false;
        child.getPersistentData().putUUID(key(child), parent.getPersistentData().getUUID(key(parent)));
        return true;
    }

    public static final class FollowHerdGoal extends Goal {
        private final Animal animal;
        private Animal leader;
        private int nextScan;
        private int nextPath;

        public FollowHerdGoal(Animal animal) {
            this.animal = animal;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override public boolean canUse() {
            if (animal.isLeashed() || animal.isPassenger() || animal.isVehicle() || !animal.getPersistentData().hasUUID(key(animal))) return false;
            if (nextScan-- > 0) return false;
            nextScan = reducedTickDelay(20);
            // Every member chooses the same stable leader while in range. Re-elect
            // from loaded herd members if that animal dies or leaves the area.
            leader = animal.level().getEntitiesOfClass(Animal.class, animal.getBoundingBox().inflate(48),
                    other -> other.isAlive() && together(animal, other)).stream()
                    .min(Comparator.comparing(Animal::getUUID)).orElse(animal);
            return leader != animal && animal.distanceToSqr(leader) > 36;
        }

        @Override public boolean canContinueToUse() {
            return leader != null && leader.isAlive() && !animal.isLeashed() && !animal.isPassenger() && !animal.isVehicle()
                    && together(animal, leader) && animal.distanceToSqr(leader) > 9 && animal.distanceToSqr(leader) < 2304;
        }

        @Override public void start() { nextPath = 0; }

        @Override public void tick() {
            if (--nextPath <= 0) {
                nextPath = adjustedTickDelay(10);
                animal.getNavigation().moveTo(leader, 1.0);
            }
        }

        @Override public void stop() { leader = null; animal.getNavigation().stop(); }
    }
}
