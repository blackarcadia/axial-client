package com.axial.cosmetics.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

/** Renders the persistent vertical marker used for player-created waypoints. */
public final class WaypointWorldRenderer {
    private static final double BEAM_HALF_WIDTH = 0.35;
    private static final float BEAM_OUTLINE_WIDTH = 1.0f;
    private static final int BEAM_ALPHA = 0x66;

    private WaypointWorldRenderer() { }

    public static void register() {
        // Gizmos are collected after entities. Registering earlier can leave the
        // marker behind terrain or omit it entirely, depending on the render path.
        WorldRenderEvents.AFTER_ENTITIES.register(WaypointWorldRenderer::render);
    }

    private static void render(net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || !WaypointConfig.enabled()) return;
        String dimension = client.world.getRegistryKey().getValue().toString();
        for (WaypointConfig.Entry waypoint : WaypointConfig.waypointsFor(client)) {
            if (!dimension.equals(waypoint.dimension())) continue;
            BlockPos position = new BlockPos(waypoint.x(), waypoint.y(), waypoint.z());
            drawBeam(client.world, position, waypoint.color());
            drawLabel(context, position, waypoint.name(), waypoint.color());
        }
    }

    private static void drawBeam(ClientWorld world, BlockPos position, int color) {
        int rgb = color & 0x00FFFFFF;
        DrawStyle style = DrawStyle.filledAndStroked(0x00FFFFFF, BEAM_OUTLINE_WIDTH, (BEAM_ALPHA << 24) | rgb);
        Vec3d min = new Vec3d(position.getX() + 0.5 - BEAM_HALF_WIDTH, world.getBottomY(), position.getZ() + 0.5 - BEAM_HALF_WIDTH);
        Vec3d max = new Vec3d(position.getX() + 0.5 + BEAM_HALF_WIDTH, world.getTopYInclusive() + 1.0, position.getZ() + 0.5 + BEAM_HALF_WIDTH);

        for (Direction direction : Direction.values()) {
            GizmoDrawing.face(min, max, direction, style);
        }
    }

    private static void drawLabel(net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext context, BlockPos position, String name, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d cameraPos = client.gameRenderer.getCamera().getCameraPos();
        MatrixStack matrices = context.matrices();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        matrices.translate(position.getX() + 0.5, position.getY() + 1.0, position.getZ() + 0.5);
        matrices.multiply(client.gameRenderer.getCamera().getRotation());
        matrices.scale(-0.025f, -0.025f, 0.025f);

        Text label = Text.literal(name);
        context.commandQueue().submitText(matrices, -client.textRenderer.getWidth(label) / 2.0f, -4.0f,
                label.asOrderedText(), false, TextRenderer.TextLayerType.SEE_THROUGH, 0xF000F0,
                color | 0xFF000000, 0x66000000, 0xFF000000);
        matrices.pop();
    }

}
