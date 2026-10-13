package com.axial.cosmetics.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Receives compact, server-authoritative world overviews from PrisonsCore. */
public final class MinimapServerOverview {
    private static final byte REQUEST = 0;
    private static final byte SNAPSHOT = 1;
    private static final byte OVERLAYS = 2;
    private static final Identifier TEXTURE_ID = Identifier.of("axial_cosmetics", "dynamic/server_minimap_overview");
    private static final Map<String, Overview> OVERVIEWS = new HashMap<>();
    private static final Map<String, Overlay> OVERLAYS_BY_WORLD = new HashMap<>();
    private static String activeWorld;
    private static NativeImage image;
    private static NativeImageBackedTexture texture;
    private static boolean textureDirty;
    private static long lastRequestAt;
    private static long waitingForOverviewSince = -1L;

    private MinimapServerOverview() { }

    public static void request() {
        long now = System.currentTimeMillis();
        if (now - lastRequestAt < 5_000) return;
        lastRequestAt = now;
        if (activeWorld == null && waitingForOverviewSince < 0) waitingForOverviewSince = now;
        ClientPlayNetworking.send(new MinimapMapPayload(new byte[]{REQUEST}));
    }

    /** Clears the previous world's map while the server sends the next world's overview. */
    public static void beginWorldChange() {
        activeWorld = null;
        lastRequestAt = 0L;
        waitingForOverviewSince = System.currentTimeMillis();
    }

    /** Unmodded servers keep the existing nearby-terrain minimap after a short handshake window. */
    public static boolean shouldFallBackToLocalTerrain() {
        return activeWorld == null && waitingForOverviewSince > 0 && System.currentTimeMillis() - waitingForOverviewSince >= 1_500L;
    }

    public static void receive(byte[] payload) {
        if (payload.length == 0) return;
        if (payload[0] == OVERLAYS) {
            receiveOverlays(payload);
            return;
        }
        if (payload[0] != SNAPSHOT) return;
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            String world = readString(input);
            int minX = input.readInt();
            int minZ = input.readInt();
            int blockStep = input.readInt();
            int width = input.readUnsignedShort();
            int height = input.readUnsignedShort();
            int rowStart = input.readUnsignedShort();
            int rowCount = input.readUnsignedShort();
            if (width <= 0 || height <= 0 || width > 512 || height > 512 || rowStart + rowCount > height) return;

            Overview overview = OVERVIEWS.compute(world, (ignored, current) -> current != null && current.matches(minX, minZ, blockStep, width, height)
                    ? current : new Overview(minX, minZ, blockStep, width, height));
            for (int row = 0; row < rowCount; row++) {
                int offset = (rowStart + row) * width;
                for (int column = 0; column < width; column++) overview.colors[offset + column] = input.readInt();
            }
            activeWorld = world;
            waitingForOverviewSince = -1L;
            textureDirty = true;
        } catch (IOException ignored) { }
    }

    private static void receiveOverlays(byte[] payload) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            String world = readString(input);
            int zoneCount = input.readUnsignedShort();
            if (zoneCount > 256) return;
            Zone[] zones = new Zone[zoneCount];
            for (int index = 0; index < zoneCount; index++) {
                zones[index] = new Zone(readString(input), input.readInt(), input.readInt(), input.readInt(), input.readInt(), input.readInt());
            }
            int poiCount = input.readUnsignedShort();
            if (poiCount > 256) return;
            Poi[] pois = new Poi[poiCount];
            for (int index = 0; index < poiCount; index++) {
                pois[index] = new Poi(readString(input), input.readInt(), input.readInt(), input.readInt());
            }
            OVERLAYS_BY_WORLD.put(world, new Overlay(List.of(zones), List.of(pois)));
        } catch (IOException ignored) { }
    }

    public static boolean render(DrawContext context, MinecraftClient client, int left, int top, int size) {
        Overview overview = activeWorld == null ? null : OVERVIEWS.get(activeWorld);
        if (overview == null) return false;
        ensureTexture(client, overview);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, left, top, 0.0f, 0.0f, size, size,
                overview.width, overview.height, overview.width, overview.height);
        drawOverlays(context, overview, left, top, size);
        if (client.player != null) {
            int x = Math.round(left + (float) (client.player.getX() - overview.minX) / (overview.width * overview.blockStep) * size);
            int z = Math.round(top + (float) (client.player.getZ() - overview.minZ) / (overview.height * overview.blockStep) * size);
            if (x >= left && x < left + size && z >= top && z < top + size) {
                context.fill(x - 3, z, x + 4, z + 1, 0xFFFFFFFF);
                context.fill(x, z - 3, x + 1, z + 4, 0xFFFFFFFF);
                context.fill(x, z, x + 1, z + 1, 0xFF2878FF);
            }
        }
        return true;
    }

    /** Server zones and POIs drawn over the terrain map in the HUD. */
    public static void renderHudOverlays(DrawContext context, int left, int top, int size, int mapPixels,
                                         double centerX, double centerZ, float blocksPerPixel, boolean circular) {
        Overlay overlay = activeWorld == null ? null : OVERLAYS_BY_WORLD.get(activeWorld);
        if (overlay == null) return;
        float pixelsPerBlock = size / (mapPixels * blocksPerPixel);
        for (Zone zone : overlay.zones) {
            float x = left + size / 2.0f + (float) (((zone.minX + zone.maxX) / 2.0 - centerX) * pixelsPerBlock);
            float z = top + size / 2.0f + (float) (((zone.minZ + zone.maxZ) / 2.0 - centerZ) * pixelsPerBlock);
            if (!inside(left, top, size, x, z, circular)) continue;
            drawPlus(context, Math.round(x), Math.round(z), zone.color | 0xFF000000);
        }
        for (Poi poi : overlay.pois) {
            float x = left + size / 2.0f + (float) ((poi.x - centerX) * pixelsPerBlock);
            float z = top + size / 2.0f + (float) ((poi.z - centerZ) * pixelsPerBlock);
            if (!inside(left, top, size, x, z, circular)) continue;
            drawPoi(context, Math.round(x), Math.round(z), poi.color);
        }
    }

    private static void drawOverlays(DrawContext context, Overview overview, int left, int top, int size) {
        Overlay overlay = activeWorld == null ? null : OVERLAYS_BY_WORLD.get(activeWorld);
        if (overlay == null) return;
        for (Zone zone : overlay.zones) {
            int x1 = projectX(overview, left, size, zone.minX);
            int z1 = projectZ(overview, top, size, zone.minZ);
            int x2 = projectX(overview, left, size, zone.maxX + 1);
            int z2 = projectZ(overview, top, size, zone.maxZ + 1);
            int fill = (zone.color & 0x00FFFFFF) | 0x44000000;
            context.fill(x1, z1, x2, z2, fill);
            int border = zone.color | 0xFF000000;
            context.fill(x1, z1, x2, z1 + 1, border);
            context.fill(x1, z2 - 1, x2, z2, border);
            context.fill(x1, z1, x1 + 1, z2, border);
            context.fill(x2 - 1, z1, x2, z2, border);
        }
        for (Poi poi : overlay.pois) drawPoi(context, projectX(overview, left, size, poi.x), projectZ(overview, top, size, poi.z), poi.color);
    }

    private static int projectX(Overview overview, int left, int size, int worldX) {
        return Math.round(left + (float) (worldX - overview.minX) / (overview.width * overview.blockStep) * size);
    }

    private static int projectZ(Overview overview, int top, int size, int worldZ) {
        return Math.round(top + (float) (worldZ - overview.minZ) / (overview.height * overview.blockStep) * size);
    }

    private static boolean inside(int left, int top, int size, float x, float z, boolean circular) {
        return x >= left && x < left + size && z >= top && z < top + size
                && (!circular || Math.hypot(x - left - size / 2.0, z - top - size / 2.0) <= size / 2.0);
    }

    private static void drawPoi(DrawContext context, int x, int z, int color) {
        int opaque = color | 0xFF000000;
        context.fill(x - 2, z - 2, x + 3, z + 3, 0xFF10141C);
        context.fill(x - 1, z - 1, x + 2, z + 2, opaque);
    }

    private static void drawPlus(DrawContext context, int x, int z, int color) {
        context.fill(x - 3, z, x + 4, z + 1, color);
        context.fill(x, z - 3, x + 1, z + 4, color);
    }

    private static void ensureTexture(MinecraftClient client, Overview overview) {
        if (texture == null || image == null || image.getWidth() != overview.width || image.getHeight() != overview.height) {
            if (image != null) image.close();
            image = new NativeImage(overview.width, overview.height, true);
            texture = new NativeImageBackedTexture(() -> "axial_cosmetics/server_minimap_overview", image);
            client.getTextureManager().registerTexture(TEXTURE_ID, texture);
            textureDirty = true;
        }
        if (!textureDirty) return;
        for (int y = 0; y < overview.height; y++) for (int x = 0; x < overview.width; x++) {
            image.setColorArgb(x, y, overview.colors[y * overview.width + x]);
        }
        texture.upload();
        textureDirty = false;
    }

    private static String readString(DataInputStream input) throws IOException {
        int length = input.readUnsignedShort();
        if (length > 255) throw new IOException("World name is too long");
        return new String(input.readNBytes(length), StandardCharsets.UTF_8);
    }

    private static final class Overview {
        private final int minX, minZ, blockStep, width, height;
        private final int[] colors;

        private Overview(int minX, int minZ, int blockStep, int width, int height) {
            this.minX = minX;
            this.minZ = minZ;
            this.blockStep = blockStep;
            this.width = width;
            this.height = height;
            this.colors = new int[width * height];
        }

        private boolean matches(int minX, int minZ, int blockStep, int width, int height) {
            return this.minX == minX && this.minZ == minZ && this.blockStep == blockStep && this.width == width && this.height == height;
        }
    }

    private record Overlay(List<Zone> zones, List<Poi> pois) { }
    private record Zone(String name, int minX, int minZ, int maxX, int maxZ, int color) { }
    private record Poi(String name, int x, int z, int color) { }
}
