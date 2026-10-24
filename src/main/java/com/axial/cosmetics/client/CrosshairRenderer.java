package com.axial.cosmetics.client;

import net.minecraft.client.gui.DrawContext;
import java.util.*;

public final class CrosshairRenderer {
    private static final Map<CrosshairGeometry.Key,CrosshairGeometry.Shape> CACHE=new LinkedHashMap<>(64,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<CrosshairGeometry.Key,CrosshairGeometry.Shape> entry){return size()>64;}
    };
    private CrosshairRenderer(){}
    public static void render(DrawContext c,int x,int y,CrosshairConfig config){render(c,x,y,config,1);}
    public static void render(DrawContext c,int x,int y,CrosshairConfig config,float pulse){
        if(config==null||!config.enabled)return;
        var key=CrosshairGeometry.Key.of(config,pulse);
        var shape=CACHE.computeIfAbsent(key,CrosshairGeometry::build);
        int alpha=Math.round(255*Math.clamp(config.opacity,.1f,1));
        int outline=(alpha<<24)|(config.outlineColor&0xFFFFFF),ink=(alpha<<24)|(config.color&0xFFFFFF);
        for(var r:shape.outline())c.fill(x+r.x(),y+r.y(),x+r.x()+r.width(),y+r.y()+r.height(),outline);
        for(var r:shape.ink())c.fill(x+r.x(),y+r.y(),x+r.x()+r.width(),y+r.y()+r.height(),ink);
    }
}
