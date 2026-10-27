package com.axial.cosmetics.client;

import java.util.List;

/** Soft alignment magnets, never collision avoidance: elements may freely overlap. */
public final class HudSnap {
    public static final int DISTANCE = 6, GAP = 4;
    public record Box(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean contains(double px, double py) { return px >= x && px < right() && py >= y && py < bottom(); }
    }
    public record Result(int x, int y, Integer guideX, Integer guideY) {}
    private HudSnap() {}

    public static Result place(Box desired, List<Box> others, int screenWidth, int screenHeight, boolean free) {
        int maxX = Math.max(0, screenWidth - desired.width()), maxY = Math.max(0, screenHeight - desired.height());
        int x = Math.clamp(desired.x(), 0, maxX), y = Math.clamp(desired.y(), 0, maxY);
        if (free) return new Result(x, y, null, null);
        var horizontal = new Axis(x, maxX); var vertical = new Axis(y, maxY);
        horizontal.offer(0, 0); horizontal.offer(maxX, screenWidth);
        horizontal.offer(maxX / 2, screenWidth / 2);
        vertical.offer(0, 0); vertical.offer(maxY, screenHeight);
        vertical.offer(maxY / 2, screenHeight / 2);
        for (Box other : others) {
            if (other.width() <= 0 || other.height() <= 0) continue;
            if (near(y, y + desired.height(), other.y(), other.bottom())) {
                horizontal.offer(other.right() + GAP, other.right() + GAP);
                horizontal.offer(other.x() - GAP - desired.width(), other.x() - GAP);
                horizontal.offer(other.x(), other.x());
                horizontal.offer(other.right() - desired.width(), other.right());
                horizontal.offer(other.x() + (other.width() - desired.width()) / 2, other.x() + other.width() / 2);
            }
            if (near(x, x + desired.width(), other.x(), other.right())) {
                vertical.offer(other.bottom() + GAP, other.bottom() + GAP);
                vertical.offer(other.y() - GAP - desired.height(), other.y() - GAP);
                vertical.offer(other.y(), other.y());
                vertical.offer(other.bottom() - desired.height(), other.bottom());
                vertical.offer(other.y() + (other.height() - desired.height()) / 2, other.y() + other.height() / 2);
            }
        }
        return new Result(horizontal.position, vertical.position, horizontal.guide, vertical.guide);
    }
    private static boolean near(int start, int end, int otherStart, int otherEnd) {
        return start <= otherEnd + GAP + DISTANCE && end >= otherStart - GAP - DISTANCE;
    }
    private static final class Axis {
        final int desired, max;
        int position, distance = DISTANCE + 1;
        Integer guide;
        Axis(int desired, int max) { this.desired = desired; this.max = max; position = desired; }
        void offer(int candidate, int line) {
            if (candidate < 0 || candidate > max) return;
            int delta = Math.abs(candidate - desired);
            if (delta < distance) { distance = delta; position = candidate; guide = line; }
        }
    }
}
