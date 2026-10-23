package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MinimapReliefTest {
    @Test void flatGroundHasNoArtificialContoursOrCoordinateNoise() {
        assertEquals(1,MinimapRelief.shade(64,64,64,64,64));
        assertEquals(1,MinimapRelief.shade(170,170,170,170,170));
    }
    @Test void OppositeSlopesHaveReadableHighlightsAndShadows() {
        double lit=MinimapRelief.shade(64,63,65,65,63);
        double shaded=MinimapRelief.shade(64,65,63,63,65);
        assertTrue(lit>1.1);assertTrue(shaded<.9);
        assertTrue(MinimapRelief.shade(64,64,64,64,64)>MinimapRelief.shade(64,68,68,68,68));
        assertTrue(MinimapRelief.shade(64,300,300,300,300)>=.64);
        assertTrue(MinimapRelief.shade(300,0,0,0,0)<=1.22);
    }
    @Test void ShallowsRevealSandWhileDeepWaterRetainsItsBiomeColour() {
        int blue=0xFF305FA0,sand=0xFFD9CB89;
        int shallow=MinimapRelief.water(blue,sand,1),deep=MinimapRelief.water(blue,sand,16);
        assertTrue(((shallow>>16)&255)>((deep>>16)&255));
        assertTrue(((shallow>>8)&255)>((deep>>8)&255));
        assertTrue((deep&255)>((deep>>16)&255));
        assertEquals(0xFF000000,deep&0xFF000000);
    }
    @Test void MissingSeabedAndLargeDepthsStayStableAndOpaque() {
        int blue=0xFF305FA0;
        assertEquals(MinimapRelief.water(blue,0,16),MinimapRelief.water(blue,0,1000));
        assertEquals(MinimapRelief.water(blue,0,1),MinimapRelief.water(blue,0,0));
        assertNotEquals(0,MinimapRelief.water(blue,0,8));
    }
}
