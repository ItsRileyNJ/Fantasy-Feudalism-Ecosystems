package net.itsrileynj.ffmtest;

import java.util.*;
import net.itsrileynj.fantasyfeudalismmobs.scorpion.Scorpion;
import net.itsrileynj.fantasyfeudalismmobs.scorpion.client.ScorpionModel;

/** Native client render observations, never part of the released mod. */
public final class ScorpionRenderProbe {
    private static final Map<Integer,Probe> PROBES=new HashMap<>();
    public static void record(Scorpion scorpion,ScorpionModel model) {PROBES.computeIfAbsent(scorpion.getId(),id->new Probe()).record(scorpion,model);}
    public static Map<Integer,Map<String,Object>> snapshot() {var out=new HashMap<Integer,Map<String,Object>>();PROBES.forEach((id,p)->out.put(id,p.snapshot()));return out;}
    private static final class Range {
        float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
        void add(float value){min=Math.min(min,value);max=Math.max(max,value);}
        float span(){return Float.isFinite(min)?max-min:0;}
    }
    private static final class Probe {
        int frames;float maxArch,minStrikeTipZ,maxAttackTailYaw,maxEating,maxPincerOpen,maxIdlePincerAngle;boolean finite=true;float maxSting,minLeg=Float.POSITIVE_INFINITY,maxLeg=Float.NEGATIVE_INFINITY,minTail=Float.POSITIVE_INFINITY,maxTail=Float.NEGATIVE_INFINITY;
        final Range idleJaw=new Range(),idleTailYaw=new Range(),walkingTailYaw=new Range(),walkingFeelerPitch=new Range();
        void record(Scorpion scorpion,ScorpionModel model) {
            if(!scorpion.isAttacking()&&!scorpion.isEating()) {
                float yaw=model.root().getChild("tail0").yRot;
                if(scorpion.walkAnimation.speed()>.02F) {
                    walkingTailYaw.add(yaw);var feeler=model.root().getChild("head").getChild("feeler0");
                    walkingFeelerPitch.add(feeler.xRot+feeler.getChild("mid").xRot+feeler.getChild("mid").getChild("tip").xRot);
                }
                else {idleTailYaw.add(yaw);idleJaw.add(model.root().getChild("arm0").getChild("forearm").getChild("palm").getChild("finger1").yRot);}
            }
            for(int i=0;i<2;i++) {
                var palm=model.root().getChild("arm"+i).getChild("forearm").getChild("palm");
                for(int j=0;j<2;j++) {
                    float angle=Math.abs(palm.getChild("finger"+j).yRot);
                    maxPincerOpen=Math.max(maxPincerOpen,angle);
                    if(!scorpion.isAttacking()&&!scorpion.isEating())maxIdlePincerAngle=Math.max(maxIdlePincerAngle,angle);
                }
            }
            if(scorpion.isStinging()) {
                maxArch=Math.max(maxArch,model.root().getChild("abdomen").xRot);
                if(scorpion.getAttackBlend(0)>.999F&&scorpion.getAttackBlend(1)>.999F)maxAttackTailYaw=Math.max(maxAttackTailYaw,Math.abs(model.root().getChild("tail0").yRot));
                var pose=new com.mojang.blaze3d.vertex.PoseStack();var part=model.root().getChild("tail0");part.translateAndRotate(pose);
                for(int i=1;i<5;i++){part=part.getChild("tail"+i);part.translateAndRotate(pose);}
                for(String name:List.of("bulb","hook","venom_tip","needle_point")){part=part.getChild(name);part.translateAndRotate(pose);}
                var tip=pose.last().pose().transformPosition(new org.joml.Vector3f(0,0,.125F));minStrikeTipZ=Math.min(minStrikeTipZ,tip.z);
            }
            maxEating=Math.max(maxEating,scorpion.getEatAmount(1));
            frames++;maxSting=Math.max(maxSting,scorpion.getStingAmount(1));float leg=model.root().getChild("leg0").yRot,tail=model.root().getChild("tail0").xRot;
            minLeg=Math.min(minLeg,leg);maxLeg=Math.max(maxLeg,leg);minTail=Math.min(minTail,tail);maxTail=Math.max(maxTail,tail);
            for(int i=0;i<8;i++)model.root().getChild("leg"+i).getChild("knee");
            model.root().getAllParts().forEach(p->{if(!Float.isFinite(p.x)||!Float.isFinite(p.y)||!Float.isFinite(p.z)||!Float.isFinite(p.xRot)||!Float.isFinite(p.yRot)||!Float.isFinite(p.zRot))finite=false;});
        }
        Map<String,Object> snapshot() {return Map.of("frames",frames,"finite",finite,"legs",8,"maxSting",maxSting,"legRange",maxLeg-minLeg,"tailRange",maxTail-minTail,"maxPincerOpen",maxPincerOpen,"maxIdlePincerAngle",maxIdlePincerAngle,"animationRanges",Map.of("idleJaw",idleJaw.span(),"idleTailYaw",idleTailYaw.span(),"walkingTailYaw",walkingTailYaw.span(),"maxEating",maxEating,"maxArch",maxArch,"minStrikeTipZ",minStrikeTipZ,"maxAttackTailYaw",maxAttackTailYaw,"walkingFeelerPitch",walkingFeelerPitch.span()));}
    }
}
