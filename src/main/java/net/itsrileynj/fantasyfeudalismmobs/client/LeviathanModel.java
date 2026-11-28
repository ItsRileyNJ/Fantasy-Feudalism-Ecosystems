package net.itsrileynj.fantasyfeudalismmobs.client;

import net.itsrileynj.fantasyfeudalismmobs.Leviathan;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

final class LeviathanModel extends HierarchicalModel<Leviathan> {
    private static final float[] SHARP_TURN_ANGLES = {.35F, .41F, .36F, .32F, .35F, .31F, .30F, .26F, .22F, .17F};
    private static final int[] CURL_DELAYS = {0, 1, 2, 3, 4, 5, 6, 8, 10, 11};
    private static final float[] STROKE_STRENGTHS = {.30F, .43F, .51F, .49F, .62F, .58F, .69F, .65F, .73F, .68F};
    private static final int[] TENTACLE_DELAYS = {0, 1, 2, 4, 6, 8, 9};
    private static final float[] TENTACLE_SPREAD = {.40F, .61F, .97F, 1.30F, 1.59F, 1.89F, 2.18F};

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart upperJaw;
    private final ModelPart jaw;
    private final ModelPart throat;
    private final ModelPart[] spine;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart[] leftTentacle;
    private final ModelPart[] rightTentacle;

    LeviathanModel(ModelPart root) {
        super(RenderType::entityTranslucentCull);
        this.root = root;
        body = root.getChild("body");
        upperJaw = body.getChild("upperJaw");
        jaw = body.getChild("jaw");
        throat = body.getChild("throat");
        var shoulder = body.getChild("shoulder");
        var trunk = shoulder.getChild("trunk");
        var tail = trunk.getChild("tail");
        var tailLink = tail.getChild("tailLink");
        var tailMid = tailLink.getChild("tailMid");
        var tailMidLink = tailMid.getChild("tailMidLink");
        var tailRear = tailMidLink.getChild("tailRear");
        var tailRearLink = tailRear.getChild("tailRearLink");
        var tailTip = tailRearLink.getChild("tailTip");
        spine = new ModelPart[]{shoulder, trunk, tail, tailLink, tailMid, tailMidLink,
                tailRear, tailRearLink, tailTip, tailTip.getChild("tailTipLink")};
        leftFin = body.getChild("leftFin");
        rightFin = body.getChild("rightFin");
        leftTentacle = tentacleChain(jaw.getChild("leftVentral"));
        rightTentacle = tentacleChain(jaw.getChild("rightVentral"));
    }

    private static ModelPart[] tentacleChain(ModelPart base) {
        var chain = new ModelPart[TENTACLE_DELAYS.length];
        chain[0] = base;
        for (int joint = 1; joint < chain.length; joint++) {
            chain[joint] = chain[joint - 1].getChild("segment_" + joint);
        }
        return chain;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Leviathan fish, float limbSwing, float limbSwingAmount, float age,
                          float headYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float partialTick = Mth.clamp(age - fish.tickCount, 0, 1);
        float stroke = fish.swimStroke(partialTick);
        float amplitude = fish.swimAmplitude(partialTick);
        body.xRot = Mth.lerp(partialTick, fish.xRotO, fish.getXRot()) * Mth.DEG_TO_RAD;
        float bank = -fish.swimCurve(0, partialTick) * .27F;
        body.zRot = bank + Mth.sin(stroke - .5F) * amplitude * .13F;
        for (int joint = 0; joint < spine.length; joint++) {
            // Model yaw and entity yaw turn in opposite directions.
            // Curl toward the inside of the turn, with a delay down the spine.
            float curve = -fish.swimCurve(CURL_DELAYS[joint], partialTick);
            float jointStroke = stroke + .8F - joint * .43F;
            float strength = amplitude * STROKE_STRENGTHS[joint] * (1F - .9F * Math.abs(curve));
            ModelPart section = spine[joint];
            section.yRot = curve * SHARP_TURN_ANGLES[joint] + Mth.sin(jointStroke) * strength;
            // A quarter-cycle offset gives the stroke a gentle circular path.
            section.xRot += Mth.cos(jointStroke) * strength * .24F;
        }
        spine[spine.length - 1].xRot += Mth.sin(stroke - 3.55F) * amplitude * .25F;
        float flare = fish.flare(0, partialTick);
        float finStroke = Mth.sin(stroke + .5F) * amplitude * .35F;
        leftFin.zRot += finStroke - bank * .25F + flare * .46F;
        rightFin.zRot -= finStroke + bank * .25F + flare * .46F;
        leftFin.yRot -= flare * .65F;
        rightFin.yRot += flare * .65F;
        float gape = fish.gape(partialTick);
        throat.visible = gape > .12F;
        jaw.xRot = gape;
        upperJaw.xRot = -.34F * gape;
        animateTentacle(leftTentacle, fish, -1F, partialTick, stroke, amplitude, gape, flare);
        animateTentacle(rightTentacle, fish, 1F, partialTick, stroke, amplitude, gape, flare);
    }

    private static void animateTentacle(ModelPart[] chain, Leviathan fish, float side, float partialTick,
                                        float stroke, float amplitude, float gape, float flare) {
        float previousYaw = 0F;
        float previousPitch = 0F;
        for (int joint = 0; joint < chain.length; joint++) {
            int delay = TENTACLE_DELAYS[joint];
            float opening = fish.flare(delay, partialTick);
            float drift = opening - fish.flare(delay + 3, partialTick);
            float distance = (float) joint / (chain.length - 1);
            float sway = Mth.sin(stroke - 1.6F - .15F * side) * amplitude * .7F * distance;
            float yaw = side * (opening * TENTACLE_SPREAD[joint] + .45F * distance * drift) + sway;
            float pitch = -.30F * (fish.flare(delay / 2, partialTick) - opening) + .22F * distance * drift;
            // These angles are measured from the base. Subtract the parent's
            // contribution to distribute the curl across the individual hinges.
            chain[joint].yRot = yaw - previousYaw;
            chain[joint].xRot = pitch - previousPitch;
            previousYaw = yaw;
            previousPitch = pitch;
        }
        chain[0].xRot -= .92F * gape + side * Mth.sin(stroke - .75F) * amplitude * .25F;
        chain[0].zRot -= side * flare * .20F;
    }
}
