package net.itsrileynj.fantasyfeudalismmobs;

import java.util.Comparator;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.Horse;

/** Automatic breeding uses elapsed world ticks, including time spent unloaded. */
public final class HorseLife {
    public static final long BREED_INTERVAL = 150L * 60 * 20;
    private static final long LEGACY_INTERVAL = 14L * 24000;
    private static final String INTERVAL = "ffm_horse_breed_interval";
    private static final String NEXT_BREED = "ffm_horse_next_breed";
    private static final String MATE = "ffm_horse_mate";

    private HorseLife() {}

    public static void initialize(Horse horse) {
        var data=horse.getPersistentData();
        if (!data.contains(NEXT_BREED)) {
            data.putLong(NEXT_BREED, horse.level().getGameTime() + BREED_INTERVAL);
            data.putLong(INTERVAL, BREED_INTERVAL);
        } else {
            long previous=data.contains(INTERVAL)?data.getLong(INTERVAL):LEGACY_INTERVAL;
            if(previous!=BREED_INTERVAL) {
                // Preserve the cycle's start and elapsed time; overdue horses stay eligible.
                data.putLong(NEXT_BREED,data.getLong(NEXT_BREED)+BREED_INTERVAL-previous);
                data.putLong(INTERVAL,BREED_INTERVAL);
            }
        }
    }

    public static long nextBreed(Horse horse) {
        initialize(horse);
        return horse.getPersistentData().getLong(NEXT_BREED);
    }

    public static boolean ready(Horse horse) {
        return horse.isAlive() && horse.getAge() == 0 && !horse.isVehicle() && !horse.isPassenger()
                && !horse.isPanicking() && horse.level().getGameTime() >= nextBreed(horse);
    }

    public static void grazed(Horse horse) {
        if (!ready(horse) || horse.isInLove()) return;
        Horse mate = horse.level().getEntitiesOfClass(Horse.class, horse.getBoundingBox().inflate(8),
                other -> other != horse && ready(other) && !other.isInLove() && !other.isNoAi())
                .stream().min(Comparator.comparingDouble(horse::distanceToSqr)).orElse(null);
        if (mate == null) return;
        horse.getPersistentData().putUUID(MATE, mate.getUUID());
        mate.getPersistentData().putUUID(MATE, horse.getUUID());
        horse.setInLove(null);
        mate.setInLove(null);
    }

    public static boolean automaticPair(Horse first, Horse second) {
        return first != second && ready(first) && ready(second) && first.isInLove() && second.isInLove()
                && first.getPersistentData().hasUUID(MATE) && second.getPersistentData().hasUUID(MATE)
                && first.getPersistentData().getUUID(MATE).equals(second.getUUID())
                && second.getPersistentData().getUUID(MATE).equals(first.getUUID());
    }

    public static void born(Animal first, Animal second, AgeableMob child) {
        if (first instanceof Horse horse) reset(horse);
        if (second instanceof Horse horse) reset(horse);
        if (child instanceof Horse foal) {
            initialize(foal);
            foal.setBred(true);
            boolean inherited = first instanceof Horse horse && SpawnHerd.inherit(horse, foal);
            if (!inherited && second instanceof Horse horse) SpawnHerd.inherit(horse, foal);
        }
    }

    private static void reset(Horse horse) {
        horse.getPersistentData().putLong(NEXT_BREED, horse.level().getGameTime() + BREED_INTERVAL);
        horse.getPersistentData().putLong(INTERVAL, BREED_INTERVAL);
        horse.getPersistentData().remove(MATE);
    }
}
