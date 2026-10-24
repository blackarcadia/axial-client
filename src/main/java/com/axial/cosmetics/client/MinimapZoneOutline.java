package com.axial.cosmetics.client;

/** Clip four real zone edges to a square or circular HUD without scanning pixels. */
final class MinimapZoneOutline {
    @FunctionalInterface interface Fill {void draw(int x1,int y1,int x2,int y2);}
    private MinimapZoneOutline() { }
    static void draw(int left,int top,int size,boolean circle,double x1,double y1,double x2,double y2,Fill fill) {
        if(size<=0||x2<=x1||y2<=y1)return;
        int firstX=(int)Math.floor(x1),lastX=(int)Math.ceil(x2)-1;
        int firstY=(int)Math.floor(y1),lastY=(int)Math.ceil(y2)-1;
        vertical(left,top,size,circle,firstX,firstY,lastY,fill);
        if(lastX!=firstX)vertical(left,top,size,circle,lastX,firstY,lastY,fill);
        horizontal(left,top,size,circle,firstY,firstX,lastX,fill);
        if(lastY!=firstY)horizontal(left,top,size,circle,lastY,firstX,lastX,fill);
    }
    private static void vertical(int left,int top,int size,boolean circle,int x,int first,int last,Fill fill) {
        if(x<left||x>=left+size)return;
        first=Math.max(first,top);last=Math.min(last,top+size-1);
        if(circle) {
            double radius=size/2.0-1,dx=x+.5-left-size/2.0;
            if(Math.abs(dx)>radius)return;
            double half=Math.sqrt(Math.max(0,radius*radius-dx*dx)),center=top+size/2.0;
            first=Math.max(first,(int)Math.ceil(center-half-.5));last=Math.min(last,(int)Math.floor(center+half-.5));
        }
        if(first<=last)fill.draw(x,first,x+1,last+1);
    }
    private static void horizontal(int left,int top,int size,boolean circle,int y,int first,int last,Fill fill) {
        if(y<top||y>=top+size)return;
        first=Math.max(first,left);last=Math.min(last,left+size-1);
        if(circle) {
            double radius=size/2.0-1,dy=y+.5-top-size/2.0;
            if(Math.abs(dy)>radius)return;
            double half=Math.sqrt(Math.max(0,radius*radius-dy*dy)),center=left+size/2.0;
            first=Math.max(first,(int)Math.ceil(center-half-.5));last=Math.min(last,(int)Math.floor(center+half-.5));
        }
        if(first<=last)fill.draw(first,y,last+1,y+1);
    }
}
