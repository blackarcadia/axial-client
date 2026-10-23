package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import com.axial.cosmetics.client.ModMenuLayout.Rect;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** Appearance controls with a live preview and quick access to the map library. */
public final class MinimapSettingsScreen extends NavigationScreen {
    private Rect panel,preview;
    private boolean capturing,createKey;
    private int scroll;
    private Rect controls;
    private final java.util.List<NavButton> rows=new java.util.ArrayList<>();
    private NavButton enabled,shape,size,zoom,coordinates,names,death,key,create;
    public MinimapSettingsScreen(Screen parent){super("Minimap",parent);}
    @Override protected void init() {
        clearChildren();rows.clear();int w=Math.min(492,width-20),h=Math.min(344,height-20);panel=new Rect((width-w)/2,(height-h)/2,w,h);
        boolean side=w>=420;int controlWidth=side?254:w-32;
        int row=26,x=panel.x()+16,y=panel.y()+(h<200?48:58);
        controls=new Rect(x,y,controlWidth,panel.bottom()-41-y);
        enabled=button("",new Rect(x,y,controlWidth,row-2),MinimapConfig::toggle);
        shape=button("",new Rect(x,y+row,controlWidth,row-2),MinimapConfig::cycleStyle);
        size=button("",new Rect(x,y+row*2,controlWidth,row-2),MinimapConfig::cycleSize);
        zoom=button("",new Rect(x,y+row*3,controlWidth,row-2),MinimapConfig::cycleZoom);
        coordinates=button("",new Rect(x,y+row*4,controlWidth,row-2),()->MinimapConfig.setCoordinates(!MinimapConfig.showCoordinates()));
        names=button("",new Rect(x,y+row*5,controlWidth,row-2),()->MinimapConfig.setWaypointNames(!MinimapConfig.showWaypointNames()));
        death=button("",new Rect(x,y+row*6,controlWidth,row-2),()->MinimapConfig.setDeath(!MinimapConfig.showDeath()));
        key=button("",new Rect(x,y+row*7,controlWidth,row-2),()->{capturing=true;createKey=false;});
        create=button("",new Rect(x,y+row*8,controlWidth,row-2),()->{capturing=true;createKey=true;});
        rows.addAll(java.util.List.of(enabled,shape,size,zoom,coordinates,names,death,key,create));
        positionRows();
        preview=side?new Rect(x+controlWidth+12,y,w-controlWidth-44,controls.height()):null;
        button("Map & Waypoints",new Rect(x,panel.bottom()-33,128,23),()->{if(parent instanceof MinimapFullScreen)close();else client.setScreen(new MinimapFullScreen(this));});
        button("Done",new Rect(panel.right()-82,panel.bottom()-33,66,23),this::close).primary=true;
    }
    @Override public void render(DrawContext c,int mx,int my,float dt) {
        heading(c,panel,"Minimap",panel.height()<200?"":"Your surroundings, at a glance");
        enabled.label("Minimap: "+(MinimapConfig.isEnabled()?"On":"Off"));enabled.selected=MinimapConfig.isEnabled();
        shape.label("Shape: "+(MinimapConfig.isCircular()?"Circle":"Square"));
        size.label("Size: "+MinimapConfig.sizeLabel());zoom.label("Map scale: "+MinimapConfig.zoomLabel());
        coordinates.label("Coordinates: "+(MinimapConfig.showCoordinates()?"On":"Off"));
        names.label("Waypoint names: "+(MinimapConfig.showWaypointNames()?"On":"Off"));
        death.label("Death marker: "+(MinimapConfig.showDeath()?"On":"Off"));
        key.label(capturing&&!createKey?"Press a key (Esc to cancel)":"Open map: "+AxialCosmetics.minimapMapKey().getBoundKeyLocalizedText().getString());key.selected=capturing&&!createKey;
        create.label(capturing&&createKey?"Press a key (Esc to cancel)":"New waypoint: "+AxialCosmetics.createWaypointKey().getBoundKeyLocalizedText().getString());create.selected=capturing&&createKey;
        if(preview!=null){frame(c,preview,LINE,SURFACE);MinimapRenderer.renderSettingsPreview(c,client,preview.x(),preview.y(),preview.width(),preview.height());}
        for(var child:children())if(child instanceof NavButton b&&b.visible)b.render(c,mx,my,dt);
        int max=Math.max(0,rows.size()*26-controls.height());
        if(max>0){int thumb=Math.max(10,controls.height()*controls.height()/(rows.size()*26));rounded(c,controls.right()+3,controls.y()+(controls.height()-thumb)*scroll/max,2,thumb,MUTED);}
        if(!error.isEmpty())line(c,error,panel.x()+16,panel.bottom()-44,RED,.75f,panel.width()-32,false);
    }
    private void positionRows() {
        scroll=Math.clamp(scroll,0,Math.max(0,rows.size()*26-controls.height()));
        for(int i=0;i<rows.size();i++){var b=rows.get(i);b.setY(controls.y()+i*26-scroll);b.visible=b.getY()>=controls.y()&&b.getBottom()<=controls.bottom();if(!b.visible&&b.isFocused())setFocused(null);}
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(controls.contains(x,y)){scroll-=(int)Math.round(vertical*26);positionRows();return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean keyPressed(KeyInput input) {
        if(!capturing)return super.keyPressed(input);
        capturing=false;
        if(input.key()!=org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE){
            (createKey?AxialCosmetics.createWaypointKey():AxialCosmetics.minimapMapKey()).setBoundKey(InputUtil.fromKeyCode(input));KeyBinding.updateKeysByCode();client.options.write();
        }
        return true;
    }
}
