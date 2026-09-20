package com.axial.cosmetics.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import com.axial.cosmetics.mixin.BeaconBeamInvoker;

/** Draws client-only beacon columns for waypoints in the active dimension. */
public final class WaypointRenderer {
    private WaypointRenderer() { }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!WaypointConfig.enabled() || client.player == null || client.world == null) return;
            String dimension = client.world.getRegistryKey().getValue().toString();
            // The player position keeps the marker stable in the normal first-person view.
            // It also avoids retaining camera state outside Fabric's render context.
            double cameraX = client.player.getBlockX();
            double cameraY = client.player.getBlockY();
            double cameraZ = client.player.getBlockZ();
            int ceiling = Math.max(client.player.getBlockY() + 96, 320);
            for (Waypoint waypoint : WaypointConfig.waypoints()) {
                if (!dimension.equals(waypoint.dimension)) continue;
                context.matrices().push();
                context.matrices().translate(waypoint.x + 0.5 - cameraX, waypoint.y - cameraY, waypoint.z + 0.5 - cameraZ);
                BeaconBeamInvoker.axial_cosmetics$renderBeam(context.matrices(), context.commandQueue(), 0.0f, 1.0f, (int) client.world.getTime(), 0, ceiling - waypoint.y);
                // The label sits at the player's current height so it remains readable above terrain.
                context.matrices().translate(0.0, client.player.getBlockY() - waypoint.y + 3.0, 0.0);
                context.matrices().multiply(client.gameRenderer.getCamera().getRotation());
                context.matrices().scale(-0.025f, -0.025f, 0.025f);
                OrderedText label = Text.literal(waypoint.name).asOrderedText();
                int labelWidth = client.textRenderer.getWidth(label);
                int color = 0xFF000000 | (waypoint.color & 0xFFFFFF);
                client.textRenderer.draw(label, -labelWidth / 2.0f, 0.0f, color, true,
                        context.matrices().peek().getPositionMatrix(), context.consumers(),
                        TextRenderer.TextLayerType.SEE_THROUGH, 0x80000000, 0xF000F0);
                context.matrices().pop();
            }
        });
    }
}
