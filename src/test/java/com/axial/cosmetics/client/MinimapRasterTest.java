package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class MinimapRasterTest {
    private static int color(int x,int z){return 0xFF000000|((x&255)<<8)|(z&255);}
    @Test void fullMapRetainsEveryPixelAt1024ResolutionAndBothZoomExtremes() {
        int size=1024;
        for(double scale:new double[]{.25,1,262144}) {
            int[] pixels=new int[size*size];
            MinimapRaster.paint(size,-123.75,50.125,scale,false,MinimapRasterTest::color,(x,z,c)->pixels[z*size+x]=c);
            for(int z=0;z<size;z++)for(int x=0;x<size;x++) {
                int wx=(int)Math.floor(-123.75+((x+.5)/size-.5)*scale*256);
                int wz=(int)Math.floor(50.125+((z+.5)/size-.5)*scale*256);
                assertEquals(color(wx,wz),pixels[z*size+x]);
            }
        }
    }
    @Test void circularMaskMatchesTheOriginalCircleWithoutPerPixelSquareRoots() {
        int size=256;int[] pixels=new int[size*size];
        MinimapRaster.paint(size,0,0,1,true,(x,z)->0xFF123456,(x,z,c)->pixels[z*size+x]=c);
        for(int z=0;z<size;z++)for(int x=0;x<size;x++)assertEquals(Math.hypot(x+.5-size/2.0,z+.5-size/2.0)>size/2.0-1?0:0xFF123456,pixels[z*size+x]);
    }
    @Test void zoomedInMapLooksUpEachBlockOnceInsteadOfOncePerDisplayPixel() {
        var calls=new AtomicInteger();
        MinimapRaster.paint(1024,0,0,.25,false,(x,z)->{calls.incrementAndGet();return color(x,z);},(x,z,c)->{});
        assertEquals(64*64,calls.get());
    }
}
