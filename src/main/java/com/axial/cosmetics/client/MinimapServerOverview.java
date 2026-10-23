package com.axial.cosmetics.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

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
    private static final Map<String, Overview> OVERVIEWS = new HashMap<>();
    private static final Map<String, Overlay> OVERLAYS_BY_WORLD = new HashMap<>();
    private static String activeWorld;
    private static long lastRequestAt;
    private static long revision;

    private MinimapServerOverview() { }

    public static void request() {
        if(MinecraftClient.getInstance().getNetworkHandler()==null||!ClientPlayNetworking.canSend(MinimapMapPayload.ID))return;
        long now = System.currentTimeMillis();
        if (now - lastRequestAt < 5_000) return;
        lastRequestAt = now;
        ClientPlayNetworking.send(new MinimapMapPayload(new byte[]{REQUEST}));
    }

    /** Clears the previous world's map while the server sends the next world's overview. */
    public static void beginWorldChange() {
        activeWorld = null;
        OVERVIEWS.clear();OVERLAYS_BY_WORLD.clear();revision++;
        lastRequestAt = 0L;
    }

    public static void receive(byte[] payload) {
        if (payload.length == 0) return;
        if (payload[0] == OVERLAYS) {
            receiveOverlays(payload);
            return;
        }
        if (payload[0] != SNAPSHOT) return;
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            input.readByte();
            String world = readString(input);
            int minX = input.readInt();
            int minZ = input.readInt();
            int blockStep = input.readInt();
            int width = input.readUnsignedShort();
            int height = input.readUnsignedShort();
            int rowStart = input.readUnsignedShort();
            int rowCount = input.readUnsignedShort();
            if (blockStep <= 0 || blockStep > 1_000_000 || width <= 0 || height <= 0 || width > 512 || height > 512 || rowCount == 0 || rowStart + rowCount > height || input.available() < rowCount * width * 4) return;

            Overview overview = OVERVIEWS.compute(world, (ignored, current) -> current != null && current.matches(minX, minZ, blockStep, width, height)
                    ? current : new Overview(minX, minZ, blockStep, width, height));
            for (int row = 0; row < rowCount; row++) {
                int offset = (rowStart + row) * width;
                for (int column = 0; column < width; column++) overview.colors[offset + column] = input.readInt();
            }
            activeWorld = world;
            revision++;
        } catch (IOException ignored) { }
    }

    private static void receiveOverlays(byte[] payload) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            input.readByte();
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
            revision++;
        } catch (IOException ignored) { }
    }

    public static long revision(){return revision;}
    public static boolean available(){return activeWorld!=null&&OVERVIEWS.containsKey(activeWorld);}
    public static int colorAt(int x,int z) {
        Overview o=activeWorld==null?null:OVERVIEWS.get(activeWorld);
        if(o==null)return 0;
        long dx=(long)x-o.minX,dz=(long)z-o.minZ;
        if(dx<0||dz<0||dx>=(long)o.width*o.blockStep||dz>=(long)o.height*o.blockStep)return 0;
        return o.colors[(int)(dz/o.blockStep)*o.width+(int)(dx/o.blockStep)];
    }

    /** Server zones and POIs drawn over the terrain map in the HUD. */
    public static void renderHudOverlays(DrawContext context, int left, int top, int size, int mapPixels,
                                         double centerX, double centerZ, float blocksPerPixel, boolean circular,boolean full) {
        Overlay overlay = activeWorld == null ? null : OVERLAYS_BY_WORLD.get(activeWorld);
        if (overlay == null) return;
        float pixelsPerBlock = size / (mapPixels * blocksPerPixel);
        for (Zone zone : overlay.zones) {
            if(full) {
                int x1=(int)Math.max(left,Math.min(left+size,left+size/2.0+(zone.minX-centerX)*pixelsPerBlock));
                int x2=(int)Math.max(left,Math.min(left+size,left+size/2.0+((double)zone.maxX+1-centerX)*pixelsPerBlock));
                int z1=(int)Math.max(top,Math.min(top+size,top+size/2.0+(zone.minZ-centerZ)*pixelsPerBlock));
                int z2=(int)Math.max(top,Math.min(top+size,top+size/2.0+((double)zone.maxZ+1-centerZ)*pixelsPerBlock));
                if(x2>x1&&z2>z1){cZone(context,x1,z1,x2,z2,zone.color);NavigationScreen.line(context,MinecraftClient.getInstance().textRenderer,zone.name,x1+4,z1+4,ModMenuUi.INK,.75f,x2-x1-8,false);}
                continue;
            }
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
            if(full)NavigationScreen.line(context,MinecraftClient.getInstance().textRenderer,poi.name,Math.round(x)+5,Math.round(z)-3,ModMenuUi.INK,.75f,Math.max(0,left+size-Math.round(x)-8),false);
        }
    }

    private static void cZone(DrawContext c,int x1,int z1,int x2,int z2,int color) {
        int opaque=color|0xFF000000;
        c.fill(x1,z1,x2,z2,(color&0xFFFFFF)|0x33000000);
        c.fill(x1,z1,x2,z1+1,opaque);c.fill(x1,z2-1,x2,z2,opaque);
        c.fill(x1,z1,x1+1,z2,opaque);c.fill(x2-1,z1,x2,z2,opaque);
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

    private static String readString(DataInputStream input) throws IOException {
        int length = input.readUnsignedShort();
        if (length > 255) throw new IOException("World name is too long");
        byte[] value=input.readNBytes(length);
        if(value.length!=length)throw new IOException("Truncated world name");
        return new String(value, StandardCharsets.UTF_8);
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
