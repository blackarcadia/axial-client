package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapViewportTest {
    @Test void projectionRoundTripsNegativeAndDistantCoordinates() {
        for(double scale:new double[]{.25,1,8,64}) {
            var v=new MapViewport(-1200,800,scale);
            for(double x:new double[]{-30000000,-1280,0,30000000}) {
                assertEquals(x,v.worldX(v.pixelX(x,91,430),91,430),.00001);
                assertEquals(x,v.worldZ(v.pixelZ(x,37,430),37,430),.00001);
            }
        }
    }
    @Test void zoomKeepsThePointUnderTheCursorInPlace() {
        var v=new MapViewport(-500,1300,1);
        double x=v.worldX(210,40,500),z=v.worldZ(280,60,500);
        v.zoom(3,210,280,40,60,500);
        assertEquals(x,v.worldX(210,40,500),.000001);
        assertEquals(z,v.worldZ(280,60,500),.000001);
        v.zoom(-100,210,280,40,60,500);
        assertEquals(MapViewport.MAX_SCALE,v.scale);
        assertEquals(x,v.worldX(210,40,500),.000001);
    }
    @Test void draggingMovesTheTerrainWithThePointer() {
        var v=new MapViewport(10,20,2);
        double beforeX=v.pixelX(50,0,512),beforeZ=v.pixelZ(70,0,512);
        v.pan(40,-20,512);
        assertEquals(beforeX+40,v.pixelX(50,0,512),.000001);
        assertEquals(beforeZ-20,v.pixelZ(70,0,512),.000001);
    }
}
