package com.axial.cosmetics.client;

import net.minecraft.client.gui.DrawContext;
import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** Shared panel geometry across menu screens; inputs use the same inverse transform as rendering. */
final class PanelResize {
    private static final PanelBoundsMotion motion=new PanelBoundsMotion(System::nanoTime);
    private static Object owner;
    private static Rect displayed;
    private static int viewportWidth,viewportHeight;
    private static long lastDraw;
    private PanelResize(){}
    static void reset(){owner=null;displayed=null;}
    static void push(DrawContext c,Object screen,Rect target,int width,int height){
        long now=System.nanoTime();
        if(owner==null||width!=viewportWidth||height!=viewportHeight||now-lastDraw>1_000_000_000L)motion.jump(target);
        else motion.redirect(target);
        owner=screen;viewportWidth=width;viewportHeight=height;lastDraw=now;displayed=motion.value();
        c.getMatrices().pushMatrix();
        c.getMatrices().translate(displayed.x(),displayed.y());
        c.getMatrices().scale(displayed.width()/(float)target.width(),displayed.height()/(float)target.height());
        c.getMatrices().translate(-target.x(),-target.y());
    }
    static double x(Object screen,Rect target,double x){return screen!=owner||displayed==null?x:PanelBoundsMotion.inverseX(displayed,target,x);}
    static double y(Object screen,Rect target,double y){return screen!=owner||displayed==null?y:PanelBoundsMotion.inverseY(displayed,target,y);}
}
