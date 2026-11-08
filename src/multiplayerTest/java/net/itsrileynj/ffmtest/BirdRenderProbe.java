package net.itsrileynj.ffmtest;

import java.util.*;
import net.itsrileynj.fantasyfeudalismmobs.ForestBird;
import net.itsrileynj.fantasyfeudalismmobs.client.ForestBirdModel;

/** Opt-in native rendered-pose evidence; omitted from production artifacts. */
public final class BirdRenderProbe {
    private static final Map<Integer,Probe> PROBES=new HashMap<>();
    private BirdRenderProbe() {}
    public static void record(ForestBird bird,ForestBirdModel model,float partial) {
        PROBES.computeIfAbsent(bird.getId(),id->new Probe()).record(bird,model,partial);
    }
    public static Map<Integer,Map<String,Object>> snapshot() {
        var result=new HashMap<Integer,Map<String,Object>>();PROBES.forEach((id,p)->result.put(id,p.snapshot()));return result;
    }
    private static final class Probe {
        int frames,groundFrames,flightFrames;
        double minWing=Double.POSITIVE_INFINITY,maxWing=Double.NEGATIVE_INFINITY;
        double minHead=Double.POSITIVE_INFINITY,maxHead=Double.NEGATIVE_INFINITY;
        double minLeg=Double.POSITIVE_INFINITY,maxLeg=Double.NEGATIVE_INFINITY;
        String kind;
        boolean finite=true;
        Map<String,Object> latest=Map.of();
        void record(ForestBird entity,ForestBirdModel model,float partial) {
            var root=model.root().getChild("bird");var head=root.getChild("head");
            var wing=root.getChild("left_wing");var rightWing=root.getChild("right_wing");var leg=root.getChild("left_leg");
            float flight=entity.flightAmount(partial);kind=entity.kind().id;frames++;
            minHead=Math.min(minHead,head.yRot);maxHead=Math.max(maxHead,head.yRot);
            if(flight>.95F) { flightFrames++;minWing=Math.min(minWing,wing.zRot);maxWing=Math.max(maxWing,wing.zRot); }
            if(flight<.05F) { groundFrames++;minLeg=Math.min(minLeg,leg.xRot);maxLeg=Math.max(maxLeg,leg.xRot); }
            model.root().getAllParts().forEach(p->{if(!Float.isFinite(p.x)||!Float.isFinite(p.y)||!Float.isFinite(p.z)||!Float.isFinite(p.xRot)||!Float.isFinite(p.yRot)||!Float.isFinite(p.zRot))finite=false;});
            latest=Map.of("flight",flight,"wingLeft",wing.zRot,"wingRight",rightWing.zRot,"headYaw",head.yRot,"leg",leg.xRot,"bodyPitch",root.xRot);
        }
        Map<String,Object> snapshot() {
            var map=new LinkedHashMap<String,Object>();map.put("frames",frames);map.put("kind",kind);map.put("finite",finite);map.put("groundFrames",groundFrames);map.put("flightFrames",flightFrames);
            map.put("headYawRange",frames>0?maxHead-minHead:0);map.put("flyingWingRange",flightFrames>0?maxWing-minWing:0);
            map.put("walkingLegRange",groundFrames>0?maxLeg-minLeg:0);map.put("latest",latest);return map;
        }
    }
}
