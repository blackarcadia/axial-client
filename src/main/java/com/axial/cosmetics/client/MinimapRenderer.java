package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import java.util.Map;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** Budgeted terrain discovery, shared projections, and cached map textures. */
public final class MinimapRenderer {
    private static final int UNKNOWN=0xFF20252B;
    private static final Identifier HUD=Identifier.of(AxialCosmetics.MOD_ID,"dynamic/minimap");
    private static final Identifier FULL=Identifier.of(AxialCosmetics.MOD_ID,"dynamic/full_map");
    private static NativeImage hudImage,fullImage;
    private static NativeImageBackedTexture hudTexture,fullTexture;
    private static String worldKey="";
    private static net.minecraft.client.world.ClientWorld lastWorld;
    private static int sampledTick=Integer.MIN_VALUE,row;
    private static int sampleX,sampleZ;
    private static float sampleScale;
    private static long hudStamp,fullStamp,lastHudUpload,lastFullUpload;
    private static double hudX=Double.NaN,hudZ,hudScale,fullX=Double.NaN,fullZ,fullScale;
    private static boolean hudCircular;
    private MinimapRenderer() { }

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
        var view=new MapViewport(client.player.getX(),client.player.getZ(),MinimapConfig.zoom()*.75);
        if(!circle)frame(c,new ModMenuLayout.Rect(left-3,top-3,size+6,size+6),LINE,PANEL);
        else disc(c,left-3,top-3,size+6,PANEL);
        upload(client,view,false,circle);
        c.drawTexture(RenderPipelines.GUI_TEXTURED,HUD,left,top,0,0,size,size,128,128,128,128);
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
        c.drawTexture(RenderPipelines.GUI_TEXTURED,FULL,left,top,0,0,size,size,256,256,256,256);
        overlays(c,client,view,left,top,size,false,true);
        c.disableScissor();
    }
    private static void update(MinecraftClient client) {
        String key=WaypointConfig.worldId(client)+"|"+client.world.getRegistryKey().getValue();
        if(lastWorld!=client.world||!worldKey.equals(key)) {
            lastWorld=client.world;
            worldKey=key;sampledTick=Integer.MIN_VALUE;row=0;hudX=fullX=Double.NaN;
            MinimapServerOverview.beginWorldChange();
        }
        MinimapServerOverview.request();
        if(MinimapServerOverview.available())return;
        // Terrain reads are bounded to 2,048 columns per tick and never load chunks.
        if(sampledTick==client.player.age)return;
        sampledTick=client.player.age;
        if(row==0){sampleX=client.player.getBlockX();sampleZ=client.player.getBlockZ();sampleScale=MinimapConfig.zoom()*1.5f;}
        Map<String,Integer> pixels=MinimapExploration.terrainFor(client);
        BlockPos.Mutable pos=new BlockPos.Mutable();
        for(int end=Math.min(128,row+16);row<end;row++)for(int col=0;col<128;col++) {
            int x=(int)Math.floor(sampleX+(col-64)*sampleScale),z=(int)Math.floor(sampleZ+(row-64)*sampleScale);
            if(!client.world.getChunkManager().isChunkLoaded(x>>4,z>>4))continue;
            int y=client.world.getTopY(Heightmap.Type.WORLD_SURFACE,x,z)-1;
            pos.set(x,y,z);
            MapColor color=client.world.getBlockState(pos).getMapColor(client.world,pos);
            if(color==MapColor.CLEAR)continue;
            int north=client.world.getChunkManager().isChunkLoaded(x>>4,(z-1)>>4)?client.world.getTopY(Heightmap.Type.WORLD_SURFACE,x,z-1)-1:y;
            MapColor.Brightness shade=y>north?MapColor.Brightness.HIGH:y<north?MapColor.Brightness.LOW:MapColor.Brightness.NORMAL;
            int argb=color.getRenderColor(shade);
            int endX=(int)Math.floor(sampleX+(col+1-64)*sampleScale),endZ=(int)Math.floor(sampleZ+(row+1-64)*sampleScale);
            // Fill the sampled footprint so zoomed-out discovery has no checkerboard holes.
            for(int wz=z;wz<Math.max(z+1,endZ);wz++)for(int wx=x;wx<Math.max(x+1,endX);wx++)MinimapExploration.discover(pixels,wx,wz,argb);
        }
        if(row>=128)row=0;
    }
    private static void upload(MinecraftClient client,MapViewport view,boolean full,boolean circle) {
        int pixels=full?256:128;
        if(full&&fullTexture==null){fullImage=new NativeImage(pixels,pixels,true);fullTexture=new NativeImageBackedTexture(()->"axial/full_map",fullImage);client.getTextureManager().registerTexture(FULL,fullTexture);}
        if(!full&&hudTexture==null){hudImage=new NativeImage(pixels,pixels,true);hudTexture=new NativeImageBackedTexture(()->"axial/minimap",hudImage);client.getTextureManager().registerTexture(HUD,hudTexture);}
        long stamp=MinimapExploration.revision()*31+MinimapServerOverview.revision();
        long now=System.currentTimeMillis();
        boolean unchanged=full?view.centerX==fullX&&view.centerZ==fullZ&&view.scale==fullScale&&stamp==fullStamp:
                view.centerX==hudX&&view.centerZ==hudZ&&view.scale==hudScale&&circle==hudCircular&&stamp==hudStamp;
        if(unchanged)return;
        if(full?now-lastFullUpload<100:now-lastHudUpload<50)return;
        NativeImage target=full?fullImage:hudImage;
        Map<String,Integer> terrain=MinimapExploration.terrainFor(client);
        double span=view.scale*256;
        for(int z=0;z<pixels;z++)for(int x=0;x<pixels;x++) {
            if(circle&&Math.hypot(x+.5-pixels/2.0,z+.5-pixels/2.0)>pixels/2.0-1){target.setColorArgb(x,z,0);continue;}
            int wx=(int)Math.floor(view.centerX+(x/(double)pixels-.5)*span),wz=(int)Math.floor(view.centerZ+(z/(double)pixels-.5)*span);
            int color=MinimapServerOverview.colorAt(wx,wz);
            if(color==0)color=terrain.getOrDefault(wx+","+wz,UNKNOWN);
            target.setColorArgb(x,z,color);
        }
        if(full){fullTexture.upload();fullX=view.centerX;fullZ=view.centerZ;fullScale=view.scale;fullStamp=stamp;lastFullUpload=now;}
        else {hudTexture.upload();hudX=view.centerX;hudZ=view.centerZ;hudScale=view.scale;hudStamp=stamp;hudCircular=circle;lastHudUpload=now;}
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
