package net.itsrileynj.fantasyfeudalismmobs.rootworm.client;

import net.itsrileynj.fantasyfeudalismmobs.rootworm.RootWormModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWorm;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWormShape;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;

/** Long root-bark worm: a plated snout and articulated bite, with most detail in its segmented body. */
public final class RootWormModel extends HierarchicalModel<RootWorm> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(RootWormModule.id("root_worm"), "main");
    private final ModelPart root;
    private final ModelPart[] segments = new ModelPart[RootWormShape.SEGMENTS.size()];
    private final ModelPart upperJaw;
    private final ModelPart lowerJaw;
    private final ModelPart leftJaw;
    private final ModelPart rightJaw;
    private final ModelPart eyes;

    public RootWormModel(ModelPart root) {
        this.root = root;
        for (int i = 0; i < segments.length; i++) segments[i] = root.getChild("segment" + i);
        upperJaw = segments[0].getChild("upper_jaw");
        lowerJaw = segments[0].getChild("lower_jaw");
        leftJaw = segments[0].getChild("left_jaw");
        rightJaw = segments[0].getChild("right_jaw");
        eyes = segments[0].getChild("eyes");

    }

    public static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        for (int i = 0; i < RootWormShape.SEGMENTS.size(); i++) {
            var shape = RootWormShape.SEGMENTS.get(i);
            float w = shape.width(), h = shape.height();
            var segment = root.addOrReplaceChild("segment" + i,
                    CubeListBuilder.create().texOffs(0, i == 0 ? 0 : 16)
                            .addBox(-w / 2, -h, -shape.length() / 2.0F, w, h, shape.length()),
                    PartPose.offset(0, 24, shape.z()));
            if (i == 0) {
                // Recessed lateral eyes sit beneath layered bark plates, without cartoon cheeks or a smile.
                segment.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-4.2F, -5.2F, -2.9F, 2.0F, 0.9F, 2.4F)
                        .addBox(2.2F, -5.2F, -2.9F, 2.0F, 0.9F, 2.4F)
                        .texOffs(0, 40).addBox(-2.9F, -5.45F, -1.9F, 5.8F, 0.7F, 3.2F), PartPose.ZERO);
                var eyeRoot = segment.addOrReplaceChild("eyes", CubeListBuilder.create(), PartPose.offset(0, -3.775F, -2.25F));
                for (int side : new int[]{-1, 1}) {
                    // Identical mesh/UVs; rotate the left assembly outward instead of using a different cube face.
                    var eye = eyeRoot.addOrReplaceChild(side < 0 ? "left_eye" : "right_eye", CubeListBuilder.create().texOffs(112, 0)
                            .addBox(0, -0.325F, -0.55F, 0.25F, 0.65F, 1.1F),
                            PartPose.offsetAndRotation(side * 3.9F, 0, 0, 0, side < 0 ? Mth.PI : 0, 0));

                }
                // The throat is behind the closed overlapping plates; it becomes visible as they spread.
                segment.addOrReplaceChild("throat", CubeListBuilder.create().texOffs(96, 16)
                        .addBox(-2.8F, -4.6F, -2.7F, 5.6F, 4.3F, 0.15F), PartPose.ZERO);
                var upper = segment.addOrReplaceChild("upper_jaw", CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-3.0F, -2.2F, -3.6F, 6.0F, 2.25F, 3.9F)
                        .texOffs(0, 40).addBox(-2.6F, -2.65F, -2.6F, 5.2F, 0.55F, 2.3F),
                        PartPose.offset(0, -2.45F, -1.7F));
                upper.addOrReplaceChild("toothlets", CubeListBuilder.create().texOffs(112, 0)
                        .addBox(-2.15F, 0, -3.0F, 0.55F, 0.75F, 0.7F)
                        .addBox(1.6F, 0, -3.0F, 0.55F, 0.75F, 0.7F), PartPose.ZERO);
                var lower = segment.addOrReplaceChild("lower_jaw", CubeListBuilder.create().texOffs(32, 16)
                        .addBox(-2.8F, 0, -3.5F, 5.6F, 2.05F, 3.8F), PartPose.offset(0, -2.45F, -1.7F));
                lower.addOrReplaceChild("toothlets", CubeListBuilder.create().texOffs(112, 0)
                        .addBox(-1.8F, -0.65F, -2.9F, 0.5F, 0.7F, 0.65F)
                        .addBox(1.3F, -0.65F, -2.9F, 0.5F, 0.7F, 0.65F), PartPose.ZERO);
                segment.addOrReplaceChild("left_jaw", CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-1.0F, -1.55F, -3.0F, 1.2F, 3.1F, 3.4F), PartPose.offset(-2.9F, -2.5F, -1.9F));
                segment.addOrReplaceChild("right_jaw", CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-0.2F, -1.55F, -3.0F, 1.2F, 3.1F, 3.4F), PartPose.offset(2.9F, -2.5F, -1.9F));
            } else {
                segment.addOrReplaceChild("bark_ring", CubeListBuilder.create().texOffs(0, 40)
                        .addBox(-w / 2 - 0.15F, -h - 0.15F, -0.55F, w + 0.3F, h + 0.15F, 1.1F), PartPose.ZERO);
                if (i < 8) {
                    segment.addOrReplaceChild("root_nubs", CubeListBuilder.create().texOffs(32, 16)
                            .addBox(-w / 2 - 0.65F, -1.5F, -0.5F, 0.9F, 0.6F, 1.3F)
                            .addBox(w / 2 - 0.25F, -1.5F, -0.5F, 0.9F, 0.6F, 1.3F), PartPose.ZERO);

                }
            }
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override public ModelPart root() { return root; }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, int color) {
        // Draw the eye boxes in a separate solid-black pass, preserving their native blink transform.
        boolean visible=eyes.visible;
        eyes.visible=false;
        try { super.renderToBuffer(pose,consumer,light,overlay,color); }
        finally { eyes.visible=visible; }
        if(!root.visible || !segments[0].visible || !visible)return;
        pose.pushPose();
        root.translateAndRotate(pose);
        segments[0].translateAndRotate(pose);
        eyes.render(pose,consumer,light,overlay,color & 0xFF000000);
        pose.popPose();
    }

    @Override public void setupAnim(RootWorm worm, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float partialTick = Mth.clamp(ageInTicks - worm.tickCount, 0, 1);
        float search = worm.getSearchAmount(partialTick);
        float movement = Math.min(1.0F, limbSwingAmount * 3) * (1 - search);
        for (int i = 0; i < segments.length; i++) {
            var pose = worm.getSearchPose(i, partialTick);
            float wave = limbSwing * 2.2F - i * 0.65F;
            segments[i].yRot = pose.yaw() + Mth.sin(wave) * 0.22F * movement;
            segments[i].xRot = -pose.pitch();
            segments[i].x = pose.x() + Mth.sin(wave) * 1.2F * movement;
            segments[i].z = pose.z();
            segments[i].y -= pose.lift() + Math.abs(Mth.cos(wave)) * 0.5F * movement;
            segments[i].y -= Mth.sin(ageInTicks * 0.08F - i * 0.6F) * 0.06F * (1 - search);
        }
        segments[0].yRot += worm.getVisualLookYaw(partialTick) * Mth.DEG_TO_RAD * 0.3F * (1 - search);
        eyes.yScale = ageInTicks % 140 > 136 ? 0.15F : 1.0F;
        float mouth = worm.getMouthOpenAmount(ageInTicks - worm.tickCount);
        upperJaw.xRot = -mouth * 0.75F;
        lowerJaw.xRot = mouth * 0.72F;
        leftJaw.yRot = mouth * 0.58F;
        rightJaw.yRot = -mouth * 0.58F;
        segments[0].xRot += (0.06F + Mth.sin(ageInTicks * 0.5F) * 0.05F) * worm.getFeedingAmount(partialTick) * (1 - search);
        // Resolve the head from a shared neck joint after every look, nod and listening rotation.
        var offset=net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWormSearchPose.headAttachmentOffset(
                -segments[0].xRot,segments[0].yRot,-segments[1].xRot,segments[1].yRot);
        segments[0].x=segments[1].x+(float)offset.x;
        segments[0].y=segments[1].y+(float)offset.y;
        segments[0].z=segments[1].z+(float)offset.z;
    }
}
