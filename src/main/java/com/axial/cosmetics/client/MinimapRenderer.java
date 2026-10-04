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
    private static final int BORDER = 2;
    private static final int REFRESH_INTERVAL_TICKS = 10;
    private static final Identifier MAP_TEXTURE = Identifier.of(AxialCosmetics.MOD_ID, "dynamic/minimap");
    private static final int[] HEIGHTS = new int[MAP_SIZE * MAP_SIZE];

    private static NativeImage image;
    private static NativeImageBackedTexture texture;
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

    public static void renderFullScreen(DrawContext context, MinecraftClient client) {
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        int size = Math.min(width - 48, height - 48);
        int left = (width - size) / 2;
        int top = (height - size) / 2;
        context.fill(0, 0, width, height, 0xD8101217);
        renderMap(context, client, left, top, size, false);
        context.drawCenteredTextWithShadow(client.textRenderer, net.minecraft.text.Text.literal("FULL MAP  •  PRESS " + AxialCosmetics.minimapMapKey().getBoundKeyLocalizedText().getString().toUpperCase() + " TO CLOSE"), width / 2, Math.max(12, top - 18), 0xFFF7F7FF);
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

        drawPlayerMarker(context, left, top, size);
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
        int originX = centerX - MAP_SIZE / 2;
        int originZ = centerZ - MAP_SIZE / 2;

        for (int mapZ = 0; mapZ < MAP_SIZE; mapZ++) for (int mapX = 0; mapX < MAP_SIZE; mapX++) {
            int worldX = originX + mapX;
            int worldZ = originZ + mapZ;
            HEIGHTS[mapZ * MAP_SIZE + mapX] = client.world.getTopY(Heightmap.Type.WORLD_SURFACE, worldX, worldZ) - 1;
        }

        for (int mapZ = 0; mapZ < MAP_SIZE; mapZ++) for (int mapX = 0; mapX < MAP_SIZE; mapX++) {
            int index = mapZ * MAP_SIZE + mapX;
            pos.set(originX + mapX, HEIGHTS[index], originZ + mapZ);
            BlockState state = client.world.getBlockState(pos);
            image.setColorArgb(mapX, mapZ, MinimapConfig.isCircular() && outsideCircle(mapX, mapZ) ? 0 : colorFor(state, client, pos, index, mapX, mapZ));
        }
        texture.upload();
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

    private static void drawPlayerMarker(DrawContext context, int left, int top, int size) {
        int center = size / 2;
        context.fill(left + center - 2, top + center - 2, left + center + 3, top + center + 3, 0xFFFFFFFF);
        context.fill(left + center - 1, top + center - 1, left + center + 2, top + center + 2, 0xFF191C22);
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
