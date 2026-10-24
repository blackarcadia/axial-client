package com.axial.cosmetics.client;

/** Clockwise angle from twelve o'clock; decreasing the endpoint moves counterclockwise. */
final class CooldownRing {
    static final int SIZE=56,STEPS=128;
    static double angle(int x,int y){double a=Math.atan2(y+.5-SIZE/2.0,x+.5-SIZE/2.0)+Math.PI/2;return a<0?a+Math.PI*2:a;}
    static int alpha(int x,int y){
        double distance=Math.hypot(x+.5-SIZE/2.0,y+.5-SIZE/2.0);
        return (int)Math.round(Math.clamp(Math.min(distance-24,27-distance)+.5,0,1)*255);
    }
    static boolean covered(int x,int y,float remaining){return alpha(x,y)>0&&remaining>0&&angle(x,y)<=Math.clamp(remaining,0,1)*Math.PI*2;}
}
