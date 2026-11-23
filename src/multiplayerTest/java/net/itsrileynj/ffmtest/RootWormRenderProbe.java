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
        int frames,lastTick=-1;float lastFeeding,maxFeedingStep,minFeeding=1,maxFeeding;double flatGap,raisedGap;boolean finite=true;float minBlink=1,maxBlink=0,maxSearch,maxMouth,minWave=Float.POSITIVE_INFINITY,maxWave=Float.NEGATIVE_INFINITY;
        void record(RootWorm worm,RootWormModel model) {
            float feeding=worm.getFeedingAmount(1);minFeeding=Math.min(minFeeding,feeding);maxFeeding=Math.max(maxFeeding,feeding);
            if(worm.tickCount==lastTick+1)maxFeedingStep=Math.max(maxFeedingStep,Math.abs(feeding-lastFeeding));
            if(worm.tickCount!=lastTick){lastTick=worm.tickCount;lastFeeding=feeding;}
            frames++;var head=model.root().getChild("segment0");float blink=head.getChild("eyes").yScale;
            minBlink=Math.min(minBlink,blink);maxBlink=Math.max(maxBlink,blink);maxMouth=Math.max(maxMouth,Math.abs(head.getChild("upper_jaw").xRot));maxSearch=Math.max(maxSearch,worm.getSearchAmount(1));
            var neck=model.root().getChild("segment1");
            var headJoint=new org.joml.Vector3f(0,-2.5F,2).rotate(new org.joml.Quaternionf().rotationZYX(head.zRot,head.yRot,head.xRot)).add(head.x,head.y,head.z);
            var neckJoint=new org.joml.Vector3f(0,-2.5F,-2.5F).rotate(new org.joml.Quaternionf().rotationZYX(neck.zRot,neck.yRot,neck.xRot)).add(neck.x,neck.y,neck.z);
            double gap=headJoint.distance(neckJoint);
            if(worm.getSearchAmount(1)>.99F)raisedGap=Math.max(raisedGap,gap);else if(worm.getSearchAmount(1)<.001F)flatGap=Math.max(flatGap,gap);
            var body=model.root().getChild("segment3");minWave=Math.min(minWave,body.x);maxWave=Math.max(maxWave,body.x);
            model.root().getAllParts().forEach(p->{if(!Float.isFinite(p.x)||!Float.isFinite(p.y)||!Float.isFinite(p.z)||!Float.isFinite(p.xRot)||!Float.isFinite(p.yRot)||!Float.isFinite(p.zRot))finite=false;});
        }
        Map<String,Object> snapshot() { var result=new LinkedHashMap<String,Object>();result.putAll(Map.of("frames",frames,"finite",finite,"blinkMin",minBlink,"blinkMax",maxBlink,"maxSearch",maxSearch,"maxMouth",maxMouth,"slitherRange",frames==0?0:maxWave-minWave));result.put("flatNeckGap",flatGap);result.put("raisedNeckGap",raisedGap);result.put("maxFeedingStep",maxFeedingStep);result.put("minFeeding",minFeeding);result.put("maxFeeding",maxFeeding);return result; }
    }
}
