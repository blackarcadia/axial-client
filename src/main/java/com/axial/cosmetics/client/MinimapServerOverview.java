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
import java.util.Map;

/** Receives compact, server-authoritative world overviews from PrisonsCore. */
public final class MinimapServerOverview {
    private static final byte REQUEST = 0;
    private static final byte SNAPSHOT = 1;
    private static final Identifier TEXTURE_ID = Identifier.of("axial_cosmetics", "dynamic/server_minimap_overview");
    private static final Map<String, Overview> OVERVIEWS = new HashMap<>();
    private static String activeWorld;
    private static NativeImage image;
    private static NativeImageBackedTexture texture;
    private static boolean textureDirty;
    private static long lastRequestAt;

    private MinimapServerOverview() { }

    public static void request() {
        long now = System.currentTimeMillis();
        if (now - lastRequestAt < 5_000) return;
        lastRequestAt = now;
        ClientPlayNetworking.send(new MinimapMapPayload(new byte[]{REQUEST}));
    }

    public static void receive(byte[] payload) {
        if (payload.length == 0 || payload[0] != SNAPSHOT) return;
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
            textureDirty = true;
        } catch (IOException ignored) { }
    }

    public static boolean render(DrawContext context, MinecraftClient client, int left, int top, int size) {
        Overview overview = activeWorld == null ? null : OVERVIEWS.get(activeWorld);
        if (overview == null) return false;
        ensureTexture(client, overview);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, left, top, 0.0f, 0.0f, size, size,
                overview.width, overview.height, overview.width, overview.height);
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
}
