package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public final class WaypointLabelRenderer {
    private WaypointLabelRenderer() { }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.gameRenderer.getCamera() == null || !WaypointConfig.enabled()) return;

        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        Quaternionf rotation = new Quaternionf(camera.getRotation()).conjugate();
        Matrix4f projection = client.gameRenderer.getBasicProjectionMatrix(client.options.getFov().getValue());
        String dimension = client.world.getRegistryKey().getValue().toString();
        int framebufferWidth = client.getWindow().getFramebufferWidth();
        int framebufferHeight = client.getWindow().getFramebufferHeight();
        float xScale = context.getScaledWindowWidth() / (float) framebufferWidth;
        float yScale = context.getScaledWindowHeight() / (float) framebufferHeight;

        for (WaypointConfig.Entry waypoint : WaypointConfig.waypointsFor(client)) {
            if (!dimension.equals(waypoint.dimension())) continue;
            Vector4f projected = new Vector4f(
                    (float) (waypoint.x() + 0.5 - cameraPos.x),
                    // Use the waypoint's real position, rather than the player's
                    // current height, so the tag stays attached to the beam.
                    (float) (waypoint.y() + 1.0 - cameraPos.y),
                    (float) (waypoint.z() + 0.5 - cameraPos.z),
                    1.0f
            ).rotate(rotation).mul(projection);
            if (projected.w <= 0.0f) continue;
            int screenX = Math.round((projected.x / projected.w * 0.5f + 0.5f) * framebufferWidth * xScale);
            int screenY = Math.round((-projected.y / projected.w * 0.5f + 0.5f) * framebufferHeight * yScale);
            double distance = Math.hypot(waypoint.x() + 0.5 - client.player.getX(), waypoint.z() + 0.5 - client.player.getZ());
            String label = waypoint.name() + " [" + formatDistance(distance) + "]";
            int labelWidth = client.textRenderer.getWidth(label) + 10;
            int labelX = screenX - labelWidth / 2;
            int labelY = screenY - 7;

            // Lunar-style markers are a single compact tag anchored to the beam,
            // rather than a floating two-line HUD card.
            context.fill(labelX, labelY, labelX + labelWidth, labelY + 14, 0xAA08080C);
            context.drawStrokedRectangle(labelX, labelY, labelWidth, 14, waypoint.color() | 0xFF000000);
            context.drawTextWithShadow(client.textRenderer, label, labelX + 5, labelY + 3, waypoint.color() | 0xFF000000);
        }
    }

    private static String formatDistance(double distance) {
        if (distance >= 1_000.0) {
            return String.format(java.util.Locale.ROOT, "%.1fkm", distance / 1_000.0);
        }
        return String.format(java.util.Locale.ROOT, "%dm", Math.round(distance));
    }
}
