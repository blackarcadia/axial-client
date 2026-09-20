package com.axial.cosmetics.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Method;

/** Draws custom waypoint colors through AxialUtils' existing beam renderer. */
public final class WaypointWorldRenderer {
    private static Method renderBeam;

    private WaypointWorldRenderer() { }

    public static void register() {
        WorldRenderEvents.BEFORE_ENTITIES.register(context -> render());
    }

    private static void render() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || !WaypointConfig.enabled()) return;
        String dimension = client.world.getRegistryKey().getValue().toString();
        for (WaypointConfig.Entry waypoint : WaypointConfig.waypointsFor(client)) {
            if (!dimension.equals(waypoint.dimension())) continue;
            drawBeam(client.world, new BlockPos(waypoint.x(), waypoint.y(), waypoint.z()), waypoint.color());
        }
    }

    private static void drawBeam(ClientWorld world, BlockPos position, int color) {
        try {
            if (renderBeam == null) {
                Class<?> renderer = Class.forName("org.axial.axialutils.client.WaypointOverlayRenderer");
                renderBeam = renderer.getDeclaredMethod("renderBeam", net.minecraft.world.World.class, BlockPos.class, int.class, boolean.class);
                renderBeam.setAccessible(true);
            }
            renderBeam.invoke(null, world, position, color, false);
        } catch (ReflectiveOperationException ignored) {
            // Keep waypoint creation available if AxialUtils changes its renderer internals.
        }
    }
}
