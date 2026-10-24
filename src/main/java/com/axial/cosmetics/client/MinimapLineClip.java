package com.axial.cosmetics.client;

/** Constant-time segment clipping for accurate diagonal zone borders on either map shape. */
final class MinimapLineClip {
    record Line(double x1,double y1,double x2,double y2) { }
    private MinimapLineClip() { }
    static Line clip(double x1,double y1,double x2,double y2,int left,int top,int size,boolean circle) {
        double dx=x2-x1,dy=y2-y1,first=0,last=1;
        if(size<=0||!Double.isFinite(dx)||!Double.isFinite(dy)||dx*dx+dy*dy<1e-12)return null;
        if(dx==0){if(x1<left||x1>left+size)return null;}
        else {double a=(left-x1)/dx,b=(left+size-x1)/dx;first=Math.max(first,Math.min(a,b));last=Math.min(last,Math.max(a,b));}
        if(dy==0){if(y1<top||y1>top+size)return null;}
        else {double a=(top-y1)/dy,b=(top+size-y1)/dy;first=Math.max(first,Math.min(a,b));last=Math.min(last,Math.max(a,b));}
        if(first>last)return null;
        double ax=x1+dx*first,ay=y1+dy*first,bx=x1+dx*last,by=y1+dy*last;
        if(!circle)return new Line(ax,ay,bx,by);
        dx=bx-ax;dy=by-ay;double radius=Math.max(0,size/2.0-3),cx=ax-left-size/2.0,cy=ay-top-size/2.0;
        double a=dx*dx+dy*dy,b=2*(cx*dx+cy*dy),c=cx*cx+cy*cy-radius*radius;
        if(a<1e-12)return c<=0?new Line(ax,ay,bx,by):null;
        double discriminant=b*b-4*a*c;if(discriminant<0)return null;
        double root=Math.sqrt(discriminant);first=Math.max(0,(-b-root)/(2*a));last=Math.min(1,(-b+root)/(2*a));
        return first>last?null:new Line(ax+dx*first,ay+dy*first,ax+dx*last,ay+dy*last);
    }
}
