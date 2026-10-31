package com.axial.cosmetics.client;

import java.util.ArrayList;
import java.util.List;

/** Original Halloween wind geometry, shared by the live renderer and generated preview assets. */
public final class HexwindGeometry {
    public static final float PERIOD=120;
    public record Point(float x,float y,float z) { }
    public record Quad(Point a,Point b,Point c,Point d,int color) { }
    private HexwindGeometry() { }
    private static Point point(double x,double y,double z){return new Point((float)x,(float)y,(float)z);}
    private static Point orbit(double angle,double radius,double height){return point(Math.cos(angle)*radius,height,Math.sin(angle)*radius);}
    public static List<Quad> frame(float ticks,boolean detailed) {
        double phase=Math.floorMod((long)Math.floor(ticks*1000),120000)/120000.0*Math.PI*2;
        var mesh=new ArrayList<Quad>();int segments=detailed?40:20;
        for(int band=0;band<3;band++) {
            double start=phase*(band==1?-1:1)+band*2.15;
            double radius=.78+band*.055,base=.32+band*.59;
            // A one-texel grid gives the smoke a stepped silhouette, even at close range.
            // Split diagonal movement into axis-aligned treads instead of smooth ribbon panels.
            for(int segment=0;segment<segments;segment++) {
                double t=segment/(double)segments,next=(segment+1.0)/segments;
                double a=start+t*Math.PI*1.52,b=start+next*Math.PI*1.52;
                double x1=snap(Math.cos(a)*radius),z1=snap(Math.sin(a)*radius);
                double x2=snap(Math.cos(b)*radius),z2=snap(Math.sin(b)*radius);
                double bottom=snap(base+Math.sin(a*2+band)*.095)-PIXEL;
                int rows=1+(int)Math.round(2*Math.sin(t*Math.PI));
                int color=segment%4==0?0xE936253F:0xEA21182C;
                tread(mesh,x1,z1,x2,z1,bottom,rows,color,segment,band);
                tread(mesh,x2,z1,x2,z2,bottom,rows,color,segment,band);
            }
        }
        for(int bat=0;bat<3;bat++) {
            double angle=phase+bat*Math.PI*2/3,flap=Math.sin(phase*6+bat)*.75;
            double radius=.98,height=.64+bat*.38+Math.sin(phase*2+bat)*.12;
            bat(mesh,angle,radius,height,flap);
        }
        pumpkin(mesh,phase+.8,.99,.78+Math.sin(phase*2)*.1);
        int motes=detailed?12:6;
        for(int i=0;i<motes;i++) {
            double angle=phase*(i%2==0?1:-1)+i*2.39996;
            double height=.14+(i%6)*.31+Math.sin(phase*2+i)*.06;
            Point center=orbit(angle,.9+(i%3)*.045,height);double size=i%3==0?PIXEL/2:PIXEL/4;
            square(mesh,center,size,i%3==0?0xEFFFAB48:0xBDAE72E2);
        }
        return List.copyOf(mesh);
    }
    private static final double PIXEL=1.0/16;
    private static double snap(double value){return Math.rint(value/PIXEL)*PIXEL;}
    private static void tread(List<Quad> mesh,double x1,double z1,double x2,double z2,double y,int rows,int color,int segment,int band) {
        if(x1==x2&&z1==z2)return;
        mesh.add(new Quad(point(x1,y,z1),point(x2,y,z2),point(x2,y+rows*PIXEL,z2),point(x1,y+rows*PIXEL,z1),color));
        // Whole square texels of colour, rather than thin, smooth neon outlines.
        if(segment%3!=0) {
            double dx=x2-x1,dz=z2-z1,length=Math.abs(dx)+Math.abs(dz);
            double ex=x1+dx/length*Math.min(PIXEL,length),ez=z1+dz/length*Math.min(PIXEL,length);
            int accent=(segment+band)%5==0?0xFFF2A13D:0xD58F50B6;
            double top=y+rows*PIXEL;
            // A tiny outward offset prevents coplanar colour pixels from flickering.
            double ox=z1==z2?0:Math.copySign(.001,x1),oz=x1==x2?0:Math.copySign(.001,z1);
            mesh.add(new Quad(point(x1+ox,top-PIXEL,z1+oz),point(ex+ox,top-PIXEL,ez+oz),point(ex+ox,top,ez+oz),point(x1+ox,top,z1+oz),accent));
        }
    }
    private static void pumpkin(List<Quad> mesh,double angle,double radius,double y) {
        Point center=orbit(angle,radius,y);double axis=angle+Math.PI/2,p=PIXEL/2,r=3*p;
        cube(mesh,center,axis,r,r,r,0xFFE18A29);
        cube(mesh,local(center,axis,0,r+p,0),axis,p,p,p,0xFF6B8237);
        // Six-pixel face with blocky ribs, square eyes and a toothy mouth.
        for(int column:new int[]{-2,1})face(mesh,local(center,axis,(column+.5)*p,0,-r-.001),axis,p/2,r,0xFFBC5E20);
        for(int side:new int[]{-1,1})face(mesh,local(center,axis,side*1.5*p,.5*p,-r-.003),axis,p/2,p/2,0xFF38202A);
        for(int column=-2;column<=1;column++) {
            double row=column==-2||column==1?-1:-2;
            face(mesh,local(center,axis,(column+.5)*p,row*p,-r-.003),axis,p/2,p/2,0xFF38202A);
        }
    }
    private static void face(List<Quad> mesh,Point c,double axis,double w,double h,int color) {
        mesh.add(new Quad(local(c,axis,-w,-h,0),local(c,axis,w,-h,0),local(c,axis,w,h,0),local(c,axis,-w,h,0),color));
    }
    private static void cube(List<Quad> mesh,Point c,double axis,double w,double h,double d,int color) {
        Point a=local(c,axis,-w,-h,-d),b=local(c,axis,w,-h,-d),cc=local(c,axis,w,h,-d),dd=local(c,axis,-w,h,-d);
        Point e=local(c,axis,-w,-h,d),f=local(c,axis,w,-h,d),g=local(c,axis,w,h,d),j=local(c,axis,-w,h,d);
        mesh.add(new Quad(a,b,cc,dd,color));mesh.add(new Quad(e,j,g,f,color));
        mesh.add(new Quad(a,dd,j,e,color));mesh.add(new Quad(b,f,g,cc,color));
        mesh.add(new Quad(dd,cc,g,j,color));mesh.add(new Quad(a,e,f,b,color));
    }
    private static void square(List<Quad> mesh,Point c,double r,int color) {
        mesh.add(new Quad(point(c.x-r,c.y-r,c.z),point(c.x+r,c.y-r,c.z),point(c.x+r,c.y+r,c.z),point(c.x-r,c.y+r,c.z),color));
        mesh.add(new Quad(point(c.x,c.y-r,c.z-r),point(c.x,c.y-r,c.z+r),point(c.x,c.y+r,c.z+r),point(c.x,c.y+r,c.z-r),color));
    }
    private static void bat(List<Quad> mesh,double angle,double radius,double y,double flap) {
        Point center=orbit(angle,radius,y);double tangent=angle+Math.PI/2,p=PIXEL/2;
        // A pixel sprite silhouette: square ears, tiny body and stepped wings.
        cube(mesh,center,tangent,p,1.5*p,p/2,0xFF302038);
        for(int side:new int[]{-1,1}) {
            face(mesh,local(center,tangent,side*.5*p,2*p,-p/2),tangent,p/2,p/2,0xFF302038);
            face(mesh,local(center,tangent,side*.5*p,.5*p,-p/2-.002),tangent,p/4,p/4,0xFFFFAD43);
            for(int column=0;column<4;column++) {
                double x=side*(column+1.5)*p;
                double lift=Math.rint(flap*column)*p;
                int rows=column<2?3:column==2?2:1;
                double top=(column<3?1.5:2.5)*p+lift;
                Point wing=local(center,tangent,x,top-rows*p/2,0);
                face(mesh,wing,tangent,p/2,rows*p/2,0xFF463052);
                face(mesh,local(center,tangent,x,top-p/2,-.001),tangent,p/2,p/2,0xFF9455B5);
            }
        }
    }
    private static Point local(Point center,double angle,double x,double y,double z) {
        return point(center.x+Math.cos(angle)*x-Math.sin(angle)*z,center.y+y,center.z+Math.sin(angle)*x+Math.cos(angle)*z);
    }
}
