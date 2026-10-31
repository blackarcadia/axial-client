package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

/** Finite border walls: adjacent faces meet at corners without crossing them. */
public final class WorldBorderOverlayGeometry {
    private static final DrawStyle STYLE = DrawStyle.filledAndStroked(0xFFFF4040, 1.5f, 0xCCFF0000);

    private WorldBorderOverlayGeometry() { }

    public static void render(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        double radius = Math.max(16, client.options.getViewDistance().getValue() * 16);
        WorldBorder border = client.world.getWorldBorder();
        double west = border.getBoundWest(), east = border.getBoundEast();
        double north = border.getBoundNorth(), south = border.getBoundSouth();
        double minX = Math.max(west, camera.x - radius), maxX = Math.min(east, camera.x + radius);
        double minZ = Math.max(north, camera.z - radius), maxZ = Math.min(south, camera.z + radius);
        double bottom = client.world.getBottomY(), top = client.world.getTopYInclusive() + 1.0;
        if (minZ < maxZ) {
            if (Math.abs(camera.x - west) < radius) wallX(west, bottom, top, minZ, maxZ, camera.x);
            if (Math.abs(camera.x - east) < radius) wallX(east, bottom, top, minZ, maxZ, camera.x);
        }
        if (minX < maxX) {
            if (Math.abs(camera.z - north) < radius) wallZ(north, bottom, top, minX, maxX, camera.z);
            if (Math.abs(camera.z - south) < radius) wallZ(south, bottom, top, minX, maxX, camera.z);
        }
    }

    private static void wallX(double x, double bottom, double top, double minZ, double maxZ, double cameraX) {
        // One camera-facing surface avoids blending the same translucent wall twice.
        GizmoDrawing.face(new Vec3d(x, bottom, minZ), new Vec3d(x, top, maxZ),
                cameraX < x ? Direction.WEST : Direction.EAST, STYLE);
    }

    private static void wallZ(double z, double bottom, double top, double minX, double maxX, double cameraZ) {
        GizmoDrawing.face(new Vec3d(minX, bottom, z), new Vec3d(maxX, top, z),
                cameraZ < z ? Direction.NORTH : Direction.SOUTH, STYLE);
    }
}
