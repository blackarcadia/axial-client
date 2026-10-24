package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MinimapLineClipTest {
    @Test void diagonalSquareClipKeepsTheActualLineInsteadOfInventingRectangularBorders() {
        var line=MinimapLineClip.clip(-50,-50,150,150,0,0,100,false);assertNotNull(line);
        assertEquals(0,line.x1(),1e-8);assertEquals(0,line.y1(),1e-8);assertEquals(100,line.x2(),1e-8);assertEquals(100,line.y2(),1e-8);
    }
    @Test void circularClipKeepsBothEndpointsInsideTheTerrainMask() {
        var line=MinimapLineClip.clip(-50,-50,150,150,0,0,100,true);assertNotNull(line);
        double delta=47/Math.sqrt(2);
        assertEquals(50-delta,line.x1(),1e-8);assertEquals(50-delta,line.y1(),1e-8);
        assertEquals(50+delta,line.x2(),1e-8);assertEquals(50+delta,line.y2(),1e-8);
    }
    @Test void horizontalAndVerticalWorldBordersClipWithoutDividingByZero() {
        var horizontal=MinimapLineClip.clip(-500,50,500,50,0,0,100,true);assertNotNull(horizontal);
        assertEquals(3,horizontal.x1(),1e-8);assertEquals(97,horizontal.x2(),1e-8);
        var vertical=MinimapLineClip.clip(50,-500,50,500,0,0,100,true);assertNotNull(vertical);
        assertEquals(3,vertical.y1(),1e-8);assertEquals(97,vertical.y2(),1e-8);
    }
    @Test void offscreenDegenerateAndInvalidEdgesDoNotRender() {
        assertNull(MinimapLineClip.clip(-100,-100,-50,-50,0,0,100,false));
        assertNull(MinimapLineClip.clip(0,0,0,100,0,0,100,true));
        assertNull(MinimapLineClip.clip(10,10,10,10,0,0,100,false));
        assertNull(MinimapLineClip.clip(Double.NaN,0,100,100,0,0,100,false));
    }
}
