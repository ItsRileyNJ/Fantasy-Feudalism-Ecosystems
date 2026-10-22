package net.itsrileynj.fantasyfeudalismmobs;

import net.minecraft.world.level.Level;

/** World-clock night, independent of weather and the delayed sky-darkening cache. */
public final class NightCycle {
    private NightCycle() {}
    public static boolean isNight(Level level) {
        long time=Math.floorMod(level.getDayTime(),24000);
        return !level.dimensionType().hasFixedTime() && time>=13000 && time<23000;
    }
    public static long nextSunrise(Level level) {
        return Math.floorDiv(level.getDayTime(),24000)*24000+23000;
    }
}
