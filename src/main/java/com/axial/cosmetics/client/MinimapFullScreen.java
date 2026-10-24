package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.math.BlockPos;
import java.util.*;
import com.axial.cosmetics.client.ModMenuLayout.Rect;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** A unified world map and waypoint library, with cursor-anchored zoom. */
public class MinimapFullScreen extends NavigationScreen {
    private NavigationLayout layout;
    private MapViewport view;
    private TextFieldWidget search;
    private String query="",selectedId,armedDelete,contextKey="",fittedWorld="";
    private long worldGeneration=-1;
    private int scroll;
    private boolean dragging,dimensionOnly=true;
    private NavButton edit,delete,visibility,dimensions,wholeWorld;
    private List<WaypointConfig.Entry> entries=List.of();
    public MinimapFullScreen(){this(null);}
    public MinimapFullScreen(Screen parent){super("Map & Waypoints",parent);}
    @Override protected void init() {
        clearChildren();layout=new NavigationLayout(width,height);
        if(view==null)view=new MapViewport(client.player==null?0:client.player.getX(),client.player==null?0:client.player.getZ(),MinimapConfig.zoom());
        int x=layout.panel.x(),y=layout.panel.y(),w=layout.panel.width();
        search=field("Search waypoints...",query,layout.search,48);search.setChangedListener(v->{query=v;scroll=0;refresh();});
        var newWaypoint=button("+ New",new Rect(x+w-228,y+13,70,24),()->client.setScreen(new CreateWaypointScreen(this,client.player==null?BlockPos.ORIGIN:client.player.getBlockPos())));newWaypoint.primary=true;newWaypoint.active=client.world!=null;
        button("Minimap",new Rect(x+w-152,y+13,92,24),()->client.setScreen(new MinimapSettingsScreen(this)));
        button("X",new Rect(x+w-50,y+13,32,24),this::close);
        int foot=layout.panel.bottom()-28;
        edit=button("Edit",new Rect(x+10,foot,48,20),()->{var e=selected();if(e!=null)client.setScreen(new CreateWaypointScreen(this,e));});
        delete=button("Delete",new Rect(x+63,foot,58,20),this::delete);delete.danger=true;
        visibility=button("Markers",new Rect(x+126,foot,62,20),()->WaypointConfig.setEnabled(!WaypointConfig.enabled()));visibility.toggle=true;
        button("Recenter",new Rect(layout.panel.right()-84,foot,74,20),()->{if(client.player!=null){view.centerX=client.player.getX();view.centerZ=client.player.getZ();}});
        dimensions=button("This dimension",new Rect(layout.sidebar.x(),y+40,layout.sidebar.width(),17),()->{dimensionOnly=!dimensionOnly;scroll=0;refresh();});
        wholeWorld=button("Whole World",new Rect(layout.map.x(),y+40,Math.min(90,layout.map.width()),17),this::fitWorld);
        refresh();
    }
    private void fitWorld() {
        var bounds=MinimapServerOverview.fullWorldBounds();
        if(bounds!=null){view.centerX=bounds.centerX();view.centerZ=bounds.centerZ();view.scale=Math.clamp(bounds.scale()*1.03,MapViewport.MIN_SCALE,MapViewport.MAX_SCALE);fittedWorld=bounds.world();}
    }
    @Override public void tick() {
        String key=WaypointConfig.worldId(client)+"|"+(client.world==null?"":client.world.getRegistryKey().getValue());
        long generation=MinimapServerOverview.worldGeneration();
        if(!key.equals(contextKey)||generation!=worldGeneration) {
            contextKey=key;worldGeneration=generation;selectedId=armedDelete=null;fittedWorld="";dragging=false;
            view=new MapViewport(client.player==null?0:client.player.getX(),client.player==null?0:client.player.getZ(),MinimapConfig.zoom());
            refresh();
        }
        var bounds=MinimapServerOverview.fullWorldBounds();if(bounds!=null&&!bounds.world().equals(fittedWorld))fitWorld();
    }
    private void refresh() {
        String dim=client.world==null?"":client.world.getRegistryKey().getValue().toString();
        entries=WaypointConfig.waypointsFor(client).stream().filter(e->!dimensionOnly||e.dimension().equals(dim))
                .filter(e->e.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparingDouble(e->client.player==null?0:Math.hypot(e.x()-client.player.getX(),e.z()-client.player.getZ()))).toList();
        scroll=Math.clamp(scroll,0,layout.maxScroll(entries.size()));
    }
    private WaypointConfig.Entry selected(){return WaypointConfig.waypointsFor(client).stream().filter(e->e.id().equals(selectedId)).findFirst().orElse(null);}
    private void delete() {
        var e=selected();if(e==null)return;
        if(!e.id().equals(armedDelete)){armedDelete=e.id();error="Press Delete again to remove "+e.name();return;}
        WaypointConfig.delete(e);selectedId=armedDelete=null;refresh();
    }
    @Override public void render(DrawContext c,int mx,int my,float dt) {
        frame(c,layout.panel,PANEL_BORDER,PANEL);
        int headingWidth=layout.panel.width()-260;
        line(c,"AXIAL CLIENT",layout.panel.x()+16,layout.panel.y()+10,BRAND,.8f,headingWidth,true);
        line(c,"Map & Waypoints",layout.panel.x()+16,layout.panel.y()+24,INK,1.35f,headingWidth,true);
        inputFrame(c,search,layout.search);
        frame(c,layout.map,LINE,FIELD);
        if(client.world!=null&&client.player!=null)MinimapRenderer.renderFullMap(c,client,layout.map.x(),layout.map.y(),layout.map.width(),view);
        else line(c,"Join a world to explore",layout.map.x()+8,layout.map.y()+12,MUTED,.85f,layout.map.width()-16,false);
        c.enableScissor(layout.list.x(),layout.list.y(),layout.list.right(),layout.list.bottom());
        for(int i=0;i<entries.size();i++) {
            Rect r=layout.row(i,scroll);if(r.bottom()<=layout.list.y()||r.y()>=layout.list.bottom())continue;
            var e=entries.get(i);boolean selected=e.id().equals(selectedId);
            frame(c,r,selected?BRAND:LINE,selected?SELECTED:r.contains(mx,my)?HOVER:SURFACE);
            circle(c,r.x()+7,r.y()+8,7,e.hidden()?MUTED:e.color());
            boolean showDistance=r.width()>=160&&client.player!=null&&client.world!=null&&e.dimension().equals(client.world.getRegistryKey().getValue().toString());
            line(c,e.name(),r.x()+20,r.y()+7,e.hidden()?MUTED:INK,.9f,r.width()-(showDistance?86:46),true);
            if(showDistance){double distance=Math.hypot(e.x()-client.player.getX(),e.z()-client.player.getZ());String d=distance>=1000?String.format(Locale.ROOT,"%.1fkm",distance/1000):Math.round(distance)+"m";line(c,d,r.right()-65,r.y()+8,MUTED,.75f,39,false);}
            String coords=(dimensionOnly?"":e.dimension().replace("minecraft:","")+"  ")+e.x()+", "+e.y()+", "+e.z();
            line(c,coords,r.x()+9,r.y()+23,MUTED,.75f,r.width()-37,false);
            icon(c,"eye",r.right()-22,r.y()+12,12,12,e.hidden()?MUTED:BRAND);
        }
        if(entries.isEmpty())line(c,query.isBlank()?"No saved locations yet":"No matching waypoints",layout.list.x()+8,layout.list.y()+16,MUTED,.8f,layout.list.width()-16,false);
        c.disableScissor();
        int max=layout.maxScroll(entries.size());if(max>0){int thumb=Math.max(12,layout.list.height()*layout.list.height()/Math.max(1,entries.size()*43));rounded(c,layout.list.right()-3,layout.list.y()+(layout.list.height()-thumb)*scroll/max,3,thumb,MUTED);}
        wholeWorld.active=MinimapServerOverview.fullWorldBounds()!=null;
        edit.active=delete.active=selected()!=null;
        delete.label(armedDelete!=null?"Confirm":"Delete");
        visibility.selected=WaypointConfig.enabled();dimensions.label(dimensionOnly?"This dimension":"All dimensions");
        for(var child:children())if(child instanceof net.minecraft.client.gui.Drawable d)d.render(c,mx,my,dt);
        int infoX=layout.map.x();int infoWidth=layout.map.width();
        line(c,error.isEmpty()?"Scroll: zoom   Drag: pan   Right click: waypoint":error,infoX,layout.panel.bottom()-43,error.isEmpty()?MUTED:RED,.75f,infoWidth,false);
        if(layout.map.contains(mx,my)) {
            String coordinates="X "+(int)Math.floor(view.worldX(mx,layout.map.x(),layout.map.width()))+"  Z "+(int)Math.floor(view.worldZ(my,layout.map.y(),layout.map.width()));
            line(c,coordinates,layout.map.x()+7,layout.map.bottom()-14,INK,.8f,layout.map.width()-14,false);
        }
        line(c,"N  /  "+String.format(Locale.ROOT,"%.0f blocks across",view.scale*256),layout.map.x()+6,layout.map.y()+7,BRAND,.75f,layout.map.width()-12,true);
        String loading=MinimapServerOverview.loadingMessage();
        if(!loading.isEmpty()) {
            rounded(c,layout.map.x()+5,layout.map.y()+19,layout.map.width()-10,15,PANEL);
            line(c,loading,layout.map.x()+9,layout.map.y()+24,MUTED,.7f,layout.map.width()-18,false);
        }
        if(client.world!=null&&client.player!=null)MinimapRenderer.renderZoneTooltip(c,client,view,
                layout.map.x(),layout.map.y(),layout.map.width(),false,mx,my);
    }
    @Override public boolean mouseClicked(Click click,boolean doubled) {
        if(layout.list.contains(click.x(),click.y())&&click.button()==0)for(int i=0;i<entries.size();i++) {
            Rect r=layout.row(i,scroll);if(!r.contains(click.x(),click.y()))continue;
            var e=entries.get(i);armedDelete=null;error="";
            if(click.x()>=r.right()-27){attempt(()->WaypointConfig.setHidden(e,!e.hidden()));refresh();return true;}
            selectedId=e.id();
            if(doubled){client.setScreen(new CreateWaypointScreen(this,e));return true;}
            if(client.world!=null&&e.dimension().equals(client.world.getRegistryKey().getValue().toString())){view.centerX=e.x();view.centerZ=e.z();}
            else error="This waypoint is in "+e.dimension().replace("minecraft:","");
            return true;
        }
        if(layout.map.contains(click.x(),click.y())) {
            if(click.button()==1&&client.player!=null&&client.world!=null) {
                int x=(int)Math.floor(view.worldX(click.x(),layout.map.x(),layout.map.width())),z=(int)Math.floor(view.worldZ(click.y(),layout.map.y(),layout.map.width()));
                client.setScreen(new CreateWaypointScreen(this,new BlockPos(x,client.player.getBlockY(),z)));return true;
            }
            if(click.button()==0){dragging=true;return true;}
        }
        return super.mouseClicked(click,doubled);
    }
    @Override public boolean mouseDragged(Click click,double dx,double dy){if(dragging&&click.button()==0){view.pan(dx,dy,layout.map.width());return true;}return super.mouseDragged(click,dx,dy);}
    @Override public boolean mouseReleased(Click click){dragging=false;return super.mouseReleased(click);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(layout.list.contains(x,y)){scroll=Math.clamp(scroll-(int)Math.round(vertical*30),0,layout.maxScroll(entries.size()));return true;}
        if(layout.map.contains(x,y)){view.zoom(vertical,x,y,layout.map.x(),layout.map.y(),layout.map.width());return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean keyPressed(KeyInput key) {
        if(!search.isFocused()) {
            if(AxialCosmetics.minimapMapKey().matchesKey(key)){close();return true;}
            if(AxialCosmetics.createWaypointKey().matchesKey(key)&&client.player!=null){client.setScreen(new CreateWaypointScreen(this,client.player.getBlockPos()));return true;}
            if(key.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_UP||key.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN) {
                if(!entries.isEmpty()) {
                    int current=-1;for(int i=0;i<entries.size();i++)if(entries.get(i).id().equals(selectedId)){current=i;break;}
                    int next=Math.clamp(current+(key.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN?1:-1),0,entries.size()-1);
                    var e=entries.get(next);selectedId=e.id();armedDelete=null;
                    scroll=Math.clamp(next*43,0,layout.maxScroll(entries.size()));
                    if(client.world!=null&&e.dimension().equals(client.world.getRegistryKey().getValue().toString())){view.centerX=e.x();view.centerZ=e.z();}
                }
                return true;
            }
            if(key.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER&&selected()!=null){client.setScreen(new CreateWaypointScreen(this,selected()));return true;}
            if(key.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE&&selected()!=null){attempt(this::delete);return true;}
        }
        return super.keyPressed(key);
    }
    @Override public boolean shouldPause(){return false;}
}
