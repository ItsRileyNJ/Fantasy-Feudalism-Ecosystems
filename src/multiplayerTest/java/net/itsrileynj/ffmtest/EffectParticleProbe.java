package net.itsrileynj.ffmtest;

import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;

/** Read-only observation of real particles queued by the vanilla client. */
final class EffectParticleProbe {
    private static int watched = -1;
    private static int ticks;
    private static final Map<Integer,Integer> colors = new TreeMap<>();
    private static final Set<Particle> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    private static String error;
    private static Field particles, x, y, z, red, green, blue, alpha;

    static void watch(int entity) {
        watched=entity;ticks=0;colors.clear();seen.clear();error=null;
    }

    private static Field field(Class<?> type, String name) throws ReflectiveOperationException {
        Field field=type.getDeclaredField(name);field.setAccessible(true);return field;
    }

    static void tick(Minecraft mc) {
        if(watched<0 || mc.level==null || error!=null)return;
        ticks++;
        var target=mc.level.getEntity(watched);
        if(target==null)return;
        try {
            if(particles==null) {
                particles=field(ParticleEngine.class,"particles");
                x=field(Particle.class,"x");y=field(Particle.class,"y");z=field(Particle.class,"z");
                red=field(Particle.class,"rCol");green=field(Particle.class,"gCol");blue=field(Particle.class,"bCol");
                alpha=field(Particle.class,"alpha");
            }
            var queues=(Map<?,?>)particles.get(mc.particleEngine);
            for(Object queue:queues.values())for(Object value:(Iterable<?>)queue) {
                if(!(value instanceof SpellParticle particle) || seen.contains(particle))continue;
                if(Math.abs(x.getDouble(particle)-target.getX())>1
                        || Math.abs(z.getDouble(particle)-target.getZ())>1
                        || y.getDouble(particle)<target.getY()-0.5
                        || y.getDouble(particle)>target.getY()+target.getBbHeight()+1
                        || alpha.getFloat(particle)<=0)continue;
                seen.add(particle);
                int rgb=(Math.round(red.getFloat(particle)*255)<<16)
                        | (Math.round(green.getFloat(particle)*255)<<8) | Math.round(blue.getFloat(particle)*255);
                colors.merge(rgb,1,Integer::sum);
            }
        } catch(ReflectiveOperationException failure) { error=failure.toString(); }
    }

    static Map<String,Object> snapshot() {
        return Map.of("entity",watched,"ticks",ticks,"colors",new TreeMap<>(colors),"error",error==null?"":error);
    }
    private EffectParticleProbe() {}
}
