package net.itsrileynj.fantasyfeudalismmobs.client;

import net.itsrileynj.fantasyfeudalismmobs.Leviathan;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

final class LeviathanModel extends HierarchicalModel<Leviathan> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart upperJaw;
    private final ModelPart jaw;
    private final ModelPart throat;
    private final ModelPart tail;
    private final ModelPart tailTip;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart leftVentral;
    private final ModelPart rightVentral;
    private final ModelPart leftVentralTip;
    private final ModelPart rightVentralTip;

    LeviathanModel(ModelPart root) {
        super(RenderType::entityTranslucentCull);
        this.root = root;
        body = root.getChild("body");
        upperJaw = body.getChild("upperJaw");
        jaw = body.getChild("jaw");
        throat = body.getChild("throat");
        tail = body.getChild("tail");
        tailTip = tail.getChild("tailTip");
        leftFin = body.getChild("leftFin");
        rightFin = body.getChild("rightFin");
        leftVentral = jaw.getChild("leftVentral");
        rightVentral = jaw.getChild("rightVentral");
        leftVentralTip = leftVentral.getChild("leftVentralTip");
        rightVentralTip = rightVentral.getChild("rightVentralTip");
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
        byte phase = fish.huntPhase();
        boolean stare = phase == Leviathan.STARE;
        boolean charge = phase == Leviathan.LUNGE;
        float stroke = age * (charge ? .44F : .14F);
        float amplitude = stare ? .018F : charge ? .31F : .19F;
        body.xRot = Mth.lerp(partialTick, fish.xRotO, fish.getXRot()) * Mth.DEG_TO_RAD;
        body.zRot = Mth.sin(stroke - .5F) * amplitude * .13F;
        tail.yRot = Mth.sin(stroke) * amplitude;
        tailTip.yRot = Mth.sin(stroke - .75F) * amplitude * 1.5F;
        leftFin.zRot += Mth.sin(stroke + .5F) * amplitude * .35F;
        rightFin.zRot -= Mth.sin(stroke + .5F) * amplitude * .35F;
        float gape = fish.gape(partialTick);
        throat.visible = gape > .12F;
        jaw.xRot = gape;
        upperJaw.xRot = -.34F * gape;
        float finSweep = Mth.sin(stroke - .75F) * amplitude * .25F;
        // The roots follow the chin, with the fins still trailing behind during a bite.
        float finPitch = (charge ? .06F : 0F) - gape;
        leftVentral.xRot = finPitch + finSweep;
        rightVentral.xRot = finPitch - finSweep;
        leftVentralTip.yRot = Mth.sin(stroke - 1.6F) * amplitude * .7F;
        rightVentralTip.yRot = Mth.sin(stroke - 1.9F) * amplitude * .7F;
    }
}
