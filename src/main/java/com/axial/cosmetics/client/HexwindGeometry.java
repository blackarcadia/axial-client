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
        var mesh=new ArrayList<Quad>();int segments=detailed?22:12;
        for(int band=0;band<3;band++) {
            double start=phase*(band==1?-1:1)+band*2.15;
            double radius=.78+band*.055,base=.32+band*.59;
            for(int segment=0;segment<segments;segment++) {
                double t=segment/(double)segments,next=(segment+1.0)/segments;
                double a=start+t*Math.PI*1.52,b=start+next*Math.PI*1.52;
                double h1=base+Math.sin(a*2+band)*.095,h2=base+Math.sin(b*2+band)*.095;
                double width1=.018+.17*Math.sin(t*Math.PI)*Math.sin(t*Math.PI),width2=.018+.17*Math.sin(next*Math.PI)*Math.sin(next*Math.PI);
                int alpha=(int)(190+50*Math.sin(t*Math.PI));
                mesh.add(new Quad(orbit(a,radius,h1-width1/2),orbit(b,radius,h2-width2/2),orbit(b,radius,h2+width2/2),orbit(a,radius,h1+width1/2),(alpha<<24)|0x171020));
                // Raised strips follow the smoke's edge, not a full solid neon ring.
                mesh.add(new Quad(orbit(a,radius+.006,h1+width1*.27),orbit(b,radius+.006,h2+width2*.27),orbit(b,radius+.006,h2+width2*.5),orbit(a,radius+.006,h1+width1*.5),0xB09F52D7));
                if(segment>segments*2/3)mesh.add(new Quad(orbit(a,radius+.01,h1-width1*.5),orbit(b,radius+.01,h2-width2*.5),orbit(b,radius+.01,h2-width2*.32),orbit(a,radius+.01,h1-width1*.32),0xD0F99235));
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
            Point center=orbit(angle,.9+(i%3)*.045,height);double size=i%3==0?.035:.018;
            square(mesh,center,size,i%3==0?0xEFFFAB48:0xBDAE72E2);
        }
        return List.copyOf(mesh);
    }
    private static void pumpkin(List<Quad> mesh,double angle,double radius,double y) {
        Point center=orbit(angle,radius,y);double axis=angle+Math.PI/2,r=.095;
        cube(mesh,center,axis,r,r,r,0xFFE17B25);
        cube(mesh,local(center,axis,0,r+.024,0),axis,.018,.025,.018,0xFF77813F);
        for(int side:new int[]{-1,1}) {
            Point eye=local(center,axis,side*.037,.025,-r-.002);
            face(mesh,eye,axis,.018,.018,0xFF351822);
        }
        face(mesh,local(center,axis,0,-.025,-r-.003),axis,.055,.012,0xFF351822);
        face(mesh,local(center,axis,0,-.044,-r-.003),axis,.028,.01,0xFF351822);
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
        Point center=orbit(angle,radius,y);double tangent=angle+Math.PI/2;
        double wing=.16+Math.cos(flap)*.035;
        // Bat wings are angular panels with notched lower edges and a gently changing pitch.
        Point bodyLow=local(center,tangent,-.025,-.05,0),bodyHigh=local(center,tangent,.025,.05,0);
        mesh.add(new Quad(bodyLow,local(center,tangent,.025,-.05,0),bodyHigh,local(center,tangent,-.025,.05,0),0xFF25172D));
        for(int side:new int[]{-1,1}) {
            Point root=local(center,tangent,side*.025,.025,0),tip=local(center,tangent,side*wing,.065+flap*.045,Math.sin(flap)*.09);
            Point notch=local(center,tangent,side*.105,-.012,Math.sin(flap)*.045),lower=local(center,tangent,side*.04,-.035,0);
            mesh.add(new Quad(root,tip,notch,lower,0xF535203F));
            mesh.add(new Quad(root,tip,local(center,tangent,side*(wing-.025),.058+flap*.045,Math.sin(flap)*.09),local(center,tangent,side*.04,.018,0),0xE9AF5FE3));
        }
        for(int side:new int[]{-1,1}) {
            Point eye=local(center,tangent,side*.012,.015,-.004);
            mesh.add(new Quad(local(eye,tangent,-.006,-.006,0),local(eye,tangent,.006,-.006,0),local(eye,tangent,.006,.006,0),local(eye,tangent,-.006,.006,0),0xFFFFA144));
        }
    }
    private static Point local(Point center,double angle,double x,double y,double z) {
        return point(center.x+Math.cos(angle)*x-Math.sin(angle)*z,center.y+y,center.z+Math.sin(angle)*x+Math.cos(angle)*z);
    }
}
