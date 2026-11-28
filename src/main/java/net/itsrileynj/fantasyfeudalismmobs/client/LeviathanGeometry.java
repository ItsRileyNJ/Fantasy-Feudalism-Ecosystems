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
        var tail = body.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0F, -2F, 10F));
        var tailTip = tail.addOrReplaceChild("tailTip", CubeListBuilder.create(), PartPose.offset(0F, 0F, 6F));
        var rightFin = body.addOrReplaceChild("rightFin", CubeListBuilder.create(), PartPose.offsetAndRotation(3F, 0F, -5F, -0.1309F, 0F, 0.305433F));
        var leftFin = body.addOrReplaceChild("leftFin", CubeListBuilder.create(), PartPose.offsetAndRotation(-3F, 0F, -5F, -0.1309F, 0F, -0.305433F));
        cube(body, "original_0", 0, 0, -2F, -5F, -1F, 4F, 6F, 11F, PartPose.ZERO);
        cube(tail, "original_1", 64, 0, -1F, -2F, 0F, 2F, 4F, 6F, PartPose.ZERO);
        cube(tailTip, "original_2", 128, 0, -1F, -3F, 0F, 2F, 6F, 5F, PartPose.ZERO);
        cube(tailTip, "original_3", 192, 0, -1F, -5F, 3F, 2F, 2F, 5F, PartPose.ZERO);
        cube(tailTip, "original_4", 256, 0, -1F, 3F, 3F, 2F, 2F, 5F, PartPose.ZERO);
        cube(tail, "original_5", 320, 0, -1F, -2.5F, 4F, 2F, 0.5F, 2F, PartPose.ZERO);
        cube(tail, "original_6", 384, 0, -1F, 2F, 4F, 2F, 0.5F, 2F, PartPose.ZERO);
        cube(tailTip, "original_7", 448, 0, -1F, -5.5F, 6F, 2F, 0.5F, 2F, PartPose.ZERO);
        cube(tailTip, "original_8", 0, 32, -1F, 5F, 6F, 2F, 0.5F, 2F, PartPose.ZERO);
        cube(tailTip, "original_9", 64, 32, -1F, -3F, 5F, 2F, 2F, 1F, PartPose.ZERO);
        cube(tailTip, "original_10", 128, 32, -1F, 1F, 5F, 2F, 2F, 1F, PartPose.ZERO);
        cube(tailTip, "original_11", 192, 32, -1F, -1F, 5F, 2F, 2F, 0.5F, PartPose.ZERO);
        cube(tailTip, "original_12", 256, 32, -1F, -3.5F, 1F, 2F, 0.5F, 2F, PartPose.ZERO);
        cube(tailTip, "original_13", 320, 32, -1F, 3F, 1F, 2F, 0.5F, 2F, PartPose.ZERO);
        cube(tail, "original_14", 384, 32, -1F, -2.5F, 0F, 2F, 0.5F, 2F, PartPose.ZERO);
        cube(tail, "original_15", 448, 32, -1F, 2F, 0F, 2F, 0.5F, 2F, PartPose.ZERO);
        cube(body, "original_16", 0, 64, 2F, -3F, -1F, 1F, 2F, 11F, PartPose.ZERO);
        cube(body, "original_17", 64, 64, -3F, -3F, -1F, 1F, 2F, 11F, PartPose.ZERO);
        cube(tail, "original_18", 128, 64, -2F, -1F, 0F, 1F, 2F, 5F, PartPose.ZERO);
        cube(tail, "original_19", 192, 64, 1F, -1F, 0F, 1F, 2F, 5F, PartPose.ZERO);
        cube(body, "original_20", 256, 64, -3F, -6F, -14F, 6F, 8F, 13F, PartPose.ZERO);
        cube(jaw, "original_21", 320, 64, -2F, -0.025F, -11F, 4F, 2.025F, 10F, PartPose.ZERO);
        cube(body, "original_22", 384, 64, 3F, -3F, -14F, 1F, 2F, 13F, PartPose.ZERO);
        cube(body, "original_23", 448, 64, -4F, -3F, -14F, 1F, 2F, 13F, PartPose.ZERO);
        cube(body, "original_26", 128, 96, -0.5F, -2F, -1F, 1F, 2F, 1F, PartPose.offsetAndRotation(0F, -4F, 9F, -0.305433F, 0F, 0F));
        cube(body, "original_27", 192, 96, -0.5F, -1F, -1F, 1F, 2F, 1F, PartPose.offsetAndRotation(0F, -5F, 5F, -0.305433F, 0F, 0F));
        cube(body, "original_28", 256, 96, -0.5F, -1F, -1F, 1F, 2F, 1F, PartPose.offsetAndRotation(0F, -5F, 1F, -0.305433F, 0F, 0F));
        cube(body, "original_29", 320, 96, -0.5F, -10F, -9F, 1F, 4F, 4F, PartPose.ZERO);
        cube(body, "original_30", 384, 96, -0.5F, -10F, -5F, 1F, 4F, 1F, PartPose.ZERO);
        cube(body, "original_31", 448, 96, -0.5F, -8F, -10F, 1F, 2F, 1F, PartPose.ZERO);
        cube(body, "original_32", 0, 128, -0.5F, -11F, -8F, 1F, 1F, 4F, PartPose.ZERO);
        cube(body, "original_33", 64, 128, -0.5F, -12F, -6F, 1F, 1F, 4F, PartPose.ZERO);
        cube(body, "original_34", 128, 128, -0.5F, -7F, -4F, 1F, 1F, 2F, PartPose.ZERO);
        cube(body, "original_35", 192, 128, -0.5F, -7F, -11F, 1F, 1F, 1F, PartPose.ZERO);
        cube(upperJaw, "original_36", 256, 128, -2F, -2F, -11F, 4F, 3F, 10F, PartPose.ZERO);
        cube(leftFin, "original_37", 320, 128, -2F, -1F, -3F, 2F, 1F, 6F, PartPose.ZERO);
        cube(rightFin, "original_38", 384, 128, 0F, -1F, -3F, 2F, 1F, 6F, PartPose.ZERO);
        cube(rightFin, "original_39", 448, 128, 2F, -1F, -2F, 2F, 1F, 6F, PartPose.ZERO);
        cube(leftFin, "original_40", 0, 160, -4F, -1F, -2F, 2F, 1F, 6F, PartPose.ZERO);
        cube(rightFin, "original_41", 64, 160, 4F, -1F, 0F, 2F, 1F, 6F, PartPose.ZERO);
        cube(leftFin, "original_42", 128, 160, -6F, -1F, 0F, 2F, 1F, 6F, PartPose.ZERO);
        cube(leftFin, "original_43", 192, 160, -5F, -1F, -1F, 1F, 1F, 1F, PartPose.ZERO);
        cube(rightFin, "original_44", 256, 160, 4F, -1F, -1F, 1F, 1F, 1F, PartPose.ZERO);
        cube(rightFin, "original_45", 320, 160, 6F, -1F, 2F, 1F, 1F, 8F, PartPose.ZERO);
        cube(leftFin, "original_46", 384, 160, -7F, -1F, 2F, 1F, 1F, 8F, PartPose.ZERO);
        cube(body, "original_47", 448, 160, -1.5F, -3F, 15F, 0.5F, 2F, 5F, PartPose.ZERO);
        cube(tailTip, "original_48", 0, 192, 1F, -1F, -1F, 0.5F, 2F, 5F, PartPose.ZERO);
        cube(tail, "original_49", 64, 192, -1F, -3F, -31F, 2F, 1F, 5F, PartPose.ZERO);
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
        body.addOrReplaceChild("throat", CubeListBuilder.create()
                .texOffs(0, 448).addBox(-2F, -2F, -14.02F, 4F, 4F, 0F, Set.of(Direction.NORTH)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 512, 512);
    }

    private static void cube(PartDefinition part, String name, int u, int v, float x, float y, float z,
                             float width, float height, float length, PartPose pose) {
        part.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, width, height, length), pose);
    }
}
