package com.axial.cosmetics.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/** Draws a client-side map of the terrain in chunks currently available to the player. */
public final class MinimapRenderer {
    public static final int SIZE = 120;
    private static final int BORDER = 2;
    private static final int SAMPLE_SIZE = 3;
    private static final int SAMPLES = SIZE / SAMPLE_SIZE;
    private static final int[] TERRAIN = new int[SAMPLES * SAMPLES];
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
            if (lastSampleTick == Integer.MIN_VALUE || client.player.age < lastSampleTick || client.player.age - lastSampleTick >= 10) sampleTerrain(client);
            for (int sampleZ = 0; sampleZ < SAMPLES; sampleZ++) for (int sampleX = 0; sampleX < SAMPLES; sampleX++)
                context.fill(left + sampleX * SAMPLE_SIZE, top + sampleZ * SAMPLE_SIZE,
                        left + (sampleX + 1) * SAMPLE_SIZE, top + (sampleZ + 1) * SAMPLE_SIZE,
                        TERRAIN[sampleZ * SAMPLES + sampleX]);
        } else {
            context.fill(left, top, left + SIZE, top + SIZE, 0xFF386B3D);
        }

        int center = SIZE / 2;
        context.fill(left + center - 2, top + center - 2, left + center + 3, top + center + 3, 0xFFFFFFFF);
        context.fill(left + center - 1, top + center - 1, left + center + 2, top + center + 2, 0xFF191C22);
        context.fill(left, top, left + SIZE, top + 1, 0x66FFFFFF);
    }

    private static void sampleTerrain(MinecraftClient client) {
        lastSampleTick = client.player.age;
        int centerX = client.player.getBlockX();
        int centerZ = client.player.getBlockZ();
        for (int sampleZ = 0; sampleZ < SAMPLES; sampleZ++) for (int sampleX = 0; sampleX < SAMPLES; sampleX++) {
            int worldX = centerX + (sampleX - SAMPLES / 2) * SAMPLE_SIZE;
            int worldZ = centerZ + (sampleZ - SAMPLES / 2) * SAMPLE_SIZE;
            int surfaceY = client.world.getTopY(Heightmap.Type.WORLD_SURFACE, worldX, worldZ) - 1;
            TERRAIN[sampleZ * SAMPLES + sampleX] = colorFor(client.world.getBlockState(new BlockPos(worldX, surfaceY, worldZ)));
        }
    }

    private static int colorFor(BlockState state) {
        if (state.isOf(Blocks.WATER) || state.isOf(Blocks.KELP) || state.isOf(Blocks.KELP_PLANT)) return 0xFF2B6599;
        if (state.isOf(Blocks.LAVA)) return 0xFFE05B28;
        if (state.isOf(Blocks.SAND) || state.isOf(Blocks.RED_SAND)) return 0xFFD7C477;
        if (state.isOf(Blocks.SNOW) || state.isOf(Blocks.SNOW_BLOCK) || state.isOf(Blocks.ICE)) return 0xFFE3EDF4;
        if (state.isOf(Blocks.STONE) || state.isOf(Blocks.DEEPSLATE) || state.isOf(Blocks.GRAVEL)) return 0xFF77777A;
        if (state.isOf(Blocks.DIRT) || state.isOf(Blocks.COARSE_DIRT) || state.isOf(Blocks.PODZOL)) return 0xFF805D3E;
        if (state.isAir()) return 0xFF27303A;
        return 0xFF4F8A48;
    }
}
