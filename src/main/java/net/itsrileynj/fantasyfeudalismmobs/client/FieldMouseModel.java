package net.itsrileynj.fantasyfeudalismmobs.client;

import net.itsrileynj.fantasyfeudalismmobs.FieldMouse;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** A small white mouse with rounded-looking ears, black eyes and an articulated pink tail. */
public final class FieldMouseModel extends HierarchicalModel<FieldMouse> {
    private final ModelPart root,head,tail;
    private final ModelPart[] feet;
    public FieldMouseModel(ModelPart root){
        this.root=root;head=root.getChild("head");tail=root.getChild("tail");
        feet=new ModelPart[]{root.getChild("front_left"),root.getChild("front_right"),root.getChild("back_left"),root.getChild("back_right")};
    }
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-1.5F,-2.8F,-2.5F,3,2.5F,5),net.minecraft.client.model.geom.PartPose.offset(0,24,0));
        var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(16,0).addBox(-1.15F,-1.6F,-2,2.3F,1.8F,2.4F),net.minecraft.client.model.geom.PartPose.offset(0,22.3F,-2.2F));
        head.addOrReplaceChild("ears",CubeListBuilder.create().texOffs(16,16)
                .addBox(-1.6F,-2.7F,-.3F,1.2F,1.6F,.6F).addBox(.4F,-2.7F,-.3F,1.2F,1.6F,.6F),net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("eyes",CubeListBuilder.create().texOffs(48,0)
                .addBox(-1.22F,-1.1F,-1.5F,.25F,.45F,.45F).addBox(.97F,-1.1F,-1.5F,.25F,.45F,.45F),net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("nose",CubeListBuilder.create().texOffs(16,16).addBox(-.35F,-.75F,-2.25F,.7F,.5F,.4F),net.minecraft.client.model.geom.PartPose.ZERO);
        root.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(0,16).addBox(-.25F,-.25F,0,.5F,.5F,4),net.minecraft.client.model.geom.PartPose.offset(0,23.2F,2.3F));
        for(String side:new String[]{"left","right"})for(String end:new String[]{"front","back"})
            root.addOrReplaceChild(end+"_"+side,CubeListBuilder.create().texOffs(24,16).addBox(-.35F,0,-.6F,.7F,.6F,1.2F),
                    net.minecraft.client.model.geom.PartPose.offset(side.equals("left")?1.1F:-1.1F,23.4F,end.equals("front")?-1.7F:1.6F));
        return LayerDefinition.create(mesh,64,32);
    }
    @Override public ModelPart root(){return root;}
    @Override public void setupAnim(FieldMouse mouse,float limbSwing,float amount,float age,float yaw,float pitch){
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot=yaw*Mth.DEG_TO_RAD;head.xRot=pitch*Mth.DEG_TO_RAD;
        tail.yRot=Mth.sin(limbSwing*.8F)*amount*.35F;
        for(int i=0;i<feet.length;i++)feet[i].xRot=Mth.cos(limbSwing*1.8F+(i==0||i==3?0:Mth.PI))*amount*.8F;
    }
}
