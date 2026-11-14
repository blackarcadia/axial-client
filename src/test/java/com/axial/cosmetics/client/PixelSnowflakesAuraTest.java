package com.axial.cosmetics.client;

import java.util.*;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PixelSnowflakesAuraTest {
    @Test void whiteFlakesRemainVisibleFiniteAndBounded(){
        for(float tick:new float[]{-1,0,.25f,20,59,119.9f,120,240}){
            var mesh=PixelSnowflakesGeometry.frame(tick,true);assertFalse(mesh.isEmpty());assertTrue(mesh.size()<1200);
            long opaque=0;
            for(var quad:mesh){
                assertEquals(0xFFFFFF,quad.color()&0xffffff);if((quad.color()>>>24)==255)opaque++;
                for(var p:List.of(quad.a(),quad.b(),quad.c(),quad.d())){
                    assertTrue(Float.isFinite(p.x())&&Float.isFinite(p.y())&&Float.isFinite(p.z()));
                    assertTrue(Math.abs(p.x())<1.3&&Math.abs(p.z())<1.3&&p.y()>-.5&&p.y()<2.6);
                }
            }
            assertTrue(opaque>mesh.size()/2,"Most of each snowflake's lifetime should be fully visible");
            assertTrue(PixelSnowflakesGeometry.frame(tick,false).size()<mesh.size());
        }
    }
    @Test void orientationVariesInThreeDimensionsAndMotionLoopsSmoothly(){
        assertEquals(PixelSnowflakesGeometry.frame(0,true),PixelSnowflakesGeometry.frame(120,true));
        assertEquals(PixelSnowflakesGeometry.frame(-1,true),PixelSnowflakesGeometry.frame(119,true));
        assertNotEquals(PixelSnowflakesGeometry.frame(0,true),PixelSnowflakesGeometry.frame(1,true));
        var normals=new HashSet<String>();boolean tilted=false;
        for(var q:PixelSnowflakesGeometry.frame(0,true)){
            var a=new Vector3f(q.b().x()-q.a().x(),q.b().y()-q.a().y(),q.b().z()-q.a().z());
            var b=new Vector3f(q.c().x()-q.a().x(),q.c().y()-q.a().y(),q.c().z()-q.a().z());
            var n=a.cross(b).normalize();normals.add(Math.round(n.x*10)+","+Math.round(n.y*10)+","+Math.round(n.z*10));
            tilted|=Math.abs(n.x)>.2&&Math.abs(n.y)>.2&&Math.abs(n.z)>.2;
        }
        assertTrue(tilted);assertTrue(normals.size()>10,"Snowflakes should not all face the same direction");
    }
    @Test void registeredAuraHasItsOwnAnimatedPreviewAndNeedsARealGrant() throws Exception {
        assertSame(WardrobeAura.PIXEL_SNOWFLAKES,WardrobeAura.fromId("pixel_snowflakes"));
        assertFalse(WardrobeAura.PIXEL_SNOWFLAKES.ownedBy(UUID.randomUUID()));
        assertNotEquals(WardrobeAura.PIXEL_STARS.preview(),WardrobeAura.PIXEL_SNOWFLAKES.preview());
        try(var in=getClass().getResourceAsStream("/assets/axial_cosmetics/textures/gui/pixel_snowflakes_preview.png")){
            assertNotNull(in);var image=javax.imageio.ImageIO.read(in);assertEquals(1024,image.getWidth());assertEquals(1024,image.getHeight());
        }
    }
}
