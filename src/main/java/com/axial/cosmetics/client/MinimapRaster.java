package com.axial.cosmetics.client;

import java.util.function.IntBinaryOperator;

/** Pure CPU rasterisation; no world reads, graphics calls, or per-pixel allocation. */
final class MinimapRaster {
    @FunctionalInterface interface PixelWriter { void set(int x,int z,int color); }
    static void paint(int pixels,double centerX,double centerZ,double scale,boolean circle,
                      IntBinaryOperator source,PixelWriter target) {
        double span=scale*256;
        int[] columns=new int[pixels],rowColors=new int[pixels];
        for(int x=0;x<pixels;x++)columns[x]=(int)Math.floor(centerX+((x+.5)/pixels-.5)*span);
        int previousZ=Integer.MIN_VALUE;
        double radius=pixels/2.0-1,radiusSquared=radius*radius;
        for(int z=0;z<pixels;z++) {
            int wz=(int)Math.floor(centerZ+((z+.5)/pixels-.5)*span);
            if(wz!=previousZ) {
                previousZ=wz;int previousX=Integer.MIN_VALUE,color=0;
                for(int x=0;x<pixels;x++) {
                    if(columns[x]!=previousX){previousX=columns[x];color=source.applyAsInt(previousX,wz);}
                    rowColors[x]=color;
                }
            }
            double dy=z+.5-pixels/2.0;
            double half=circle?Math.sqrt(Math.max(0,radiusSquared-dy*dy)):pixels;
            int left=circle?(int)Math.ceil(pixels/2.0-half-.5):0;
            int right=circle?(int)Math.floor(pixels/2.0+half-.5):pixels-1;
            for(int x=0;x<pixels;x++)target.set(x,z,circle&&(dy*dy>radiusSquared||x<left||x>right)?0:rowColors[x]);
        }
    }
}
