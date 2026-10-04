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
    public static final int SIZE = 120;
    private static final int BORDER = 2;
    private static final int REFRESH_INTERVAL_TICKS = 10;
    private static final Identifier MAP_TEXTURE = Identifier.of(AxialCosmetics.MOD_ID, "dynamic/minimap");
    private static final int[] HEIGHTS = new int[SIZE * SIZE];

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

    private static void render(DrawContext context, MinecraftClient client) {
        int left = MinimapConfig.getX(client.getWindow().getScaledWidth());
        int top = MinimapConfig.getY(client.getWindow().getScaledHeight());
        context.fill(left - BORDER, top - BORDER, left + SIZE + BORDER, top + SIZE + BORDER, 0xE8101217);
        context.fill(left, top, left + SIZE, top + SIZE, 0xFF27303A);

        if (client.player != null && client.world != null) {
            ensureTexture(client);
            int centerX = client.player.getBlockX();
            int centerZ = client.player.getBlockZ();
            if (needsRefresh(client.player.age, centerX, centerZ)) sampleTerrain(client, centerX, centerZ);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, MAP_TEXTURE, left, top, 0.0f, 0.0f, SIZE, SIZE, SIZE, SIZE, SIZE, SIZE);
        } else {
            context.fill(left, top, left + SIZE, top + SIZE, 0xFF386B3D);
        }

        drawPlayerMarker(context, left, top);
        context.fill(left, top, left + SIZE, top + 1, 0x66FFFFFF);
    }

    private static boolean needsRefresh(int tick, int centerX, int centerZ) {
        return tick < lastSampleTick || centerX != lastCenterX || centerZ != lastCenterZ || tick - lastSampleTick >= REFRESH_INTERVAL_TICKS;
    }

    private static void ensureTexture(MinecraftClient client) {
        if (texture != null) return;
        image = new NativeImage(SIZE, SIZE, true);
        texture = new NativeImageBackedTexture(() -> "axial_cosmetics/minimap", image);
        client.getTextureManager().registerTexture(MAP_TEXTURE, texture);
    }

    private static void sampleTerrain(MinecraftClient client, int centerX, int centerZ) {
        lastSampleTick = client.player.age;
        lastCenterX = centerX;
        lastCenterZ = centerZ;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int originX = centerX - SIZE / 2;
        int originZ = centerZ - SIZE / 2;

        for (int mapZ = 0; mapZ < SIZE; mapZ++) for (int mapX = 0; mapX < SIZE; mapX++) {
            int worldX = originX + mapX;
            int worldZ = originZ + mapZ;
            HEIGHTS[mapZ * SIZE + mapX] = client.world.getTopY(Heightmap.Type.WORLD_SURFACE, worldX, worldZ) - 1;
        }

        for (int mapZ = 0; mapZ < SIZE; mapZ++) for (int mapX = 0; mapX < SIZE; mapX++) {
            int index = mapZ * SIZE + mapX;
            pos.set(originX + mapX, HEIGHTS[index], originZ + mapZ);
            BlockState state = client.world.getBlockState(pos);
            image.setColorArgb(mapX, mapZ, colorFor(state, client, pos, index, mapX, mapZ));
        }
        texture.upload();
    }

    private static int colorFor(BlockState state, MinecraftClient client, BlockPos pos, int index, int mapX, int mapZ) {
        MapColor mapColor = state.getMapColor(client.world, pos);
        if (mapColor == MapColor.CLEAR) return 0xFF27303A;

        int height = HEIGHTS[index];
        int west = mapX == 0 ? height : HEIGHTS[index - 1];
        int north = mapZ == 0 ? height : HEIGHTS[index - SIZE];
        int slope = height - (west + north) / 2;
        MapColor.Brightness brightness = slope > 1 ? MapColor.Brightness.HIGH
                : slope < -1 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
        return mapColor.getRenderColor(brightness);
    }

    private static void drawPlayerMarker(DrawContext context, int left, int top) {
        int center = SIZE / 2;
        context.fill(left + center - 2, top + center - 2, left + center + 3, top + center + 3, 0xFFFFFFFF);
        context.fill(left + center - 1, top + center - 1, left + center + 2, top + center + 2, 0xFF191C22);
    }
}
