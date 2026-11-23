package net.itsrileynj.fantasyfeudalismmobs.scorpion.client;

import net.itsrileynj.fantasyfeudalismmobs.scorpion.*;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Bronze plated carapace, eight articulated legs, two pincers and a five-jointed venom tail. */
public final class ScorpionModel extends HierarchicalModel<Scorpion> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(ScorpionModule.id("scorpion"),"main");
    public static final ModelLayerLocation LAVA_LAYER=new ModelLayerLocation(ScorpionModule.id("lava_scorpion"),"main");
    private final ModelPart root,head,abdomen,bulb;
    private final ModelPart[] legs=new ModelPart[8],knees=new ModelPart[8],arms=new ModelPart[2],palms=new ModelPart[2],jaws=new ModelPart[4],tail=new ModelPart[5],chelicerae=new ModelPart[2],cheliceraFangs=new ModelPart[2],feelers=new ModelPart[2];
    public ScorpionModel(ModelPart root) {
        this.root=root;head=root.getChild("head");abdomen=root.getChild("abdomen");
        for(int i=0;i<8;i++){legs[i]=root.getChild("leg"+i);knees[i]=legs[i].getChild("knee");}
        for(int i=0;i<2;i++){arms[i]=root.getChild("arm"+i);palms[i]=arms[i].getChild("forearm").getChild("palm");for(int j=0;j<2;j++)jaws[i*2+j]=palms[i].getChild("finger"+j);}
        for(int i=0;i<2;i++){chelicerae[i]=head.getChild("chelicera"+i);cheliceraFangs[i]=chelicerae[i].getChild("fang");feelers[i]=head.getChild("feeler"+i);}
        tail[0]=root.getChild("tail0");for(int i=1;i<5;i++)tail[i]=tail[i-1].getChild("tail"+i);bulb=tail[4].getChild("bulb");
    }
    public static LayerDefinition layer() {return layer(false);}
    public static LayerDefinition lavaLayer() {return layer(true);}
    private static LayerDefinition layer(boolean lava) {
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        var abdomen=root.addOrReplaceChild("abdomen",CubeListBuilder.create(),PartPose.offset(0,20,3.5F));
        float[] widths={11.5F,11.2F,10.1F,8.8F,7.1F};
        // Five plain, butt-joined shell sections replace the overlapping armor/edge layers.
        for(int i=0;i<widths.length;i++) {
            float w=widths[i];
            abdomen.addOrReplaceChild("section"+i,CubeListBuilder.create().texOffs(lava?64:0,i*7)
                    .addBox(-w/2,-2.8F,-1.5F,w,4,3),PartPose.offset(0,0,-4+i*3));
        }
        var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(lava?64:0,0)
                .addBox(-6,-2.8F,-4.2F,12,3.8F,7.7F),PartPose.offset(0,20,-5.5F));
        // Graduated front-facing eye cluster, matching the reference's large outer eyes.
        head.addOrReplaceChild("eye_mask",CubeListBuilder.create().texOffs(64,0)
                .addBox(-5.1F,-2.45F,-.1F,10.2F,2,.28F),PartPose.offset(0,0,-4.3F));
        for(int side:new int[]{-1,1}) {
            head.addOrReplaceChild("outer_eye"+side,CubeListBuilder.create().texOffs(96,96)
                    .addBox(-.55F,-.8F,-.17F,1.1F,1.6F,.34F),PartPose.offsetAndRotation(side*4.1F,-1.65F,-4.5F,0,0,side*.35F));
            head.addOrReplaceChild("middle_eye"+side,CubeListBuilder.create().texOffs(96,96)
                    .addBox(-.32F,-.4F,-.17F,.64F,.8F,.34F),PartPose.offsetAndRotation(side*2.4F,-1.5F,-4.5F,0,0,side*.2F));
            head.addOrReplaceChild("inner_eye"+side,CubeListBuilder.create().texOffs(96,96)
                    .addBox(-.2F,-.225F,-.17F,.4F,.45F,.34F),PartPose.offset(side*.9F,-1.4F,-4.5F));
        }
        head.addOrReplaceChild("mouth",CubeListBuilder.create().texOffs(lava?0:64,lava?64:0)
                .addBox(-2.1F,-.9F,-.25F,4.2F,1.8F,.5F),PartPose.offset(0,.25F,-4.35F));
        for(int i=0;i<2;i++) {
            int side=i==0?-1:1;
            var chelicera=head.addOrReplaceChild("chelicera"+i,CubeListBuilder.create().texOffs(lava?64:0,0)
                    .addBox(-.95F,-.9F,-2.1F,1.9F,2,2.55F)
                    .texOffs(lava?0:64,lava?64:0).addBox(-.275F+side*.45F,-.35F,-3.5F,.55F,1,1.8F),
                    PartPose.offsetAndRotation(side*1.35F,.35F,-4.45F,0,-side*.15F,0));
            chelicera.addOrReplaceChild("fang",CubeListBuilder.create().texOffs(lava?0:64,lava?64:0)
                    .addBox(-.35F,-.55F,-1.9F,.7F,1.1F,2.1F)
                    .addBox(-.22F,-.2F,-2.7F,.44F,.7F,.95F),PartPose.offsetAndRotation(-side*.5F,.35F,-1.8F,.25F,-side*.35F,0));
            // Paired tapered cheek feelers slope forward/downward beside the chelicerae.
            head.addOrReplaceChild("feeler"+i,CubeListBuilder.create().texOffs(lava?0:64,lava?64:0)
                    .addBox(-.38F,-.25F,-.32F,.76F,3.1F,.64F)
                    .addBox(-.23F,2.6F,-.24F,.46F,2.3F,.48F)
                    .addBox(-.1F,4.6F,-.1F,.2F,1.8F,.2F),PartPose.offsetAndRotation(side*4.65F,-1.8F,-4.05F,-.65F,0,side*.17F));
        }
        float[] hipWidths={5.3F,5.2F,4.6F,3.8F};
        for(int pair=0;pair<4;pair++)for(int s=0;s<2;s++) {
            int side=s==0?-1:1,index=pair*2+s;float yaw=side*(.7F-pair*.45F);
            var leg=root.addOrReplaceChild("leg"+index,CubeListBuilder.create().texOffs(lava?64:0,0).addBox(side<0?-4.8F:0,-.65F,-.7F,4.8F,1.3F,1.4F)
                    .texOffs(lava?0:64,lava?64:0).addBox(side<0?-4.85F:3.9F,-.8F,-.8F,.95F,1.6F,1.6F),PartPose.offsetAndRotation(side*hipWidths[pair],20.5F,-5+pair*4,0,yaw,-side*.35F));
            var knee=leg.addOrReplaceChild("knee",CubeListBuilder.create().texOffs(lava?64:0,0).addBox(side<0?-3.5F:0,-.5F,-.5F,3.5F,1,1)
                    .texOffs(lava?0:64,lava?64:0).addBox(side<0?-6.2F:3.3F,-.35F,-.35F,2.9F,.7F,.7F),PartPose.offsetAndRotation(side*4.8F,0,0,0,0,side*1.25F));
            knee.addOrReplaceChild("point",CubeListBuilder.create().texOffs(lava?0:64,lava?64:0).addBox(side<0?-1:0,-.22F,-.22F,1,.44F,.44F),PartPose.offset(side*6.2F,0,0));
        }
        for(int i=0;i<2;i++) {
            int side=i==0?-1:1;
            var arm=root.addOrReplaceChild("arm"+i,CubeListBuilder.create().texOffs(lava?64:0,0).addBox(-1.2F,-1.1F,-6.8F,2.4F,2.6F,7.6F)
                    .texOffs(lava?0:64,lava?64:0).addBox(-1.3F,-1.2F,-6.8F,2.6F,2.8F,.6F),PartPose.offsetAndRotation(side*4.8F,20.3F,-7,0,-side*1.1F,0));
            var forearm=arm.addOrReplaceChild("forearm",CubeListBuilder.create().texOffs(lava?64:0,0).addBox(-1.4F,-1.4F,-5,2.8F,2.8F,5.6F)
                    .texOffs(0,64).addBox(-1,-1.55F,-4.2F,2,.25F,4),PartPose.offsetAndRotation(0,0,-6.8F,0,side*1.0F,0));
            // Plain palms and two-piece fingers keep the claws simple and connected.
            var palm=forearm.addOrReplaceChild("palm",CubeListBuilder.create().texOffs(lava?64:0,0)
                    .addBox(-3.6F,-2.6F,-6.6F,7.2F,5.2F,7.2F),PartPose.offsetAndRotation(0,0,-5,0,side*.12F,0));
            for(int finger=0;finger<2;finger++) {
                int sign=finger==0?-1:1;
                boolean outer=sign==side;
                float width=outer?2.34F:1.98F,height=outer?4.46F:3.78F;
                // Original bulky finger, with a single step down to a narrower end.
                // Both cubes share the finger pivot so the tip follows every attack pose.
                palm.addOrReplaceChild("finger"+finger,CubeListBuilder.create().texOffs(lava?0:64,lava?64:0)
                        .addBox(-width/2,-height/2,-3.7F,width,height,4.1F)
                        .addBox(-width*.3F-sign*.5F,-height*.3F,-6.5F,width*.6F,height*.6F,2.85F),
                        PartPose.offsetAndRotation(sign*2.05F,0,-6.3F,0,sign*.14F,0));
            }
        }
        PartDefinition previous=null;
        float[] angles={.9F,.65F,.65F,.5F,.35F},tailWidths={3.8F,3.6F,3.3F,3.0F,2.8F};
        for(int i=0;i<5;i++) {
            float w=tailWidths[i];
            // One shell cube per link eliminates coincident cap/collar faces.
            var cube=CubeListBuilder.create().texOffs(lava?64:0,0).addBox(-w/2,-1.6F,0,w,3.2F,4.7F);
            previous=(i==0?root:previous).addOrReplaceChild("tail"+i,cube,PartPose.offsetAndRotation(0,i==0?19:0,i==0?11:4.4F,angles[i],0,0));
        }
        var bulb=previous.addOrReplaceChild("bulb",CubeListBuilder.create().texOffs(lava?64:0,0).addBox(-2.1F,-1.9F,-.4F,4.2F,3.8F,5.8F),PartPose.offsetAndRotation(0,0,4.4F,.2F,0,0));
        var hook=bulb.addOrReplaceChild("hook",CubeListBuilder.create().texOffs(lava?0:64,lava?64:0).addBox(-.9F,-.75F,0,1.8F,1.5F,3.4F),PartPose.offsetAndRotation(0,0,5,.45F,0,0));
        var tip=hook.addOrReplaceChild("venom_tip",CubeListBuilder.create().texOffs(lava?0:64,lava?64:0)
                .addBox(-.48F,-.4F,0,.96F,.8F,2.6F),PartPose.offsetAndRotation(0,0,3.1F,.28F,0,0));
        tip.addOrReplaceChild("needle_point",CubeListBuilder.create().texOffs(lava?0:64,lava?64:0)
                .addBox(-.17F,-.17F,0,.34F,.34F,2),PartPose.offsetAndRotation(0,0,2.35F,.22F,0,0));
        return LayerDefinition.create(mesh,128,128);
    }
    @Override public ModelPart root() {return root;}
    @Override public void setupAnim(Scorpion scorpion,float swing,float swingAmount,float age,float headYaw,float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float move=Math.min(1,swingAmount*3),partial=Mth.clamp(age-scorpion.tickCount,0,1),sting=scorpion.getStingAmount(partial),pincerOpen=scorpion.getPincerOpenAmount(partial);
        // The simplified carapace stays rigid; its mouthparts animate independently.
        for(int pair=0;pair<4;pair++)for(int s=0;s<2;s++) {
            int index=pair*2+s,side=s==0?-1:1;float phase=swing*1.9F+pair*Mth.PI+(s==0?0:Mth.PI);
            legs[index].yRot+=Mth.cos(phase)*.32F*move;
            legs[index].zRot-=side*Math.max(0,Mth.sin(phase))*.3F*move;
            knees[index].zRot+=side*Mth.sin(phase)*.2F*move;
        }
        for(int i=0;i<2;i++) {
            int side=i==0?-1:1;
            feelers[i].zRot+=side*Mth.sin(age*.09F)*.025F;
            feelers[i].xRot+=Mth.sin(age*.12F+i)*.02F-sting*.08F;
            chelicerae[i].yRot+=side*(Mth.sin(age*.16F)*.08F+sting*.12F);
            cheliceraFangs[i].yRot+=side*(.18F+Mth.sin(age*.18F)*.1F+sting*.18F);
            arms[i].yRot+=side*(Mth.sin(age*.07F)*.04F-sting*.15F);palms[i].xRot+=sting*.12F;
            for(int j=0;j<2;j++) {
                int sign=j==0?-1:1;
                // Crab-style chela: the larger outer jaw stays fixed; the inner jaw hinges open.
                if(sign!=side)jaws[i*2+j].yRot-=sign*pincerOpen*.5F;
            }
        }
        for(int i=0;i<tail.length;i++)tail[i].xRot+=Mth.sin(age*.065F-i*.35F)*.025F;
        tail[0].xRot+=.5F*sting;bulb.xRot-=.35F*sting;
    }
}
