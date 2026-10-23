package com.axial.cosmetics.client;

/** North-west relief lighting and depth-dependent visibility of the seabed. */
final class MinimapRelief {
    private MinimapRelief() { }

    static double shade(int height,int north,int east,int south,int west) {
        double n=Math.clamp(height-north,-4,4),w=Math.clamp(height-west,-4,4);
        double slopeZ=Math.clamp(south-north,-6,6),slopeX=Math.clamp(east-west,-6,6);
        double shelter=Math.min(6,Math.max(0,Math.max(Math.max(north,east),Math.max(south,west))-height));
        return Math.clamp(1+n*.065+w*.045+slopeZ*.025+slopeX*.018-shelter*.012,.64,1.22);
    }

    static int color(int base,double shade,int bed,int waterDepth) {
        int r=Math.clamp((int)Math.round(((base>>16)&255)*shade),0,255);
        int g=Math.clamp((int)Math.round(((base>>8)&255)*shade),0,255);
        int b=Math.clamp((int)Math.round((base&255)*shade),0,255);
        int shaded=0xFF000000|(r<<16)|(g<<8)|b;
        return waterDepth>0?water(shaded,bed,waterDepth):shaded;
    }

    static int water(int surface,int bed,int depth) {
        int bounded=Math.clamp(depth,1,16);
        double visibility=bed==0?0:.62*Math.exp(-(bounded-1)*.25);
        double brightness=1.04-.16*(bounded-1)/15.0;
        int r=channel(surface,bed,16,visibility,brightness);
        int g=channel(surface,bed,8,visibility,brightness);
        int b=channel(surface,bed,0,visibility,brightness);
        return 0xFF000000|(r<<16)|(g<<8)|b;
    }

    private static int channel(int surface,int bed,int shift,double visibility,double brightness) {
        return Math.clamp((int)Math.round(((surface>>shift)&255)*brightness*(1-visibility)+((bed>>shift)&255)*visibility),0,255);
    }
}
