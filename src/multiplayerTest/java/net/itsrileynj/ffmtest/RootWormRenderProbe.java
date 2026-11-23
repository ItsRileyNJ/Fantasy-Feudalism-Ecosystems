package net.itsrileynj.ffmtest;

import java.util.*;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWorm;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.client.RootWormModel;

/** Opt-in observations of native rendering; excluded from the distributable mod. */
public final class RootWormRenderProbe {
    private static final Map<Integer,Probe> PROBES=new HashMap<>();
    private RootWormRenderProbe() {}
    public static void record(RootWorm worm,RootWormModel model) { PROBES.computeIfAbsent(worm.getId(),id->new Probe()).record(worm,model); }
    public static Map<Integer,Map<String,Object>> snapshot() { var result=new HashMap<Integer,Map<String,Object>>();PROBES.forEach((id,p)->result.put(id,p.snapshot()));return result; }
    private static final class Probe {
        int frames;boolean finite=true;float minBlink=1,maxBlink=0,maxSearch,maxMouth,minWave=Float.POSITIVE_INFINITY,maxWave=Float.NEGATIVE_INFINITY;
        void record(RootWorm worm,RootWormModel model) {
            frames++;var head=model.root().getChild("segment0");float blink=head.getChild("eyes").yScale;
            minBlink=Math.min(minBlink,blink);maxBlink=Math.max(maxBlink,blink);maxMouth=Math.max(maxMouth,Math.abs(head.getChild("upper_jaw").xRot));maxSearch=Math.max(maxSearch,worm.getSearchAmount(1));
            var body=model.root().getChild("segment3");minWave=Math.min(minWave,body.x);maxWave=Math.max(maxWave,body.x);
            model.root().getAllParts().forEach(p->{if(!Float.isFinite(p.x)||!Float.isFinite(p.y)||!Float.isFinite(p.z)||!Float.isFinite(p.xRot)||!Float.isFinite(p.yRot)||!Float.isFinite(p.zRot))finite=false;});
        }
        Map<String,Object> snapshot() { return Map.of("frames",frames,"finite",finite,"blinkMin",minBlink,"blinkMax",maxBlink,"maxSearch",maxSearch,"maxMouth",maxMouth,"slitherRange",frames==0?0:maxWave-minWave); }
    }
}
