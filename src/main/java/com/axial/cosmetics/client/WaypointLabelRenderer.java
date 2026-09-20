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

        for (WaypointConfig.Entry waypoint : WaypointConfig.waypoints()) {
            if (!dimension.equals(waypoint.dimension())) continue;
            Vector4f projected = new Vector4f(
                    (float) (waypoint.x() + 0.5 - cameraPos.x),
                    (float) (client.player.getY() - cameraPos.y),
                    (float) (waypoint.z() + 0.5 - cameraPos.z),
                    1.0f
            ).rotate(rotation).mul(projection);
            if (projected.w <= 0.0f) continue;
            int screenX = Math.round((projected.x / projected.w * 0.5f + 0.5f) * framebufferWidth * xScale);
            int screenY = Math.round((-projected.y / projected.w * 0.5f + 0.5f) * framebufferHeight * yScale);
            String label = waypoint.name();
            int labelWidth = client.textRenderer.getWidth(label);
            context.fill(screenX - labelWidth / 2 - 4, screenY - 3, screenX + labelWidth / 2 + 4, screenY + 11, 0xA0000000);
            context.drawTextWithShadow(client.textRenderer, label, screenX - labelWidth / 2, screenY, waypoint.color() | 0xFF000000);
        }
    }
}
