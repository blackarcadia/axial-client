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
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.function.IntBinaryOperator;

/** Receives server-authoritative terrain from AxialWorldMap and PrisonsCore. */
public final class MinimapServerOverview {
    private static final byte REQUEST = 0;
    private static final byte SNAPSHOT = 1;
    private static final byte OVERLAYS = 2;
    private static final Map<String, Overview> OVERVIEWS = new HashMap<>();
    private static final Map<String, Overlay> OVERLAYS_BY_WORLD = new HashMap<>();
    private static final Map<String,ArmorZoneOverlay.MapData> ARMOR_BY_WORLD=new HashMap<>();
    private static String activeWorld,fullWorldKey;
    private static Overview fullWorld,pendingWorld;
    private static String pendingWorldKey;
    private static long pendingSnapshot,committedSnapshot=Long.MIN_VALUE;
    private static String cacheScope;
    private static final LinkedHashMap<String,Overview> WORLD_CACHE=new LinkedHashMap<>(8,.75f,true);
    private static long lastRequestAt;
    private static long revision;
    private static long worldGeneration;
    private static boolean fullRequestPending=true;
    private static boolean checkedProvider,fullProviderAvailable;
    private static String providerMessage="";
    private static boolean providerRendering;
    private static long lastFullAttempt,lastFullProgress;
    private static boolean wasVisible,pauseSent;
    private static TextureSnapshot textureSnapshot;

    /** Hidden maps unsubscribe once; showing a map requests the current complete snapshot. */
    public static void tick(boolean visible) {
        if(MinecraftClient.getInstance().getNetworkHandler()==null)return;
        if(!visible) {
            wasVisible=false;
            if(!pauseSent&&ClientPlayNetworking.canSend(WorldMapPayload.ID)) {
                ClientPlayNetworking.send(new WorldMapPayload(new byte[]{REQUEST,2}));pauseSent=true;
                pendingWorld=null;pendingWorldKey=null;
            }
            return;
        }
        if(!wasVisible){fullRequestPending=true;lastRequestAt=0;pauseSent=false;}
        wasVisible=true;request();
    }

    private MinimapServerOverview() { }

    private static void request() {
        if(MinecraftClient.getInstance().getNetworkHandler()==null)return;
        long now = System.currentTimeMillis();
        checkedProvider=true;
        fullProviderAvailable=ClientPlayNetworking.canSend(WorldMapPayload.ID);
        if((fullWorld==null||!fullWorld.complete())&&now-Math.max(lastFullAttempt,lastFullProgress)>=30_000)fullRequestPending=true;
        if (now - lastRequestAt < 5_000) return;
        boolean sent=false;
        if(fullProviderAvailable) {
            if(fullRequestPending)lastFullAttempt=now;
            ClientPlayNetworking.send(new WorldMapPayload(nextWorldRequest()));
            sent=true;
        }
        if(ClientPlayNetworking.canSend(MinimapMapPayload.ID)) {
            ClientPlayNetworking.send(new MinimapMapPayload(new byte[]{REQUEST}));sent=true;
        }
        if(sent)lastRequestAt = now;
    }

    static byte[] nextWorldRequest() {
        byte[] request=fullRequestPending?new byte[]{REQUEST,1}:new byte[]{REQUEST};
        fullRequestPending=false;
        return request;
    }

    /** Clears the previous world's map while the server sends the next world's overview. */
    public static void beginWorldChange() {
        beginWorldChange(null);
        WORLD_CACHE.clear();
    }
    public static void beginWorldChange(String nextScope) {
        if(cacheScope!=null&&fullWorld!=null&&fullWorldKey!=null) {
            WORLD_CACHE.put(cacheScope+"\0"+fullWorldKey,fullWorld.copy());
            if(WORLD_CACHE.size()>8)WORLD_CACHE.remove(WORLD_CACHE.keySet().iterator().next());
        }
        cacheScope=nextScope;
        activeWorld = null;fullWorld=null;fullWorldKey=null;
        pendingWorld=null;pendingWorldKey=null;pendingSnapshot=committedSnapshot=Long.MIN_VALUE;
        textureSnapshot=null;wasVisible=pauseSent=false;
        OVERVIEWS.clear();OVERLAYS_BY_WORLD.clear();ARMOR_BY_WORLD.clear();revision++;
        lastRequestAt = 0L;
        fullRequestPending=true;worldGeneration++;
        checkedProvider=fullProviderAvailable=providerRendering=false;providerMessage="";lastFullAttempt=lastFullProgress=0;
    }

    public static void receive(byte[] payload) {
        if (payload.length == 0) return;
        if(payload[0]==3) {
            try{var zones=ArmorZoneOverlay.read(payload);ARMOR_BY_WORLD.put(zones.world(),zones);}catch(IOException ignored){ }
            return;
        }
        if (payload[0] == OVERLAYS) {
            receiveOverlays(payload);
            return;
        }
        readSnapshot(payload,false);
    }

    public static void receiveFullWorld(byte[] payload) {
        if(payload.length>0&&payload[0]==3) {
            try(var input=new DataInputStream(new ByteArrayInputStream(payload))) {
                input.readByte();String message=readString(input);
                int done=input.readInt(),total=input.readInt();
                if(done<0||total<0||done>total||input.available()!=0)return;
                providerMessage=message+(total>0?" ("+(int)(done*100L/total)+"%)":"");
                providerRendering=total>0&&done<total;
                checkedProvider=fullProviderAvailable=true;
            } catch(IOException ignored){ }
            return;
        }
        if(payload.length>0&&payload[0]==4){readCompleteSnapshot(payload);return;}
        readSnapshot(payload,true);
    }
    /** Stage a revision separately; renderer readers only ever capture complete immutable images. */
    private static void readCompleteSnapshot(byte[] payload) {
        try(var input=new DataInputStream(new ByteArrayInputStream(payload))) {
            input.readByte();String world=readString(input);long snapshot=input.readLong();
            int minX=input.readInt(),minZ=input.readInt(),step=input.readInt();
            int width=input.readUnsignedShort(),height=input.readUnsignedShort();
            int start=input.readUnsignedShort(),count=input.readUnsignedShort();
            if(step<=0||step>1_000_000||width<=0||height<=0||width>512||height>512||count==0||start+count>height||input.available()!=count*width*4)return;
            if(world.equals(fullWorldKey)&&snapshot<committedSnapshot)return;
            if(world.equals(pendingWorldKey)&&snapshot<pendingSnapshot)return;
            if(pendingWorld==null||!world.equals(pendingWorldKey)||snapshot!=pendingSnapshot
                    ||!pendingWorld.matches(minX,minZ,step,width,height)) {
                pendingWorld=new Overview(minX,minZ,step,width,height);pendingWorldKey=world;pendingSnapshot=snapshot;
            }
            for(int i=start*width;i<(start+count)*width;i++)pendingWorld.colors[i]=input.readInt();
            pendingWorld.rows.set(start,start+count);
            lastFullProgress=System.currentTimeMillis();checkedProvider=fullProviderAvailable=true;
            if(pendingWorld.complete()) {
                fullWorld=pendingWorld;fullWorldKey=world;committedSnapshot=snapshot;
                pendingWorld=null;pendingWorldKey=null;providerRendering=false;providerMessage="";revision++;
                textureSnapshot=new TextureSnapshot(fullWorld.minX,fullWorld.minZ,fullWorld.blockStep,fullWorld.width,fullWorld.height,fullWorld.colors);

            }
        } catch(IOException ignored){ }
    }
    private static void readSnapshot(byte[] payload,boolean full) {
        if(payload.length==0||payload[0]!=SNAPSHOT)return;
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

            Overview overview;
            if(full) {
                Overview cached=cacheScope==null?null:WORLD_CACHE.get(cacheScope+"\0"+world);
                overview=world.equals(fullWorldKey)&&fullWorld!=null&&fullWorld.matches(minX,minZ,blockStep,width,height)?fullWorld:
                        cached!=null&&cached.matches(minX,minZ,blockStep,width,height)?cached.copy():new Overview(minX,minZ,blockStep,width,height);
                fullWorld=overview;fullWorldKey=world;
            } else overview=OVERVIEWS.compute(world,(ignored,current)->current!=null&&current.matches(minX,minZ,blockStep,width,height)?current:new Overview(minX,minZ,blockStep,width,height));
            for (int row = 0; row < rowCount; row++) {
                int offset = (rowStart + row) * width;
                for (int column = 0; column < width; column++) overview.colors[offset + column] = input.readInt();
            }
            overview.rows.set(rowStart,rowStart+rowCount);
            if(full){textureSnapshot=null;lastFullProgress=System.currentTimeMillis();checkedProvider=fullProviderAvailable=true;if(!providerRendering)providerMessage="";}
            if(!full)activeWorld = world;
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

    /** Only complete, revision-tagged images use the direct texture path; arrays stay immutable. */
    static TextureSnapshot textureSnapshot(){return textureSnapshot;}
    static final class TextureSnapshot {
        final int minX,minZ,step,width,height;
        final int[] colors;
        final boolean opaque;
        TextureSnapshot(int minX,int minZ,int step,int width,int height,int[] colors) {
            this.minX=minX;this.minZ=minZ;this.step=step;this.width=width;this.height=height;this.colors=colors;
            boolean filled=true;for(int color:colors)if((color>>>24)!=255){filled=false;break;}opaque=filled;
        }
    }
    public static String fullWorldIdentity(){return fullWorldKey;}
    static boolean awaitingPreparedWorld(){return fullProviderAvailable&&fullWorldKey==null;}
    public static long revision(){return revision;}
    public static long worldGeneration(){return worldGeneration;}
    public static String loadingMessage() {
        if(pendingWorld!=null&&fullWorld==null)return "Loading cached full map: "+pendingWorld.rows.cardinality()*100/pendingWorld.height+"%";
        if(providerRendering)return providerMessage;
        if(fullWorld!=null)return fullWorld.complete()?"":"Loading full map: "+fullWorld.rows.cardinality()*100/fullWorld.height+"%";
        if(!providerMessage.isEmpty())return providerMessage;
        if(checkedProvider&&!fullProviderAvailable)return "Full-world map unavailable on this server";
        return "Waiting for full-world map...";
    }
    public static boolean available(){return fullWorld!=null||activeWorld!=null&&OVERVIEWS.containsKey(activeWorld);}
    public record WorldBounds(String world,double centerX,double centerZ,double scale) { }
    public static WorldBounds fullWorldBounds(){return fullWorld==null?null:new WorldBounds(fullWorldKey,fullWorld.minX+fullWorld.width*(double)fullWorld.blockStep/2,fullWorld.minZ+fullWorld.height*(double)fullWorld.blockStep/2,Math.max(fullWorld.width,fullWorld.height)*(double)fullWorld.blockStep/256);}
    public static int colorAt(int x,int z) {
        return compositeColor(fullWorld,activeWorld==null?null:OVERVIEWS.get(activeWorld),x,z);
    }
    /** Captures one world's array, so worker builds cannot switch worlds halfway through. */
    static IntBinaryOperator terrainReader() {
        Overview overview=fullWorld,legacy=activeWorld==null?null:OVERVIEWS.get(activeWorld);
        return (x,z)->compositeColor(overview,legacy,x,z);
    }
    private static int compositeColor(Overview full,Overview legacy,int x,int z) {
        if(full==null)return colorAt(legacy,x,z);
        if(!contains(full,x,z))return 0;
        int color=colorAt(full,x,z);
        return color==0?colorAt(legacy,x,z):color;
    }
    private static boolean contains(Overview o,int x,int z) {
        long dx=(long)x-o.minX,dz=(long)z-o.minZ;
        return dx>=0&&dz>=0&&dx<(long)o.width*o.blockStep&&dz<(long)o.height*o.blockStep;
    }
    private static int colorAt(Overview o,int x,int z) {
        if(o==null)return 0;
        long dx=(long)x-o.minX,dz=(long)z-o.minZ;
        if(dx<0||dz<0||dx>=(long)o.width*o.blockStep||dz>=(long)o.height*o.blockStep)return 0;
        return o.colors[(int)(dz/o.blockStep)*o.width+(int)(dx/o.blockStep)];
    }

    /** Server zones and POIs drawn over the terrain map in the HUD. */
    public static void renderHudOverlays(DrawContext context, int left, int top, int size, int mapPixels,
                                         double centerX, double centerZ, float blocksPerPixel, boolean circular,boolean full) {
        renderArmorOutlines(context,left,top,size,centerX,centerZ,blocksPerPixel,circular,full);
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
            MinimapZoneOutline.draw(left,top,size,circular,
                    left+size/2.0+(zone.minX-centerX)*pixelsPerBlock,
                    top+size/2.0+(zone.minZ-centerZ)*pixelsPerBlock,
                    left+size/2.0+((double)zone.maxX+1-centerX)*pixelsPerBlock,
                    top+size/2.0+((double)zone.maxZ+1-centerZ)*pixelsPerBlock,
                    (x1,y1,x2,y2)->context.fill(x1,y1,x2,y2,zone.color|0xFF000000));
            float x = left + size / 2.0f + (float) ((((double)zone.minX + zone.maxX) / 2.0 - centerX) * pixelsPerBlock);
            float z = top + size / 2.0f + (float) ((((double)zone.minZ + zone.maxZ) / 2.0 - centerZ) * pixelsPerBlock);
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

    /** Include overlapping safe/custom zones and the underlying armour tier. */
    static List<String> zoneNamesAt(double x,double z) {
        if(activeWorld==null||!Double.isFinite(x)||!Double.isFinite(z))return List.of();
        var names=new java.util.LinkedHashSet<String>();
        var overlay=OVERLAYS_BY_WORLD.get(activeWorld);
        if(overlay!=null)for(var zone:overlay.zones) {
            if(x>=zone.minX&&x<(double)zone.maxX+1&&z>=zone.minZ&&z<(double)zone.maxZ+1)names.add(zone.name);
        }
        var armor=armorForActiveWorld();
        if(armor!=null){String tier=armor.zoneAt(x,z);if(tier!=null)names.add(tier);}
        return List.copyOf(names);
    }

    static ArmorZoneOverlay.MapData armorForActiveWorld(){return activeWorld==null?null:ARMOR_BY_WORLD.get(activeWorld);}
    private static void renderArmorOutlines(DrawContext context,int left,int top,int size,double centerX,double centerZ,float scale,boolean circular,boolean full) {
        var overlay=armorForActiveWorld();if(overlay==null)return;
        double pixelsPerBlock=size/(scale*256.0);
        for(var region:overlay.regions()) {
            for(double[] ring:region.rings())for(int i=0,j=ring.length-2;i<ring.length;j=i,i+=2) {
                double x1=left+size/2.0+(ring[j]-centerX)*pixelsPerBlock,y1=top+size/2.0+(ring[j+1]-centerZ)*pixelsPerBlock;
                double x2=left+size/2.0+(ring[i]-centerX)*pixelsPerBlock,y2=top+size/2.0+(ring[i+1]-centerZ)*pixelsPerBlock;
                var line=MinimapLineClip.clip(x1,y1,x2,y2,left,top,size,circular);if(line==null)continue;
                double dx=line.x2()-line.x1(),dy=line.y2()-line.y1();
                context.getMatrices().pushMatrix();
                try {
                    context.getMatrices().translate((float)line.x1(),(float)line.y1());
                    context.getMatrices().rotate((float)Math.atan2(dy,dx));
                    context.fill(0,-1,Math.max(1,(int)Math.ceil(Math.hypot(dx,dy))),1,region.color()|0xFF000000);
                } finally{context.getMatrices().popMatrix();}
            }
            double x=left+size/2.0+(region.labelX()-centerX)*pixelsPerBlock,z=top+size/2.0+(region.labelZ()-centerZ)*pixelsPerBlock;
            if(full&&inside(left,top,size,(float)x,(float)z,false))NavigationScreen.line(context,MinecraftClient.getInstance().textRenderer,region.name(),(int)x+4,(int)z+4,ModMenuUi.INK,.75f,Math.max(0,left+size-(int)x-8),false);
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
        private final BitSet rows=new BitSet();

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
        private boolean complete(){return rows.cardinality()==height;}
        private Overview copy() {
            Overview copy=new Overview(minX,minZ,blockStep,width,height);
            System.arraycopy(colors,0,copy.colors,0,colors.length);copy.rows.or(rows);return copy;
        }
    }

    private record Overlay(List<Zone> zones, List<Poi> pois) { }
    private record Zone(String name, int minX, int minZ, int maxX, int maxZ, int color) { }
    private record Poi(String name, int x, int z, int color) { }
}
