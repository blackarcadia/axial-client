package com.axial.cosmetics.client;

/** Small CPU-side texture pyramid: preserve texels up close, average them at distance. */
final class MinimapTile {
    static final int SIZE=16;
    private final int[][] levels=new int[5][];

    MinimapTile(int[] pixels) {
        if(pixels.length!=SIZE*SIZE)throw new IllegalArgumentException("Expected a 16 by 16 tile");
        levels[0]=pixels.clone();
        // Cutout textures (leaves, plants) should not leave black gaps on the map.
        int base=average(pixels);
        for(int i=0;i<levels[0].length;i++)levels[0][i]=over(levels[0][i],base);
        for(int level=1,size=8;level<levels.length;level++,size/=2) {
            int[] previous=levels[level-1],next=levels[level]=new int[size*size];
            for(int z=0;z<size;z++)for(int x=0;x<size;x++) {
                int p=z*2*size*2+x*2;
                next[z*size+x]=average(new int[]{previous[p],previous[p+1],previous[p+size*2],previous[p+size*2+1]});
            }
        }
    }
    static int levelFor(double blocksPerPixel) {
        return Math.clamp((int)Math.floor(Math.log(Math.max(1,blocksPerPixel*SIZE))/Math.log(2)),0,4);
    }
    int sample(double x,double z,int level,int tint,double shade) {
        int size=SIZE>>level;
        int ix=Math.min(size-1,(int)((x-Math.floor(x))*size));
        int iz=Math.min(size-1,(int)((z-Math.floor(z))*size));
        return tint(levels[level][iz*size+ix],tint,shade);
    }
    int average(int tint,double shade){return tint(levels[4][0],tint,shade);}
    private static int tint(int color,int tint,double shade) {
        int r=(int)Math.round(((color>>16)&255)*((tint>>16)&255)/255.0*shade);
        int g=(int)Math.round(((color>>8)&255)*((tint>>8)&255)/255.0*shade);
        int b=(int)Math.round((color&255)*(tint&255)/255.0*shade);
        return 0xFF000000|(Math.clamp(r,0,255)<<16)|(Math.clamp(g,0,255)<<8)|Math.clamp(b,0,255);
    }
    private static int average(int[] pixels) {
        long r=0,g=0,b=0,a=0;
        for(int c:pixels){int alpha=c>>>24;a+=alpha;r+=((c>>16)&255)*alpha;g+=((c>>8)&255)*alpha;b+=(c&255)*alpha;}
        return a==0?0xFF808080:0xFF000000|((int)(r/a)<<16)|((int)(g/a)<<8)|(int)(b/a);
    }
    private static int over(int c,int base) {
        int a=c>>>24;
        return 0xFF000000|(((((c>>16)&255)*a+((base>>16)&255)*(255-a))/255)<<16)
                |(((((c>>8)&255)*a+((base>>8)&255)*(255-a))/255)<<8)|(((c&255)*a+(base&255)*(255-a))/255);
    }
}
