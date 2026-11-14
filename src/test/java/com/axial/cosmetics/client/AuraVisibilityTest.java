package com.axial.cosmetics.client;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuraVisibilityTest {
    @Test void auraPipelineIsUnlitEmissiveWithoutDirectionalBackFaceDimming(){
        var flags=AuraRenderLayers.PIPELINE.getShaderDefines().flags();
        assertTrue(flags.contains("EMISSIVE"));assertTrue(flags.contains("NO_CARDINAL_LIGHTING"));assertTrue(flags.contains("NO_OVERLAY"));
        assertFalse(flags.contains("PER_FACE_LIGHTING"));
    }
    @Test void starsStayOpaqueAcrossTheMainLifetimeThenFadeOut(){
        var star=new PixelStarsGeometry.Star(0,1,0,0,0,0,0,20,.12f,0xA8DFFF,0);
        for(double tick:new double[]{3,8,15}){
            var mesh=PixelStarsGeometry.render(List.of(star),tick,0,0,0,0,true);
            assertEquals(255,mesh.getFirst().color()>>>24);
            double left=Double.POSITIVE_INFINITY,right=Double.NEGATIVE_INFINITY;
            for(var q:mesh)for(var p:List.of(q.a(),q.b(),q.c(),q.d())){left=Math.min(left,p.x());right=Math.max(right,p.x());}
            assertTrue(right-left>.30,"Stars should have a readable silhouette rather than tiny dim pinpoints");
        }
        assertTrue((PixelStarsGeometry.render(List.of(star),19,0,0,0,0,true).getFirst().color()>>>24)<100);
        assertTrue(PixelStarsGeometry.render(List.of(star),20,0,0,0,0,true).isEmpty());
    }
    @Test void hexwindRibbonsHaveVisibleColorAndAlpha(){
        var ribbons=HexwindGeometry.frame(30,true);
        assertTrue((ribbons.getFirst().color()>>>24)>=200);
        assertTrue(((ribbons.getFirst().color()>>>16)&255)>=50);
        assertTrue((ribbons.get(1).color()>>>24)>=220);
    }
}
