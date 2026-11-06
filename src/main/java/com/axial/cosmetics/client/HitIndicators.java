package com.axial.cosmetics.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.TextGizmo;
import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;

/** Server-confirmed local hits only; no guessed damage values or indicators for missed swings. */
public final class HitIndicators {
    private record Hit(Vec3d origin, double started, double velocityX, double velocityZ) {}
    private static final ArrayDeque<Hit> HITS = new ArrayDeque<>();
    private static ClientWorld world;
    private HitIndicators() {}
    public static void register() {
        HitIndicatorsConfig.load();
        WorldRenderEvents.AFTER_ENTITIES.register(context -> render());
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, next) -> clear());
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }
    public static void clear() { HITS.clear(); world = null; }
    private static double now(MinecraftClient client) {
        return (client.world.getTime() + client.getRenderTickCounter().getTickProgress(false)) / 20.0;
    }
    public static void onDamage(EntityDamageS2CPacket packet) {
        var client = MinecraftClient.getInstance();
        if (!HitIndicatorsConfig.enabled() || client.world == null || client.player == null) return;
        if (packet.createDamageSource(client.world).getAttacker() != client.player) return;
        if (!(client.world.getEntityById(packet.entityId()) instanceof LivingEntity target)
                || target == client.player || target.isInvisible() || target.squaredDistanceTo(client.player) > 64 * 64) return;
        if (world != client.world) { clear(); world = client.world; }
        var random = ThreadLocalRandom.current();
        // Place the label on the camera-facing side so it doesn't start hidden inside the target.
        Vec3d towardCamera = client.gameRenderer.getCamera().getCameraPos().subtract(target.getEntityPos());
        double angle = Math.atan2(towardCamera.z, towardCamera.x) + random.nextDouble(-.85, .85);
        double radius = target.getWidth() * .55 + .06;
        Vec3d origin = target.getEntityPos().add(Math.cos(angle) * radius,
                target.getHeight() * random.nextDouble(.25, .85), Math.sin(angle) * radius);
        while (HITS.size() >= 64) HITS.removeFirst();
        HITS.addLast(new Hit(origin, now(client), random.nextDouble(-.45, .45), random.nextDouble(-.45, .45)));
    }
    private static void render() {
        var client = MinecraftClient.getInstance();
        if (!HitIndicatorsConfig.enabled() || client.world == null || client.world != world) { clear(); return; }
        double time = now(client);
        HITS.removeIf(hit -> time - hit.started >= HitIndicatorAnimation.DURATION || time < hit.started);
        if (client.options.hudHidden || client.player == null) return;
        for (Hit hit : HITS) {
            if (hit.origin.squaredDistanceTo(client.gameRenderer.getCamera().getCameraPos()) > 64 * 64) continue;
            var frame = HitIndicatorAnimation.frame(time - hit.started, hit.velocityX, hit.velocityZ);
            if ((frame.color() >>> 24) < 4) continue;
            GizmoDrawing.text("HIT", hit.origin.add(frame.x(), frame.y(), frame.z()),
                    TextGizmo.Style.centered(frame.color()).scaled(frame.scale()));
        }
    }
}
