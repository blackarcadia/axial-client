package com.axial.cosmetics.client;

/** One north-up projection shared by map drawing, mouse navigation and markers. */
final class MapViewport {
    static final double MIN_SCALE=.25, MAX_SCALE=64;
    double centerX, centerZ, scale;
    MapViewport(double x,double z,double scale) { centerX=x;centerZ=z;this.scale=Math.clamp(scale,MIN_SCALE,MAX_SCALE); }
    double worldX(double pixel,int left,int size) { return centerX+(pixel-left-size/2.0)*scale*256/size; }
    double worldZ(double pixel,int top,int size) { return centerZ+(pixel-top-size/2.0)*scale*256/size; }
    double pixelX(double world,int left,int size) { return left+size/2.0+(world-centerX)*size/(scale*256); }
    double pixelZ(double world,int top,int size) { return top+size/2.0+(world-centerZ)*size/(scale*256); }
    void pan(double dx,double dy,int size) { centerX-=dx*scale*256/size;centerZ-=dy*scale*256/size; }
    void zoom(double amount,double px,double py,int left,int top,int size) {
        double x=worldX(px,left,size),z=worldZ(py,top,size);
        scale=Math.clamp(scale*Math.pow(.8,amount),MIN_SCALE,MAX_SCALE);
        centerX+=x-worldX(px,left,size);centerZ+=z-worldZ(py,top,size);
    }
}
