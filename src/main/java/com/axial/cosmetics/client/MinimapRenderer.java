package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/** Draws a detailed top-down map of every loaded surface block in the viewport. */
public final class MinimapRenderer {
    private static final int MAP_SIZE = 96;
    private static final int FULL_MAP_SIZE = 256;
    private static final int BORDER = 2;
    private static final int REFRESH_INTERVAL_TICKS = 10;
    private static final Identifier MAP_TEXTURE = Identifier.of(AxialCosmetics.MOD_ID, "dynamic/minimap");
    private static final Identifier FULL_MAP_TEXTURE = Identifier.of(AxialCosmetics.MOD_ID, "dynamic/full_map");
    private static final int[] HEIGHTS = new int[MAP_SIZE * MAP_SIZE];

    private static NativeImage image;
    private static NativeImageBackedTexture texture;
    private static NativeImage fullMapImage;
    private static NativeImageBackedTexture fullMapTexture;
    private static int lastCenterX = Integer.MIN_VALUE;
    private static int lastCenterZ = Integer.MIN_VALUE;
    private static int lastSampleTick = Integer.MIN_VALUE;

    private MinimapRenderer() { }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!MinimapConfig.isEnabled() || client.options.hudHidden || client.player == null || client.world == null || client.currentScreen != null) return;
        render(context, client);
    }

    public static void renderPreview(DrawContext context, MinecraftClient client) {
        if (MinimapConfig.isEnabled()) render(context, client);
    }

    public static void renderFullScreen(DrawContext context, MinecraftClient client, float centerX, float centerZ, float blocksPerPixel) {
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        int size = Math.min(width - 48, height - 48);
        int left = (width - size) / 2;
        int top = (height - size) / 2;
        context.fill(0, 0, width, height, 0xD8101217);
        renderExploredMap(context, client, left, top, size, centerX, centerZ, blocksPerPixel);
        context.drawCenteredTextWithShadow(client.textRenderer, net.minecraft.text.Text.literal("FULL MAP  •  SCROLL TO ZOOM  •  DRAG TO PAN  •  PRESS " + AxialCosmetics.minimapMapKey().getBoundKeyLocalizedText().getString().toUpperCase() + " TO CLOSE"), width / 2, Math.max(12, top - 18), 0xFFF7F7FF);
    }

    private static void render(DrawContext context, MinecraftClient client) {
        int size = MinimapConfig.getSize();
        int left = MinimapConfig.getX(client.getWindow().getScaledWidth());
        int top = MinimapConfig.getY(client.getWindow().getScaledHeight());
        renderMap(context, client, left, top, size, MinimapConfig.isCircular());
    }

    private static void renderMap(DrawContext context, MinecraftClient client, int left, int top, int size, boolean circular) {
        if (circular) drawCircleBorder(context, left, top, size);
        else {
            context.fill(left - BORDER, top - BORDER, left + size + BORDER, top + size + BORDER, 0xE8101217);
            context.fill(left, top, left + size, top + size, 0xFF27303A);
        }
        if (client.player != null && client.world != null) {
            ensureTexture(client);
            int centerX = client.player.getBlockX();
            int centerZ = client.player.getBlockZ();
            if (needsRefresh(client.player.age, centerX, centerZ)) sampleTerrain(client, centerX, centerZ);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, MAP_TEXTURE, left, top, 0.0f, 0.0f, size, size, MAP_SIZE, MAP_SIZE, MAP_SIZE, MAP_SIZE);
        } else {
            context.fill(left, top, left + size, top + size, 0xFF386B3D);
        }

        drawWaypointMarkers(context, client, left, top, size, MAP_SIZE, client.player == null ? 0 : client.player.getX(), client.player == null ? 0 : client.player.getZ(), MinimapConfig.zoom(), MinimapConfig.isCircular());
        drawDeathMarker(context, client, left, top, size, MAP_SIZE, client.player == null ? 0 : client.player.getX(), client.player == null ? 0 : client.player.getZ(), MinimapConfig.zoom());
        drawPlayerMarker(context, left, top, size, client.player == null ? 0.0f : client.player.getYaw());
        if (!circular) context.fill(left, top, left + size, top + 1, 0x66FFFFFF);
    }

    private static boolean needsRefresh(int tick, int centerX, int centerZ) {
        return tick < lastSampleTick || centerX != lastCenterX || centerZ != lastCenterZ || tick - lastSampleTick >= REFRESH_INTERVAL_TICKS;
    }

    private static void ensureTexture(MinecraftClient client) {
        if (texture != null) return;
        image = new NativeImage(MAP_SIZE, MAP_SIZE, true);
        texture = new NativeImageBackedTexture(() -> "axial_cosmetics/minimap", image);
        client.getTextureManager().registerTexture(MAP_TEXTURE, texture);
    }

    private static void sampleTerrain(MinecraftClient client, int centerX, int centerZ) {
        lastSampleTick = client.player.age;
        lastCenterX = centerX;
        lastCenterZ = centerZ;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        float blocksPerPixel = MinimapConfig.zoom();
        int originX = (int) Math.floor(centerX - MAP_SIZE * blocksPerPixel / 2.0f);
        int originZ = (int) Math.floor(centerZ - MAP_SIZE * blocksPerPixel / 2.0f);

        for (int mapZ = 0; mapZ < MAP_SIZE; mapZ++) for (int mapX = 0; mapX < MAP_SIZE; mapX++) {
            int worldX = (int) Math.floor(originX + mapX * blocksPerPixel);
            int worldZ = (int) Math.floor(originZ + mapZ * blocksPerPixel);
            HEIGHTS[mapZ * MAP_SIZE + mapX] = client.world.getTopY(Heightmap.Type.WORLD_SURFACE, worldX, worldZ) - 1;
        }

        for (int mapZ = 0; mapZ < MAP_SIZE; mapZ++) for (int mapX = 0; mapX < MAP_SIZE; mapX++) {
            int index = mapZ * MAP_SIZE + mapX;
            int worldX = (int) Math.floor(originX + mapX * blocksPerPixel);
            int worldZ = (int) Math.floor(originZ + mapZ * blocksPerPixel);
            pos.set(worldX, HEIGHTS[index], worldZ);
            BlockState state = client.world.getBlockState(pos);
            int color = colorFor(state, client, pos, index, mapX, mapZ);
            image.setColorArgb(mapX, mapZ, MinimapConfig.isCircular() && outsideCircle(mapX, mapZ) ? 0 : color);
            MinimapExploration.discover(client, worldX, worldZ, color);
        }
        texture.upload();
        MinimapExploration.saveIfDue();
    }

    private static int colorFor(BlockState state, MinecraftClient client, BlockPos pos, int index, int mapX, int mapZ) {
        MapColor mapColor = state.getMapColor(client.world, pos);
        if (mapColor == MapColor.CLEAR) return 0xFF27303A;

        int height = HEIGHTS[index];
        int west = mapX == 0 ? height : HEIGHTS[index - 1];
        int north = mapZ == 0 ? height : HEIGHTS[index - MAP_SIZE];
        int slope = height - (west + north) / 2;
        MapColor.Brightness brightness = slope > 1 ? MapColor.Brightness.HIGH
                : slope < -1 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
        return mapColor.getRenderColor(brightness);
    }

    private static void drawPlayerMarker(DrawContext context, int left, int top, int size, float yaw) {
        int center = size / 2;
        double radians = Math.toRadians(yaw);
        int tipX = left + center + (int) Math.round(-Math.sin(radians) * 7);
        int tipY = top + center + (int) Math.round(Math.cos(radians) * 7);
        int backX = left + center + (int) Math.round(Math.sin(radians) * 4);
        int backY = top + center - (int) Math.round(Math.cos(radians) * 4);
        drawLine(context, backX - 3, backY, tipX, tipY, 0xFFFFFFFF);
        drawLine(context, backX + 3, backY, tipX, tipY, 0xFFFFFFFF);
        context.fill(left + center - 2, top + center - 2, left + center + 3, top + center + 3, 0xFF20283A);
    }

    private static void renderExploredMap(DrawContext context, MinecraftClient client, int left, int top, int size, float centerX, float centerZ, float blocksPerPixel) {
        context.fill(left - BORDER, top - BORDER, left + size + BORDER, top + size + BORDER, 0xE8101217);
        ensureFullMapTexture(client);
        float startX = centerX - FULL_MAP_SIZE * blocksPerPixel / 2.0f;
        float startZ = centerZ - FULL_MAP_SIZE * blocksPerPixel / 2.0f;
        for (int z = 0; z < FULL_MAP_SIZE; z++) for (int x = 0; x < FULL_MAP_SIZE; x++) {
            fullMapImage.setColorArgb(x, z, MinimapExploration.colorAt(client, (int) Math.floor(startX + x * blocksPerPixel), (int) Math.floor(startZ + z * blocksPerPixel)));
        }
        fullMapTexture.upload();
        context.drawTexture(RenderPipelines.GUI_TEXTURED, FULL_MAP_TEXTURE, left, top, 0.0f, 0.0f, size, size, FULL_MAP_SIZE, FULL_MAP_SIZE, FULL_MAP_SIZE, FULL_MAP_SIZE);
        drawWaypointMarkers(context, client, left, top, size, FULL_MAP_SIZE, centerX, centerZ, blocksPerPixel, false);
        drawDeathMarker(context, client, left, top, size, FULL_MAP_SIZE, centerX, centerZ, blocksPerPixel);
        if (client.player != null) {
            float px = left + size / 2.0f + (float) ((client.player.getX() - centerX) / blocksPerPixel * size / FULL_MAP_SIZE);
            float pz = top + size / 2.0f + (float) ((client.player.getZ() - centerZ) / blocksPerPixel * size / FULL_MAP_SIZE);
            drawPlayerMarker(context, Math.round(px) - size / 2, Math.round(pz) - size / 2, size, client.player.getYaw());
        }
    }

    private static void ensureFullMapTexture(MinecraftClient client) {
        if (fullMapTexture != null) return;
        fullMapImage = new NativeImage(FULL_MAP_SIZE, FULL_MAP_SIZE, true);
        fullMapTexture = new NativeImageBackedTexture(() -> "axial_cosmetics/full_map", fullMapImage);
        client.getTextureManager().registerTexture(FULL_MAP_TEXTURE, fullMapTexture);
    }

    private static void drawWaypointMarkers(DrawContext context, MinecraftClient client, int left, int top, int size, int mapPixels, double centerX, double centerZ, float blocksPerPixel, boolean circular) {
        if (!WaypointConfig.enabled()) return;
        float pixelsPerBlock = size / (mapPixels * blocksPerPixel);
        String dimension = client.world == null ? "" : client.world.getRegistryKey().getValue().toString();
        for (WaypointConfig.Entry waypoint : WaypointConfig.waypointsFor(client)) {
            if (!dimension.equals(waypoint.dimension())) continue;
            float x = left + size / 2.0f + (float) ((waypoint.x() - centerX) * pixelsPerBlock);
            float z = top + size / 2.0f + (float) ((waypoint.z() - centerZ) * pixelsPerBlock);
            if (x < left || x >= left + size || z < top || z >= top + size || (circular && Math.hypot(x - left - size / 2.0, z - top - size / 2.0) > size / 2.0)) continue;
            int markerX = Math.round(x), markerY = Math.round(z);
            context.fill(markerX - 3, markerY, markerX + 4, markerY + 1, waypoint.color() | 0xFF000000);
            context.fill(markerX, markerY - 3, markerX + 1, markerY + 4, waypoint.color() | 0xFF000000);
            context.drawTextWithShadow(client.textRenderer, net.minecraft.text.Text.literal(waypoint.name()), markerX + 5, markerY - 4, 0xFFFFFFFF);
        }
    }

    private static void drawDeathMarker(DrawContext context, MinecraftClient client, int left, int top, int size, int mapPixels, double centerX, double centerZ, float blocksPerPixel) {
        MinimapExploration.Death death = MinimapExploration.deathFor(client);
        if (death == null) return;
        float pixelsPerBlock = size / (mapPixels * blocksPerPixel);
        int x = Math.round(left + size / 2.0f + (float) ((death.x() - centerX) * pixelsPerBlock));
        int z = Math.round(top + size / 2.0f + (float) ((death.z() - centerZ) * pixelsPerBlock));
        if (x < left || x >= left + size || z < top || z >= top + size) return;
        drawLine(context, x - 4, z - 4, x + 4, z + 4, 0xFFFF3B4E);
        drawLine(context, x + 4, z - 4, x - 4, z + 4, 0xFFFF3B4E);
    }

    private static void drawLine(DrawContext context, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1, dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1, error = dx + dy;
        while (true) {
            context.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) return;
            int twiceError = 2 * error;
            if (twiceError >= dy) { error += dy; x0 += sx; }
            if (twiceError <= dx) { error += dx; y0 += sy; }
        }
    }

    private static boolean outsideCircle(int x, int y) {
        float radius = MAP_SIZE / 2.0f - 1.0f;
        float dx = x + 0.5f - MAP_SIZE / 2.0f;
        float dy = y + 0.5f - MAP_SIZE / 2.0f;
        return dx * dx + dy * dy > radius * radius;
    }

    private static void drawCircleBorder(DrawContext context, int left, int top, int size) {
        float radius = size / 2.0f;
        float center = radius;
        for (int y = 0; y < size; y++) {
            float distanceY = y + 0.5f - center;
            int edge = (int) Math.sqrt(Math.max(0, radius * radius - distanceY * distanceY));
            int start = (int) (center - edge);
            int end = (int) (center + edge);
            context.fill(left + start, top + y, left + Math.min(start + BORDER, end), top + y + 1, 0xE8101217);
            context.fill(left + Math.max(start, end - BORDER), top + y, left + end, top + y + 1, 0xE8101217);
        }
    }
}
