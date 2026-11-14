package com.axial.cosmetics.client;

import java.util.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.axial.cosmetics.client.HexwindGeometry.Point;
import com.axial.cosmetics.client.HexwindGeometry.Quad;

/** White, branched pixel snowflakes drifting and tumbling around the player. */
public final class PixelSnowflakesGeometry {
    public static final float PERIOD=120;
    private record Flake(double phase,double angle,double radius,double bob,float size,float pitch,float yaw,float roll,int spin) {}
    private record Run(int x,int y,int width) {}
    private static final List<Flake> FLAKES;
    private static final List<Run> SHAPE;
    static {
        var random=new Random(0x5A0F1A);var flakes=new ArrayList<Flake>();
        for(int i=0;i<28;i++)flakes.add(new Flake((i+random.nextDouble()*.65)/28,random.nextDouble()*Math.PI*2,1.00+random.nextDouble()*.30,
                random.nextDouble()*Math.PI*2,.14f+random.nextFloat()*.05f,(random.nextFloat()-.5f)*1.7f,random.nextFloat()*(float)Math.PI*2,random.nextFloat()*(float)Math.PI*2,random.nextBoolean()?1:-1));
        FLAKES=List.copyOf(flakes);
        boolean[][] pixels=new boolean[13][13];
        // Six separated arms with forked tips, rather than a dense star/grid silhouette.
        for(int i=-6;i<=6;i++)pixel(pixels,0,i);
        for(int a:new int[]{-1,1})for(int b:new int[]{-1,1}){
            for(int i=1;i<=6;i++)pixel(pixels,a*i,b*Math.round(i*.5f));
            pixel(pixels,a, b*5);pixel(pixels,a*2,b*6);
            pixel(pixels,a*4,b*3);pixel(pixels,a*4,b*4);
            pixel(pixels,a*5,b*2);pixel(pixels,a*6,b*2);
        }
        var runs=new ArrayList<Run>();
        for(int y=0;y<13;y++)for(int x=0;x<13;){
            if(!pixels[y][x]){x++;continue;}int start=x;while(x<13&&pixels[y][x])x++;runs.add(new Run(start-6,y-6,x-start));
        }
        SHAPE=List.copyOf(runs);
    }
    private PixelSnowflakesGeometry() {}
    private static void pixel(boolean[][] mask,int x,int y){mask[y+6][x+6]=true;}
    public static List<Quad> frame(float ticks,boolean detailed){
        double time=ticks-Math.floor(ticks/PERIOD)*PERIOD,phase=time/PERIOD*Math.PI*2;
        var mesh=new ArrayList<Quad>();
        for(int i=0;i<FLAKES.size();i++){
            if(!detailed&&i%2!=0)continue;
            var f=FLAKES.get(i);double age=(time/PERIOD+f.phase())%1;
            double fade=Math.min(1,age*9)*Math.min(1,(1-age)*9);int alpha=(int)(fade*255);if(alpha==0)continue;
            double angle=f.angle()+phase*f.spin()+Math.sin(phase+f.bob())*.18;
            double radius=f.radius()+Math.sin(phase*2+f.bob())*.055;
            float x=(float)(Math.cos(angle)*radius),z=(float)(Math.sin(angle)*radius),y=(float)(2.20-age*2.35+Math.sin(phase*2+f.bob())*.055);
            var rotation=new Quaternionf().rotateYXZ(f.yaw()+(float)phase*f.spin(),f.pitch()+(float)Math.sin(phase+f.bob())*.23f,f.roll()-(float)phase*f.spin());
            float pixel=f.size()/6.5f;int color=(alpha<<24)|0xFFFFFF;
            for(var run:SHAPE){
                float u=(run.x()-.5f)*pixel,v=(run.y()-.5f)*pixel,w=run.width()*pixel;
                mesh.add(new Quad(point(x,y,z,u,v,rotation),point(x,y,z,u+w,v,rotation),point(x,y,z,u+w,v+pixel,rotation),point(x,y,z,u,v+pixel,rotation),color));
            }
        }
        return List.copyOf(mesh);
    }
    private static Point point(float x,float y,float z,float u,float v,Quaternionf rotation){
        var p=rotation.transform(new Vector3f(u,v,0));return new Point(x+p.x,y+p.y,z+p.z);
    }
}
