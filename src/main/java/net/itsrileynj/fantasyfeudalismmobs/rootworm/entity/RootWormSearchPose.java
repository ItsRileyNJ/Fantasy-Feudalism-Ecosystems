package net.itsrileynj.fantasyfeudalismmobs.rootworm.entity;

import net.minecraft.util.Mth;

/** Shared search centerline, in model pixels. Rendering and damage parts follow the same curve. */
public final class RootWormSearchPose {
    // Rear five segments stay planted; only the front four rise to listen.
    private static final int FIRST_GROUNDED_SEGMENT = 4;
    private static final float[] PITCH = {-0.15F, 0.75F, 1.15F, 0.85F, 0, 0, 0, 0, 0};
    public record Pose(float x, float lift, float z, float pitch, float yaw) {}

    public static Pose sample(int segment, float amount, float age) {
        amount = Mth.clamp(amount, 0, 1);
        float scan = Mth.sin(age * 0.09F) * 0.45F * amount;
        float x = 0, lift = 0, z = RootWormShape.SEGMENTS.get(segment).z();
        if (segment < FIRST_GROUNDED_SEGMENT && amount > 0) {
            z = RootWormShape.SEGMENTS.get(FIRST_GROUNDED_SEGMENT).z();
            for (int i = FIRST_GROUNDED_SEGMENT - 1; i >= segment; i--) {
                float length = RootWormShape.SEGMENTS.get(i + 1).z() - RootWormShape.SEGMENTS.get(i).z();
                float pitch = (PITCH[i] + PITCH[i + 1]) * 0.5F * amount;
                float yaw = scan * (FIRST_GROUNDED_SEGMENT - 0.5F - i) / FIRST_GROUNDED_SEGMENT;
                float horizontal = length * Mth.cos(pitch);
                x -= horizontal * Mth.sin(yaw);
                lift += length * Mth.sin(pitch);
                z -= horizontal * Mth.cos(yaw);
            }
        }
        return new Pose(x, lift, z, PITCH[segment] * amount, scan * Math.max(0, FIRST_GROUNDED_SEGMENT - segment) / FIRST_GROUNDED_SEGMENT);
    }

    /** Extra AABB coverage for tilted body segments; unchanged when the worm is resting. */
    public static float width(int segment, Pose pose) {
        return RootWormShape.hitWidth(segment) + Math.abs(Mth.sin(pose.pitch())) * RootWormShape.SEGMENTS.get(segment).height() / 16;
    }
    public static float height(int segment, Pose pose) {
        return RootWormShape.hitHeight(segment) + Math.abs(Mth.sin(pose.pitch())) * RootWormShape.SEGMENTS.get(segment).length() / 16;
    }
    public static float bottom(int segment, Pose pose) {
        return RootWormShape.hitBottom(segment) + pose.lift() / 16
                - Math.abs(Mth.sin(pose.pitch())) * RootWormShape.SEGMENTS.get(segment).length() / 32;
    }
    private RootWormSearchPose() {}
}
