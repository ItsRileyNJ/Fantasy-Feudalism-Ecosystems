package net.itsrileynj.fantasyfeudalismmobs.client;

import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Native layer built from the supplied Leviathian Blockbench geometry. */
final class LeviathanGeometry {
    private LeviathanGeometry() {}

    static LayerDefinition layer() {
        var mesh = new MeshDefinition();
        var body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0, 18, 0));
        var upperJaw = body.addOrReplaceChild("upperJaw", CubeListBuilder.create(), PartPose.offset(0F, -2F, -13F));
        var jaw = body.addOrReplaceChild("jaw", CubeListBuilder.create(), PartPose.offset(0F, -1F, -13F));
        var tail = body.addOrReplaceChild("tail", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0F, -2F, 9F, -.045F, 0F, 0F));
        var tailMid = tail.addOrReplaceChild("tailMid", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0F, 0F, 8F, .035F, 0F, 0F));
        var tailRear = tailMid.addOrReplaceChild("tailRear", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0F, 0F, 10F, .1F, 0F, 0F));
        var tailTip = tailRear.addOrReplaceChild("tailTip", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0F, 0F, 10F, .125F, 0F, 0F));
        var rightFin = body.addOrReplaceChild("rightFin", CubeListBuilder.create(), PartPose.offsetAndRotation(2.65F, 0F, -5F, -0.1309F, 0F, 0.418879F));
        var leftFin = body.addOrReplaceChild("leftFin", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.65F, 0F, -5F, -0.1309F, 0F, -0.418879F));
        cube(body, "original_0", 0, 0, -2F, -5F, -1F, 4F, 6F, 11F, PartPose.ZERO);
        cube(body, "original_16", 0, 64, 2F, -3F, -1F, 1F, 2F, 11F, PartPose.ZERO);
        cube(body, "original_17", 64, 64, -3F, -3F, -1F, 1F, 2F, 11F, PartPose.ZERO);
        cube(body, "original_20", 256, 64, -3F, -6F, -14F, 6F, 8F, 13F, PartPose.ZERO);
        cube(jaw, "original_21", 320, 64, -2F, -0.025F, -11F, 4F, 2.025F, 10F, PartPose.ZERO);
        cube(body, "original_22", 384, 64, 3F, -3F, -14F, 1F, 2F, 13F, PartPose.ZERO);
        cube(body, "original_23", 448, 64, -4F, -3F, -14F, 1F, 2F, 13F, PartPose.ZERO);
        cube(body, "original_29", 320, 96, -0.5F, -10F, -9F, 1F, 4F, 4F, PartPose.ZERO);
        cube(body, "original_30", 384, 96, -0.5F, -10F, -5F, 1F, 4F, 1F, PartPose.ZERO);
        cube(body, "original_31", 448, 96, -0.5F, -8F, -10F, 1F, 2F, 1F, PartPose.ZERO);
        cube(body, "original_32", 0, 128, -0.5F, -11F, -8F, 1F, 1F, 4F, PartPose.ZERO);
        cube(body, "original_33", 64, 128, -0.5F, -12F, -6F, 1F, 1F, 4F, PartPose.ZERO);
        cube(body, "original_34", 128, 128, -0.5F, -7F, -4F, 1F, 1F, 2F, PartPose.ZERO);
        cube(body, "original_35", 192, 128, -0.5F, -7F, -11F, 1F, 1F, 1F, PartPose.ZERO);
        cube(upperJaw, "original_36", 256, 128, -2F, -2F, -11F, 4F, 3F, 10F, PartPose.ZERO);
        cube(upperJaw, "original_50", 128, 192, -1F, -2.5F, -11F, 2F, 0.5F, 3F, PartPose.ZERO);
        cube(upperJaw, "original_51", 192, 192, -1F, -3.5F, -3F, 2F, 1.5F, 2F, PartPose.ZERO);
        cube(upperJaw, "original_52", 256, 192, -0.5F, -9F, 9F, 1F, 1F, 1F, PartPose.ZERO);
        cube(body, "original_53", 320, 192, -0.5F, -8F, -4F, 1F, 1F, 1F, PartPose.ZERO);
        cube(upperJaw, "mouth_roof", 384, 192, -1.63F, 0.86F, -10.8F, 3.26F, 0.26F, 9.8F, PartPose.ZERO);
        cube(jaw, "mouth_floor", 448, 192, -1.58F, -0.12F, -10.1F, 3.16F, 0.3F, 9.1F, PartPose.ZERO);
        cube(upperJaw, "left_gum", 0, 224, -1.99F, 0.81F, -10.75F, 0.32F, 0.26F, 9.55F, PartPose.ZERO);
        cube(upperJaw, "left_tooth_0", 64, 224, -0.14F, -0.27F, 0F, 0.28F, 0.68F, 0.25F, PartPose.offsetAndRotation(-1.77F, 1F, -10.4F, -0.226893F, 0F, 0F));
        cube(upperJaw, "left_tooth_1", 128, 224, -0.14F, -0.27F, 0F, 0.28F, 0.92F, 0.25F, PartPose.offsetAndRotation(-1.77F, 1F, -9.46F, 0.122173F, 0F, 0F));
        cube(upperJaw, "left_tooth_2", 192, 224, -0.14F, -0.27F, 0F, 0.28F, 0.63F, 0.25F, PartPose.offsetAndRotation(-1.77F, 1F, -8.52F, 0.122173F, 0F, 0F));
        cube(upperJaw, "left_tooth_3", 256, 224, -0.14F, -0.27F, 0F, 0.28F, 0.8F, 0.25F, PartPose.offsetAndRotation(-1.77F, 1F, -7.58F, -0.226893F, 0F, 0F));
        cube(upperJaw, "left_tooth_4", 320, 224, -0.14F, -0.27F, 0F, 0.28F, 1F, 0.25F, PartPose.offsetAndRotation(-1.77F, 1F, -6.64F, 0.122173F, 0F, 0F));
        cube(upperJaw, "left_tooth_5", 384, 224, -0.14F, -0.27F, 0F, 0.28F, 0.7F, 0.25F, PartPose.offsetAndRotation(-1.77F, 1F, -5.7F, 0.122173F, 0F, 0F));
        cube(upperJaw, "left_tooth_6", 448, 224, -0.14F, -0.27F, 0F, 0.28F, 0.94F, 0.25F, PartPose.offsetAndRotation(-1.77F, 1F, -4.76F, -0.226893F, 0F, 0F));
        cube(upperJaw, "left_tooth_7", 0, 256, -0.14F, -0.27F, 0F, 0.28F, 0.58F, 0.25F, PartPose.offsetAndRotation(-1.77F, 1F, -3.82F, 0.122173F, 0F, 0F));
        cube(upperJaw, "left_tooth_8", 64, 256, -0.14F, -0.27F, 0F, 0.28F, 0.73F, 0.25F, PartPose.offsetAndRotation(-1.77F, 1F, -2.88F, 0.122173F, 0F, 0F));
        cube(jaw, "left_gum", 128, 256, -1.95F, -0.08F, -10.75F, 0.32F, 0.32F, 9.55F, PartPose.ZERO);
        cube(jaw, "left_tooth_0", 192, 256, -0.14F, -0.41F, 0F, 0.28F, 0.68F, 0.25F, PartPose.offsetAndRotation(-1.73F, 0F, -10.09F, 0.226893F, 0F, 0F));
        cube(jaw, "left_tooth_1", 256, 256, -0.14F, -0.65F, 0F, 0.28F, 0.92F, 0.25F, PartPose.offsetAndRotation(-1.73F, 0F, -9.15F, -0.122173F, 0F, 0F));
        cube(jaw, "left_tooth_2", 320, 256, -0.14F, -0.36F, 0F, 0.28F, 0.63F, 0.25F, PartPose.offsetAndRotation(-1.73F, 0F, -8.21F, -0.122173F, 0F, 0F));
        cube(jaw, "left_tooth_3", 384, 256, -0.14F, -0.53F, 0F, 0.28F, 0.8F, 0.25F, PartPose.offsetAndRotation(-1.73F, 0F, -7.27F, 0.226893F, 0F, 0F));
        cube(jaw, "left_tooth_4", 448, 256, -0.14F, -0.73F, 0F, 0.28F, 1F, 0.25F, PartPose.offsetAndRotation(-1.73F, 0F, -6.33F, -0.122173F, 0F, 0F));
        cube(jaw, "left_tooth_5", 0, 288, -0.14F, -0.43F, 0F, 0.28F, 0.7F, 0.25F, PartPose.offsetAndRotation(-1.73F, 0F, -5.39F, -0.122173F, 0F, 0F));
        cube(jaw, "left_tooth_6", 64, 288, -0.14F, -0.67F, 0F, 0.28F, 0.94F, 0.25F, PartPose.offsetAndRotation(-1.73F, 0F, -4.45F, 0.226893F, 0F, 0F));
        cube(jaw, "left_tooth_7", 128, 288, -0.14F, -0.31F, 0F, 0.28F, 0.58F, 0.25F, PartPose.offsetAndRotation(-1.73F, 0F, -3.51F, -0.122173F, 0F, 0F));
        cube(jaw, "left_tooth_8", 192, 288, -0.14F, -0.46F, 0F, 0.28F, 0.73F, 0.25F, PartPose.offsetAndRotation(-1.73F, 0F, -2.57F, -0.122173F, 0F, 0F));
        cube(upperJaw, "right_gum", 256, 288, 1.67F, 0.81F, -10.75F, 0.32F, 0.26F, 9.55F, PartPose.ZERO);
        cube(upperJaw, "right_tooth_0", 320, 288, -0.14F, -0.27F, 0F, 0.28F, 0.68F, 0.25F, PartPose.offsetAndRotation(1.77F, 1F, -10.4F, -0.226893F, 0F, 0F));
        cube(upperJaw, "right_tooth_1", 384, 288, -0.14F, -0.27F, 0F, 0.28F, 0.92F, 0.25F, PartPose.offsetAndRotation(1.77F, 1F, -9.46F, 0.122173F, 0F, 0F));
        cube(upperJaw, "right_tooth_2", 448, 288, -0.14F, -0.27F, 0F, 0.28F, 0.63F, 0.25F, PartPose.offsetAndRotation(1.77F, 1F, -8.52F, 0.122173F, 0F, 0F));
        cube(upperJaw, "right_tooth_3", 0, 320, -0.14F, -0.27F, 0F, 0.28F, 0.8F, 0.25F, PartPose.offsetAndRotation(1.77F, 1F, -7.58F, -0.226893F, 0F, 0F));
        cube(upperJaw, "right_tooth_4", 64, 320, -0.14F, -0.27F, 0F, 0.28F, 1F, 0.25F, PartPose.offsetAndRotation(1.77F, 1F, -6.64F, 0.122173F, 0F, 0F));
        cube(upperJaw, "right_tooth_5", 128, 320, -0.14F, -0.27F, 0F, 0.28F, 0.7F, 0.25F, PartPose.offsetAndRotation(1.77F, 1F, -5.7F, 0.122173F, 0F, 0F));
        cube(upperJaw, "right_tooth_6", 192, 320, -0.14F, -0.27F, 0F, 0.28F, 0.94F, 0.25F, PartPose.offsetAndRotation(1.77F, 1F, -4.76F, -0.226893F, 0F, 0F));
        cube(upperJaw, "right_tooth_7", 256, 320, -0.14F, -0.27F, 0F, 0.28F, 0.58F, 0.25F, PartPose.offsetAndRotation(1.77F, 1F, -3.82F, 0.122173F, 0F, 0F));
        cube(upperJaw, "right_tooth_8", 320, 320, -0.14F, -0.27F, 0F, 0.28F, 0.73F, 0.25F, PartPose.offsetAndRotation(1.77F, 1F, -2.88F, 0.122173F, 0F, 0F));
        cube(jaw, "right_gum", 384, 320, 1.63F, -0.08F, -10.75F, 0.32F, 0.32F, 9.55F, PartPose.ZERO);
        cube(jaw, "right_tooth_0", 448, 320, -0.14F, -0.41F, 0F, 0.28F, 0.68F, 0.25F, PartPose.offsetAndRotation(1.73F, 0F, -10.09F, 0.226893F, 0F, 0F));
        cube(jaw, "right_tooth_1", 0, 352, -0.14F, -0.65F, 0F, 0.28F, 0.92F, 0.25F, PartPose.offsetAndRotation(1.73F, 0F, -9.15F, -0.122173F, 0F, 0F));
        cube(jaw, "right_tooth_2", 64, 352, -0.14F, -0.36F, 0F, 0.28F, 0.63F, 0.25F, PartPose.offsetAndRotation(1.73F, 0F, -8.21F, -0.122173F, 0F, 0F));
        cube(jaw, "right_tooth_3", 128, 352, -0.14F, -0.53F, 0F, 0.28F, 0.8F, 0.25F, PartPose.offsetAndRotation(1.73F, 0F, -7.27F, 0.226893F, 0F, 0F));
        cube(jaw, "right_tooth_4", 192, 352, -0.14F, -0.73F, 0F, 0.28F, 1F, 0.25F, PartPose.offsetAndRotation(1.73F, 0F, -6.33F, -0.122173F, 0F, 0F));
        cube(jaw, "right_tooth_5", 256, 352, -0.14F, -0.43F, 0F, 0.28F, 0.7F, 0.25F, PartPose.offsetAndRotation(1.73F, 0F, -5.39F, -0.122173F, 0F, 0F));
        cube(jaw, "right_tooth_6", 320, 352, -0.14F, -0.67F, 0F, 0.28F, 0.94F, 0.25F, PartPose.offsetAndRotation(1.73F, 0F, -4.45F, 0.226893F, 0F, 0F));
        cube(jaw, "right_tooth_7", 384, 352, -0.14F, -0.31F, 0F, 0.28F, 0.58F, 0.25F, PartPose.offsetAndRotation(1.73F, 0F, -3.51F, -0.122173F, 0F, 0F));
        cube(jaw, "right_tooth_8", 448, 352, -0.14F, -0.46F, 0F, 0.28F, 0.73F, 0.25F, PartPose.offsetAndRotation(1.73F, 0F, -2.57F, -0.122173F, 0F, 0F));
        var leftVentral = jaw.addOrReplaceChild("leftVentral", CubeListBuilder.create(), PartPose.offset(-1.7F, 0.95F, -3.25F));
        var leftVentralTip = leftVentral.addOrReplaceChild("leftVentralTip", CubeListBuilder.create(), PartPose.offset(-6.9F, 5.36F, 8.75F));
        leftVentral.addOrReplaceChild("web", CubeListBuilder.create()
                .texOffs(0, 384).addBox(-8F, 0F, 0F, 8F, 0F, 14F, Set.of(Direction.UP, Direction.DOWN)),
                PartPose.offsetAndRotation(0F, 0F, 0F, -.32F, 0F, -0.43F));
        cube(leftVentral, "sweep_0", 64, 384, -0.595F, -0.465F, -0.11F, 1.19F, 0.93F, 3.594167F, PartPose.offsetAndRotation(0F, 0F, 0F, -0.427852F, -0.924038F, 0F));
        cube(leftVentral, "sweep_1", 128, 384, -0.47F, -0.35F, -0.11F, 0.94F, 0.7F, 4.5818F, PartPose.offsetAndRotation(-2.45F, 1.4F, 1.85F, -0.510176F, -0.751953F, 0F));
        cube(leftVentral, "sweep_2", 192, 384, -0.355F, -0.27F, -0.11F, 0.71F, 0.54F, 5.092966F, PartPose.offsetAndRotation(-5.05F, 3.53F, 4.63F, -0.384981F, -0.422046F, 0F));
        cube(leftVentralTip, "sweep_3", 256, 384, -0.28F, -0.21F, -0.11F, 0.56F, 0.42F, 5.898908F, PartPose.offsetAndRotation(0F, 0F, 0F, -0.177013F, -0.179853F, 0F));
        cube(leftVentralTip, "sweep_4", 320, 384, -0.19F, -0.155F, -0.11F, 0.38F, 0.31F, 5.668541F, PartPose.offsetAndRotation(-1F, 1F, 5.5F, -0.130682F, -0.027771F, 0F));
        cube(leftVentralTip, "sweep_5", 384, 384, -0.12F, -0.1F, -0.11F, 0.24F, 0.2F, 5.970652F, PartPose.offsetAndRotation(-1.15F, 1.71F, 10.9F, -0.052192F, 0.122195F, 0F));
        cube(leftVentralTip, "sweep_6", 448, 384, -0.07F, -0.06F, -0.11F, 0.14F, 0.12F, 4.374323F, PartPose.offsetAndRotation(-0.45F, 2.01F, 16.6F, 0.052982F, 0.268366F, 0F));
        var rightVentral = jaw.addOrReplaceChild("rightVentral", CubeListBuilder.create(), PartPose.offset(1.7F, 0.95F, -3.25F));
        var rightVentralTip = rightVentral.addOrReplaceChild("rightVentralTip", CubeListBuilder.create(), PartPose.offset(6.9F, 5.36F, 8.75F));
        rightVentral.addOrReplaceChild("web", CubeListBuilder.create()
                .texOffs(0, 416).addBox(0F, 0F, 0F, 8F, 0F, 14F, Set.of(Direction.UP, Direction.DOWN)),
                PartPose.offsetAndRotation(0F, 0F, 0F, -.32F, 0F, 0.43F));
        cube(rightVentral, "sweep_0", 64, 416, -0.595F, -0.465F, -0.11F, 1.19F, 0.93F, 3.594167F, PartPose.offsetAndRotation(0F, 0F, 0F, -0.427852F, 0.924038F, 0F));
        cube(rightVentral, "sweep_1", 128, 416, -0.47F, -0.35F, -0.11F, 0.94F, 0.7F, 4.5818F, PartPose.offsetAndRotation(2.45F, 1.4F, 1.85F, -0.510176F, 0.751953F, 0F));
        cube(rightVentral, "sweep_2", 192, 416, -0.355F, -0.27F, -0.11F, 0.71F, 0.54F, 5.092966F, PartPose.offsetAndRotation(5.05F, 3.53F, 4.63F, -0.384981F, 0.422046F, 0F));
        cube(rightVentralTip, "sweep_3", 256, 416, -0.28F, -0.21F, -0.11F, 0.56F, 0.42F, 5.898908F, PartPose.offsetAndRotation(0F, 0F, 0F, -0.177013F, 0.179853F, 0F));
        cube(rightVentralTip, "sweep_4", 320, 416, -0.19F, -0.155F, -0.11F, 0.38F, 0.31F, 5.668541F, PartPose.offsetAndRotation(1F, 1F, 5.5F, -0.130682F, 0.027771F, 0F));
        cube(rightVentralTip, "sweep_5", 384, 416, -0.12F, -0.1F, -0.11F, 0.24F, 0.2F, 5.970652F, PartPose.offsetAndRotation(1.15F, 1.71F, 10.9F, -0.052192F, -0.122195F, 0F));
        cube(rightVentralTip, "sweep_6", 448, 416, -0.07F, -0.06F, -0.11F, 0.14F, 0.12F, 4.374323F, PartPose.offsetAndRotation(0.45F, 2.01F, 16.6F, 0.052982F, -0.268366F, 0F));
        leftFin.addOrReplaceChild("pectoral_web", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-13F, 0F, -3F, 13F, 0F, 15F, Set.of(Direction.UP, Direction.DOWN)),
                PartPose.ZERO);
        rightFin.addOrReplaceChild("pectoral_web", CubeListBuilder.create()
                .texOffs(128, 0).addBox(0F, 0F, -3F, 13F, 0F, 15F, Set.of(Direction.UP, Direction.DOWN)),
                PartPose.ZERO);
        cube(tail, "body_section_0", 192, 0, -1.8F, -2.2F, -0.3F, 3.6F, 4.4F, 3.3F, PartPose.ZERO);
        cube(tail, "body_section_1", 256, 0, -1.5F, -1.9F, 2.8F, 3F, 3.8F, 3.2F, PartPose.ZERO);
        cube(tail, "body_section_2", 320, 0, -1.2F, -1.575F, 5.8F, 2.4F, 3.15F, 2.8F, PartPose.ZERO);
        cube(tailMid, "body_section_0", 384, 0, -1.06F, -1.425F, -0.6F, 2.12F, 2.85F, 4.4F, PartPose.ZERO);
        cube(tailMid, "body_section_1", 448, 0, -0.91F, -1.255F, 3.5F, 1.82F, 2.51F, 3.6F, PartPose.ZERO);
        cube(tailMid, "body_section_2", 0, 32, -0.765F, -1.09F, 6.8F, 1.53F, 2.18F, 4.05F, PartPose.ZERO);
        cube(tailRear, "body_section_0", 64, 32, -0.67F, -0.98F, -0.7F, 1.34F, 1.96F, 4.3F, PartPose.ZERO);
        cube(tailRear, "body_section_1", 128, 32, -0.56F, -0.855F, 3.35F, 1.12F, 1.71F, 3.75F, PartPose.ZERO);
        cube(tailRear, "body_section_2", 192, 32, -0.44F, -0.695F, 6.9F, 0.88F, 1.39F, 3.9F, PartPose.ZERO);
        cube(tailTip, "body_section_0", 256, 32, -0.37F, -0.595F, -0.65F, 0.74F, 1.19F, 3.8F, PartPose.ZERO);
        cube(tailTip, "body_section_1", 320, 32, -0.27F, -0.455F, 2.9F, 0.54F, 0.91F, 3.45F, PartPose.ZERO);
        cube(tailTip, "body_section_2", 384, 32, -0.18F, -0.325F, 6.1F, 0.36F, 0.65F, 3.2F, PartPose.ZERO);
        cube(tailTip, "body_section_3", 448, 32, -0.08F, -0.18F, 9.1F, 0.16F, 0.36F, 2.9F, PartPose.ZERO);
        body.addOrReplaceChild("dorsal_ribbon", CubeListBuilder.create()
                .texOffs(128, 64).addBox(0F, -13F, -5F, 0F, 11F, 15F, Set.of(Direction.WEST, Direction.EAST)),
                PartPose.ZERO);
        body.addOrReplaceChild("ventral_ribbon", CubeListBuilder.create()
                .texOffs(192, 64).addBox(0F, -2F, -5F, 0F, 11F, 15F, Set.of(Direction.WEST, Direction.EAST)),
                PartPose.ZERO);
        tail.addOrReplaceChild("dorsal_ribbon", CubeListBuilder.create()
                .texOffs(0, 96).addBox(0F, -11F, 0F, 0F, 11F, 9F, Set.of(Direction.WEST, Direction.EAST)),
                PartPose.ZERO);
        tail.addOrReplaceChild("ventral_ribbon", CubeListBuilder.create()
                .texOffs(64, 96).addBox(0F, 0F, 0F, 0F, 11F, 9F, Set.of(Direction.WEST, Direction.EAST)),
                PartPose.ZERO);
        tailMid.addOrReplaceChild("dorsal_ribbon", CubeListBuilder.create()
                .texOffs(128, 96).addBox(0F, -11F, 0F, 0F, 11F, 11F, Set.of(Direction.WEST, Direction.EAST)),
                PartPose.ZERO);
        tailMid.addOrReplaceChild("ventral_ribbon", CubeListBuilder.create()
                .texOffs(192, 96).addBox(0F, 0F, 0F, 0F, 11F, 11F, Set.of(Direction.WEST, Direction.EAST)),
                PartPose.ZERO);
        tailRear.addOrReplaceChild("dorsal_ribbon", CubeListBuilder.create()
                .texOffs(256, 96).addBox(0F, -11F, 0F, 0F, 11F, 11F, Set.of(Direction.WEST, Direction.EAST)),
                PartPose.ZERO);
        tailRear.addOrReplaceChild("ventral_ribbon", CubeListBuilder.create()
                .texOffs(320, 128).addBox(0F, 0F, 0F, 0F, 11F, 11F, Set.of(Direction.WEST, Direction.EAST)),
                PartPose.ZERO);
        tailTip.addOrReplaceChild("dorsal_ribbon", CubeListBuilder.create()
                .texOffs(384, 128).addBox(0F, -11F, 0F, 0F, 11F, 12F, Set.of(Direction.WEST, Direction.EAST)),
                PartPose.ZERO);
        tailTip.addOrReplaceChild("ventral_ribbon", CubeListBuilder.create()
                .texOffs(448, 128).addBox(0F, 0F, 0F, 0F, 11F, 12F, Set.of(Direction.WEST, Direction.EAST)),
                PartPose.ZERO);
        tailTip.addOrReplaceChild("tail_fin", CubeListBuilder.create()
                .texOffs(0, 448).addBox(0F, -8F, 9.5F, 0F, 18F, 22F, Set.of(Direction.WEST, Direction.EAST)),
                PartPose.ZERO);
        body.addOrReplaceChild("throat", CubeListBuilder.create()
                .texOffs(0, 160).addBox(-2F, -2F, -14.02F, 4F, 4F, 0F, Set.of(Direction.NORTH)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 512, 512);
    }

    private static void cube(PartDefinition part, String name, int u, int v, float x, float y, float z,
                             float width, float height, float length, PartPose pose) {
        part.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, width, height, length), pose);
    }
}
