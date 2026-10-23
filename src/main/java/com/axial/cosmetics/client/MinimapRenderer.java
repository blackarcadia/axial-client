package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.block.MapColor;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import java.util.concurrent.*;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** Budgeted terrain discovery, shared projections, and cached map textures. */
public final class MinimapRenderer {
    private static final int UNKNOWN=0xFF20252B;
    private static final int HUD_PIXELS=256,FULL_PIXELS=1024;
    private static final Identifier HUD=Identifier.of(AxialCosmetics.MOD_ID,"dynamic/minimap");
    private static final Identifier FULL=Identifier.of(AxialCosmetics.MOD_ID,"dynamic/full_map");
    private static NativeImage hudImage,fullImage;
    private static NativeImageBackedTexture hudTexture,fullTexture;
    private static String worldKey="";
    private static net.minecraft.client.world.ClientWorld lastWorld;
    private static int sampledTick=Integer.MIN_VALUE,row,column,lastDrawTick=Integer.MIN_VALUE;
    private static final ExecutorService RASTER=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"axial-map-raster");t.setDaemon(true);return t;});
    private record ImageBuild(double x,double z,double scale,long stamp,long generation,boolean circle) { }
    private static Future<ImageBuild> hudBuild,fullBuild;
    private static boolean hudReady,fullReady;
    private static int sampleX,sampleZ;
    private static int sampleWidth;
    private static long hudStamp,fullStamp,lastHudUpload,lastFullUpload;
    private static double hudX=Double.NaN,hudZ,hudScale,fullX=Double.NaN,fullZ,fullScale;
    private static boolean hudCircular;
    private MinimapRenderer() { }

    /** Called at connection/world boundaries, before any new overview is rendered. */
    public static void refreshWorld(MinecraftClient client) {
        lastWorld=client.world;
        worldKey=contextKey(client);
        sampledTick=lastDrawTick=Integer.MIN_VALUE;row=column=0;hudX=fullX=Double.NaN;hudReady=fullReady=false;
        lastHudUpload=lastFullUpload=0;
        MinimapServerOverview.beginWorldChange(worldKey);
    }
    private static String contextKey(MinecraftClient client) {
        return client.world==null?"":WaypointConfig.worldId(client)+"|"+client.world.getRegistryKey().getValue();
    }
    /** Runs even when the HUD minimap and full-map screen are closed. */
    public static void tick(MinecraftClient client) {
        syncWorld(client);
        if(client.world!=null&&client.player!=null) {
            MinimapServerOverview.request();
            boolean full=client.currentScreen instanceof MinimapFullScreen;
            if(full||MinimapConfig.isEnabled()&&!client.options.hudHidden&&client.currentScreen==null
                    ||lastDrawTick!=Integer.MIN_VALUE&&client.player.age-lastDrawTick<=2)sample(client,full);
        }
    }
    public static void syncWorld(MinecraftClient client) {
        if(lastWorld!=client.world||!worldKey.equals(contextKey(client)))refreshWorld(client);
    }

    public static void render(DrawContext c,RenderTickCounter ticks) {
        MinecraftClient client=MinecraftClient.getInstance();
        if(!MinimapConfig.isEnabled()||client.options.hudHidden||client.player==null||client.world==null||client.currentScreen!=null)return;
        render(c,client);
    }
    public static void renderPreview(DrawContext c,MinecraftClient client){if(MinimapConfig.isEnabled())render(c,client);}
    private static void render(DrawContext c,MinecraftClient client) {
        int size=Math.min(MinimapConfig.getSize(),Math.min(c.getScaledWindowWidth()-12,c.getScaledWindowHeight()-32));
        if(size<16)return;
        int left=Math.clamp(MinimapConfig.getX(c.getScaledWindowWidth()),6,c.getScaledWindowWidth()-size-6);
        int top=Math.clamp(MinimapConfig.getY(c.getScaledWindowHeight()),6,c.getScaledWindowHeight()-size-26);
        drawHud(c,client,left,top,size);
    }
    public static void renderSettingsPreview(DrawContext c,MinecraftClient client,int x,int y,int width,int height) {
        if(client.world==null||client.player==null){label(c,client.textRenderer,"Join a world to preview",x+8,y+height/2,MUTED,.85f,width-16,false);return;}
        int size=Math.max(16,Math.min(Math.min(width-20,height-32),MinimapConfig.getSize()));
        drawHud(c,client,x+(width-size)/2,y+(height-size-18)/2,size);
    }
    private static void drawHud(DrawContext c,MinecraftClient client,int left,int top,int size) {
        if(client.world==null||client.player==null)return;
        update(client);
        boolean circle=MinimapConfig.isCircular();
        double scale=MinimapConfig.zoom()*.75,step=scale*256/HUD_PIXELS;
        var view=new MapViewport(Math.rint(client.player.getX()/step)*step,Math.rint(client.player.getZ()/step)*step,scale);
        if(!circle)frame(c,new ModMenuLayout.Rect(left-3,top-3,size+6,size+6),LINE,PANEL);
        else disc(c,left-3,top-3,size+6,PANEL);
        upload(client,view,false,circle);
        if(hudReady)c.drawTexture(RenderPipelines.GUI_TEXTURED,HUD,left,top,0,0,size,size,HUD_PIXELS,HUD_PIXELS,HUD_PIXELS,HUD_PIXELS);
        c.enableScissor(left,top,left+size,top+size);
        overlays(c,client,view,left,top,size,circle,false);
        c.disableScissor();
        label(c,client.textRenderer,"N",left+size/2-3,top+4,BRAND,.85f,12,true);
        if(MinimapConfig.showCoordinates()) {
            rounded(c,left-3,top+size+5,size+6,16,PANEL);
            String value=client.player.getBlockX()+"  /  "+client.player.getBlockY()+"  /  "+client.player.getBlockZ();
            int w=UiTypography.width(client.textRenderer,value,.75f,false);
            label(c,client.textRenderer,value,left+Math.max(2,(size-w)/2),top+size+10,MUTED,.75f,size-4,false);
        }
    }
    public static void renderFullScreen(DrawContext c,MinecraftClient client,float x,float z,float scale) {
        int size=Math.max(1,Math.min(c.getScaledWindowWidth()-48,c.getScaledWindowHeight()-48));
        renderFullMap(c,client,(c.getScaledWindowWidth()-size)/2,(c.getScaledWindowHeight()-size)/2,size,new MapViewport(x,z,scale));
    }
    static void renderFullMap(DrawContext c,MinecraftClient client,int left,int top,int size,MapViewport view) {
        if(client.world==null||client.player==null)return;
        update(client);upload(client,view,true,false);
        c.enableScissor(left,top,left+size,top+size);
        if(fullReady)c.drawTexture(RenderPipelines.GUI_TEXTURED,FULL,left,top,0,0,size,size,FULL_PIXELS,FULL_PIXELS,FULL_PIXELS,FULL_PIXELS);
        overlays(c,client,view,left,top,size,false,true);
        c.disableScissor();
    }
    private static void update(MinecraftClient client) {
        syncWorld(client);
        lastDrawTick=client.player.age;
    }
    private static void sample(MinecraftClient client,boolean full) {
        // Live world access stays on the game thread, bounded by time and column count.
        if(sampledTick==client.player.age)return;
        sampledTick=client.player.age;
        if(row==0&&column==0) {
            sampleWidth=Math.max(144,(int)Math.ceil(192*MinimapConfig.zoom()));
            sampleX=client.player.getBlockX()-sampleWidth/2;
            sampleZ=client.player.getBlockZ()-sampleWidth/2;
        }
        MinimapTerrain pixels=MinimapExploration.terrainFor(client);
        BlockPos.Mutable pos=new BlockPos.Mutable(),bedPos=new BlockPos.Mutable();
        long deadline=System.nanoTime()+(full?1_500_000:750_000);
        int sampled=0,visited=0;
        while(row<sampleWidth&&sampled<(full?2048:512)&&visited<8192) {
            if((visited++&15)==0&&System.nanoTime()>=deadline)break;
            int x=sampleX+column,z=sampleZ+row;
            if(++column>=sampleWidth){column=0;row++;}
            if(!pixels.shouldSample(x,z,client.player.age))continue;
            if(!client.world.getChunkManager().isChunkLoaded(x>>4,z>>4))continue;
            sampled++;
            int y=client.world.getTopY(Heightmap.Type.WORLD_SURFACE,x,z)-1;
            pos.set(x,y,z);
            var state=client.world.getBlockState(pos);
            // Look through map-transparent decorations rather than punching holes in terrain.
            for(int depth=0;depth<16&&state.getMapColor(client.world,pos)==MapColor.CLEAR&&y>client.world.getBottomY();depth++) {
                pos.setY(--y);state=client.world.getBlockState(pos);
            }
            MapColor color=state.getMapColor(client.world,pos);
            if(color==MapColor.CLEAR)continue;
            int north=heightAt(client,x,z-1,y),west=heightAt(client,x-1,z,y);
            int east=heightAt(client,x+1,z,y),south=heightAt(client,x,z+1,y);
            boolean water=state.getFluidState().isIn(FluidTags.WATER);
            int waterDepth=0,bed=0;
            if(water) {
                // Inspect at most sixteen blocks vertically in this already-loaded column.
                // Keep opaque waterlogged blocks (e.g. slabs) as their own visible surface.
                water=state.getBlock()==net.minecraft.block.Blocks.WATER;
                if(water) {
                    bedPos.set(pos);
                    while(waterDepth<16&&bedPos.getY()>client.world.getBottomY()) {
                        bedPos.setY(bedPos.getY()-1);waterDepth++;
                        var below=client.world.getBlockState(bedPos);
                        if(!below.getFluidState().isIn(FluidTags.WATER)) {
                            if(!below.isAir())bed=below.getMapColor(client.world,bedPos).getRenderColor(MapColor.Brightness.NORMAL);
                            break;
                        }
                    }
                }
            }
            double shade=water?1:MinimapRelief.shade(y,north,east,south,west);
            int argb=MinimapRelief.color(color.getRenderColor(MapColor.Brightness.NORMAL),shade,bed,waterDepth);
            MinimapExploration.discover(pixels,x,z,argb);
            pixels.sampled(x,z,client.player.age);
        }
        if(row>=sampleWidth)row=0;
    }

    private static int heightAt(MinecraftClient client,int x,int z,int fallback) {
        return client.world.getChunkManager().isChunkLoaded(x>>4,z>>4)
                ?client.world.getTopY(Heightmap.Type.WORLD_SURFACE,x,z)-1:fallback;
    }

    private static void upload(MinecraftClient client,MapViewport view,boolean full,boolean circle) {
        int pixels=full?FULL_PIXELS:HUD_PIXELS;
        if(full&&fullTexture==null){fullImage=new NativeImage(pixels,pixels,true);fullTexture=new NativeImageBackedTexture(()->"axial/full_map",fullImage);client.getTextureManager().registerTexture(FULL,fullTexture);}
        if(!full&&hudTexture==null){hudImage=new NativeImage(pixels,pixels,true);hudTexture=new NativeImageBackedTexture(()->"axial/minimap",hudImage);client.getTextureManager().registerTexture(HUD,hudTexture);}
        Future<ImageBuild> pending=full?fullBuild:hudBuild;
        if(pending!=null) {
            if(!pending.isDone())return;
            try {
                ImageBuild result=pending.get();
                if(result.generation==MinimapServerOverview.worldGeneration()) {
                    if(full) {
                        fullTexture.upload();fullReady=true;fullX=result.x;fullZ=result.z;fullScale=result.scale;fullStamp=result.stamp;lastFullUpload=System.currentTimeMillis();
                    } else {
                        hudTexture.upload();hudReady=true;hudX=result.x;hudZ=result.z;hudScale=result.scale;hudStamp=result.stamp;hudCircular=result.circle;lastHudUpload=System.currentTimeMillis();
                    }
                }
            } catch(InterruptedException ex){Thread.currentThread().interrupt();}
            catch(ExecutionException ex){org.slf4j.LoggerFactory.getLogger(MinimapRenderer.class).warn("Could not build map image",ex.getCause());}
            if(full)fullBuild=null;else hudBuild=null;
        }
        long stamp=MinimapExploration.revision()*31+MinimapServerOverview.revision();
        long now=System.currentTimeMillis();
        boolean unchanged=full?view.centerX==fullX&&view.centerZ==fullZ&&view.scale==fullScale&&stamp==fullStamp:
                view.centerX==hudX&&view.centerZ==hudZ&&view.scale==hudScale&&circle==hudCircular&&stamp==hudStamp;
        if(unchanged|| (full?now-lastFullUpload<100:now-lastHudUpload<50))return;
        NativeImage target=full?fullImage:hudImage;
        var local=MinimapExploration.terrainFor(client).reader();
        var server=MinimapServerOverview.terrainReader();
        boolean localDetail=view.scale*256<=2048;
        ImageBuild build=new ImageBuild(view.centerX,view.centerZ,view.scale,stamp,MinimapServerOverview.worldGeneration(),circle);
        Future<ImageBuild> future=RASTER.submit(()->{
            MinimapRaster.paint(pixels,build.x,build.z,build.scale,circle,(x,z)->{
                int color=localDetail?local.applyAsInt(x,z):server.applyAsInt(x,z);
                if(color==0)color=localDetail?server.applyAsInt(x,z):local.applyAsInt(x,z);
                return color==0?UNKNOWN:color;
            },target::setColorArgb);
            return build;
        });
        if(full)fullBuild=future;else hudBuild=future;
    }
    private static void overlays(DrawContext c,MinecraftClient client,MapViewport view,int left,int top,int size,boolean circle,boolean names) {
        MinimapServerOverview.renderHudOverlays(c,left,top,size,256,view.centerX,view.centerZ,(float)view.scale,circle,names);
        if(WaypointConfig.enabled())for(var e:WaypointConfig.visibleFor(client)) {
            int x=(int)Math.round(view.pixelX(e.x()+.5,left,size)),y=(int)Math.round(view.pixelZ(e.z()+.5,top,size));
            if(!inside(x,y,left,top,size,circle,6))continue;
            ModMenuUi.circle(c,x-4,y-4,9,0xFF101419);ModMenuUi.circle(c,x-2,y-2,5,e.color());
            if(names||MinimapConfig.showWaypointNames()) {
                int edge=left+size;
                if(circle){double radius=size/2.0-2,dy=Math.max(Math.abs(y-3-top-size/2.0),Math.abs(y+5-top-size/2.0));edge=left+size/2+(int)Math.sqrt(Math.max(0,radius*radius-dy*dy));}
                label(c,client.textRenderer,e.name(),x+7,y-3,INK,.75f,Math.max(0,edge-x-10),false);
            }
        }
        var death=MinimapExploration.deathFor(client);
        if(death!=null&&MinimapConfig.showDeath()) {
            int x=(int)Math.round(view.pixelX(death.x(),left,size)),y=(int)Math.round(view.pixelZ(death.z(),top,size));
            if(inside(x,y,left,top,size,circle,6)){label(c,client.textRenderer,"X",x-3,y-4,RED,.9f,10,true);}
        }
        int x=(int)Math.round(view.pixelX(client.player.getX(),left,size)),y=(int)Math.round(view.pixelZ(client.player.getZ(),top,size));
        if(inside(x,y,left,top,size,circle,8)) {
            ModMenuUi.circle(c,x-5,y-5,11,0xFF14171B);ModMenuUi.circle(c,x-3,y-3,7,INK);
            double angle=Math.toRadians(client.player.getYaw());
            int tx=x-(int)Math.round(Math.sin(angle)*9),ty=y+(int)Math.round(Math.cos(angle)*9);
            ModMenuUi.circle(c,tx-2,ty-2,4,BRAND);
        }
    }
    private static boolean inside(int x,int y,int l,int t,int size,boolean circle,int pad) {
        return x>=l+pad&&x<l+size-pad&&y>=t+pad&&y<t+size-pad&&(!circle||Math.hypot(x-l-size/2.0,y-t-size/2.0)<size/2.0-pad);
    }
    private static void disc(DrawContext c,int x,int y,int size,int color) {
        for(int row=0;row<size;row++){double dy=row+.5-size/2.0;int half=(int)Math.sqrt(Math.max(0,size*size/4.0-dy*dy));c.fill(x+size/2-half,y+row,x+size/2+half,y+row+1,color);}
    }
}
