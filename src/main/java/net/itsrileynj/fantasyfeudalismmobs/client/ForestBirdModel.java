package net.itsrileynj.fantasyfeudalismmobs.client;

import java.util.List;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

public final class ForestBirdModel extends HierarchicalModel<ForestBird> {
    private final ModelPart root,bird,head,leftWing,rightWing,leftLeg,rightLeg,tail;
    private final List<ModelPart> parts;
    public ForestBirdModel(ModelPart root) {
        this.root=root;bird=root.getChild("bird");head=bird.getChild("head");
        leftWing=bird.getChild("left_wing");rightWing=bird.getChild("right_wing");
        leftLeg=bird.getChild("left_leg");rightLeg=bird.getChild("right_leg");tail=bird.getChild("tail");parts=root.getAllParts().toList();
    }
    @Override public ModelPart root() { return root; }
    @Override public void setupAnim(ForestBird entity,float walk,float amount,float age,float yaw,float pitch) {
        for(var part:parts)part.resetPose();
        float flying=entity.flightAmount(Mth.clamp(age-entity.tickCount,0,1));
        float flap=Mth.sin(age*(entity.kind()==BirdKind.CROW?1.15F:1.8F)+entity.getId()*.7F);
        leftWing.zRot=Mth.lerp(flying,-1.15F,.15F+flap*.9F);rightWing.zRot=-leftWing.zRot;
        leftWing.xRot=rightWing.xRot=(1-flying)*.12F;
        leftLeg.xRot=Mth.lerp(flying,Mth.cos(walk*2.5F)*amount*1.1F,-.7F);
        rightLeg.xRot=Mth.lerp(flying,Mth.cos(walk*2.5F+Mth.PI)*amount*1.1F,-.7F);
        head.yRot=Mth.clamp(yaw*Mth.DEG_TO_RAD,-1.2F,1.2F);
        head.xRot=Mth.clamp(pitch*Mth.DEG_TO_RAD,-.5F,.5F)-flying*.1F;
        bird.xRot=flying*.16F;
        bird.y+=flying*flap*.12F+(1-flying)*Math.abs(Mth.sin(walk*2.5F))*Math.min(amount,.5F)*.2F;
        tail.xRot=flying*.1F+Mth.sin(walk*.8F)*amount*.05F*(1-flying);
    }
}
