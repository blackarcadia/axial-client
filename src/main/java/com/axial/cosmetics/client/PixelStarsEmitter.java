package com.axial.cosmetics.client;

import java.util.*;

/** Tick-based, bounded motion history. Rendering never spawns particles. */
final class PixelStarsEmitter {
    static final int MAX_STARS=64;
    private final Random random;
    private final ArrayList<PixelStarsGeometry.Star> stars=new ArrayList<>();
    private double x,y,z;
    private long previous=Long.MIN_VALUE;
    private int sequence;
    PixelStarsEmitter(UUID account){random=new Random(account.getMostSignificantBits()^account.getLeastSignificantBits());}
    void tick(long tick,double nx,double ny,double nz){
        stars.removeIf(s->tick-s.born()>=s.life());
        if(previous==Long.MIN_VALUE||tick-previous!=1||square(nx-x)+square(ny-y)+square(nz-z)>9){
            stars.clear();x=nx;y=ny;z=nz;previous=tick;return;
        }
        double speed=Math.sqrt(square(nx-x)+square(nz-z)+square(ny-y)*.25);
        if(speed>.008){
            int count=Math.min(4,Math.max(2,(int)Math.ceil(speed*10)));
            for(int i=0;i<count;i++){
                double blend=(i+random.nextDouble())/count,angle=random.nextDouble()*Math.PI*2,radius=.82+random.nextDouble()*.16,drift=.016+random.nextDouble()*.016;
                stars.add(new PixelStarsGeometry.Star(x+(nx-x)*blend+Math.cos(angle)*radius,y+(ny-y)*blend+.12+random.nextDouble()*1.65,z+(nz-z)*blend+Math.sin(angle)*radius,
                        Math.cos(angle)*drift,.008+random.nextDouble()*.014,Math.sin(angle)*drift,tick-1+blend,18+random.nextDouble()*8,.10f+random.nextFloat()*.05f,PixelStarsGeometry.COLORS[sequence++%3],random.nextFloat()*.8f));
            }
        }
        while(stars.size()>MAX_STARS)stars.removeFirst();
        x=nx;y=ny;z=nz;previous=tick;
    }
    List<PixelStarsGeometry.Star> stars(){return List.copyOf(stars);}
    private static double square(double x){return x*x;}
}
