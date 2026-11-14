package com.axial.cosmetics.client;

import java.util.*;
import com.axial.cosmetics.client.HexwindGeometry.Point;
import com.axial.cosmetics.client.HexwindGeometry.Quad;

/** Crisp pastel pixel stars. World-space births let a moving player leave a real trail. */
public final class PixelStarsGeometry {
    public static final float PERIOD=120;
    static final int[] COLORS={0xFAFDFF,0xA8DFFF,0xFFB8DF};
    public record Star(double x,double y,double z,double vx,double vy,double vz,double born,double life,float size,int color,float angle) {}
    private PixelStarsGeometry() {}
    public static List<Quad> render(List<Star> stars,double tick,double x,double y,double z,float yaw,boolean detailed){
        var mesh=new ArrayList<Quad>();double a=Math.toRadians(yaw),cos=Math.cos(a),sin=Math.sin(a);
        for(int i=0;i<stars.size();i++){
            if(!detailed&&i%2!=0)continue;
            var star=stars.get(i);double age=tick-star.born();if(age<0||age>=star.life())continue;
            double t=age/star.life(),fade=Math.min(1,age/1.2)*Math.min(1,(1-t)*5);
            double dx=star.x()+star.vx()*age-x,dz=star.z()+star.vz()*age-z;
            double px=cos*dx+sin*dz,pz=sin*dx-cos*dz,py=star.y()+star.vy()*age-y;
            double size=star.size()*1.55*(.93+.07*Math.sin(age*.45+star.angle()))*(1-.10*t);
            int color=((int)(fade*255)<<24)|star.color();
            for(double plane:new double[]{0,Math.PI/2}){
                if(detailed){
                    int[] rows={1,1,3,7,3,1,1};double pixel=size/3.5;
                    for(int row=0;row<rows.length;row++)rect(mesh,px,py,pz,-rows[row]*pixel/2,(row-3.5)*pixel,rows[row]*pixel,pixel,plane,star.angle()+age*.015,color);
                    rect(mesh,px,py,pz,-pixel*.4,-pixel*.4,pixel*.8,pixel*.8,plane,star.angle()+age*.015,((int)(fade*255)<<24)|0xFAFDFF);
                    // Faint separated tip pixels add sparkle without a solid square halo.
                    for(int tip:new int[]{-1,1})rect(mesh,px,py,pz,tip*size*1.15-pixel/2,-pixel/2,pixel,pixel,plane,star.angle()+age*.015,((int)(fade*110)<<24)|star.color());
                }else{
                    rect(mesh,px,py,pz,-size,-size/5,size*2,size*2/5,plane,star.angle(),color);
                    rect(mesh,px,py,pz,-size/5,-size,size*2/5,size*2,plane,star.angle(),color);
                }
            }
        }
        return List.copyOf(mesh);
    }
    private static Point point(double x,double y,double z,double u,double v,double plane,double angle){
        double du=u*Math.cos(angle)-v*Math.sin(angle),dv=u*Math.sin(angle)+v*Math.cos(angle);
        return new Point((float)(x+du*Math.cos(plane)),(float)(y+dv),(float)(z+du*Math.sin(plane)));
    }
    private static void rect(List<Quad> mesh,double x,double y,double z,double u,double v,double w,double h,double plane,double angle,int color){
        mesh.add(new Quad(point(x,y,z,u,v,plane,angle),point(x,y,z,u+w,v,plane,angle),point(x,y,z,u+w,v+h,plane,angle),point(x,y,z,u,v+h,plane,angle),color));
    }
    /** Wardrobe preview simulates movement, without creating live world emitters. */
    public static List<Quad> frame(float ticks,boolean detailed){
        double time=ticks-Math.floor(ticks/PERIOD)*PERIOD;var stars=new ArrayList<Star>();var random=new Random(0x57A25);
        for(int i=0;i<72;i++){
            double birth=i*PERIOD/72.0,life=17+random.nextDouble()*8,age=(time-birth+PERIOD)%PERIOD;
            double angle=random.nextDouble()*Math.PI*2,radius=.28+random.nextDouble()*.12;
            stars.add(new Star(Math.cos(angle)*radius,.15+random.nextDouble()*1.65,Math.sin(angle)*radius+age*.035,
                    Math.cos(angle)*.019,.009+random.nextDouble()*.012,Math.sin(angle)*.019,time-age,life,.10f+random.nextFloat()*.045f,COLORS[i%3],random.nextFloat()*.7f));
        }
        return render(stars,time,0,0,0,0,detailed);
    }
}
