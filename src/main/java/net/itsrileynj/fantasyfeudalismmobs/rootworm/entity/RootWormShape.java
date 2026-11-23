package net.itsrileynj.fantasyfeudalismmobs.rootworm.entity;

import java.util.List;

/** Common geometry used by the client model and the server/client damage parts. Units are model pixels. */
public final class RootWormShape {
    public record Segment(int width, int height, int length, float z) {}
    public static final List<Segment> SEGMENTS = List.of(
            new Segment(8, 5, 5, -16), new Segment(8, 6, 5, -11.5F), new Segment(8, 6, 5, -7),
            new Segment(7, 5, 5, -2.5F), new Segment(7, 5, 5, 2), new Segment(6, 4, 4, 6.5F),
            new Segment(5, 4, 4, 10), new Segment(4, 3, 3, 13.5F), new Segment(2, 2, 3, 16));

    public static float hitWidth(int index) {
        if (index == 0) return 0.92F; // Includes the snout, open mouth plates and small head turns.
        var segment = SEGMENTS.get(index);
        // Allow for bark nubs, segment rotation and the 1.2-pixel lateral slither on either side.
        return Math.max((segment.width() + (index < 8 ? 1.3F : 0.4F)) / 16 + 0.16F, segment.length() / 16.0F + 0.08F);
    }

    public static float hitHeight(int index) { return index == 0 ? 0.65F : (SEGMENTS.get(index).height() + 0.3F) / 16 + 0.08F; }
    public static float hitForward(int index) { return -(SEGMENTS.get(index).z() - (index == 0 ? 1.5F : 0)) / 16; }
    public static float hitBottom(int index) { return index == 0 ? -0.10F : -0.02F; }

    private RootWormShape() {}
}
