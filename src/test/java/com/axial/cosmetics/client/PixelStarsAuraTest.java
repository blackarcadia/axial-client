package com.axial.cosmetics.client;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PixelStarsAuraTest {
    @Test void emitterNeedsMovementAndStopsEmittingAtRest() {
        var e=new PixelStarsEmitter(new UUID(1,2));
        for(int t=0;t<10;t++)e.tick(t,0,64,0);
        assertTrue(e.stars().isEmpty());
        e.tick(10,.2,64,0);assertFalse(e.stars().isEmpty());
        var born=e.stars().stream().map(PixelStarsGeometry.Star::born).toList();
        e.tick(11,.2,64,0);assertEquals(born,e.stars().stream().map(PixelStarsGeometry.Star::born).toList());
        for(int t=12;t<50;t++)e.tick(t,.2,64,0);
        assertTrue(e.stars().isEmpty(),"The old trail must finish fading after movement stops");
    }
    @Test void trailBirthsStayBehindInWorldSpaceAndTeleportResetsHistory() {
        var e=new PixelStarsEmitter(new UUID(2,3));e.tick(0,1000000,64,-1000000);
        e.tick(1,1000000.2,64,-1000000);var first=e.stars().getFirst();
        for(int t=2;t<12;t++)e.tick(t,1000000+t*.2,64,-1000000);
        assertEquals(first,e.stars().getFirst(),"New player positions must not drag old stars along");
        assertTrue(first.x()<1000001);
        e.tick(12,2000000,70,2000000);assertTrue(e.stars().isEmpty());
        e.tick(13,2000000.2,70,2000000);assertFalse(e.stars().isEmpty());
        e.tick(20,2000000.4,70,2000000);assertTrue(e.stars().isEmpty(),"A gap in tracking cannot create a long bridge");
    }
    @Test void birthsAreBoundedAndDoNotDependOnRendering() {
        var e=new PixelStarsEmitter(new UUID(3,4));
        for(int t=0;t<2000;t++){e.tick(t,t*.25,0,0);assertTrue(e.stars().size()<=PixelStarsEmitter.MAX_STARS);}
        var before=e.stars();PixelStarsGeometry.render(before,1999.5,499.8,0,0,135,true);
        assertEquals(before,e.stars());
    }
    @Test void previewIsSeamlessFinitePastelAndHasALighterDistantMesh() {
        assertEquals(PixelStarsGeometry.frame(0,true),PixelStarsGeometry.frame(120,true));
        assertNotEquals(PixelStarsGeometry.frame(0,true),PixelStarsGeometry.frame(1,true));
        var colors=new HashSet<Integer>();
        for(float t=0;t<120;t+=2){
            var mesh=PixelStarsGeometry.frame(t,true);assertFalse(mesh.isEmpty());assertTrue(mesh.size()<1500);
            assertTrue(PixelStarsGeometry.frame(t,false).size()<mesh.size());
            for(var q:mesh){colors.add(q.color()&0xffffff);for(var p:List.of(q.a(),q.b(),q.c(),q.d())){
                assertTrue(Float.isFinite(p.x())&&Float.isFinite(p.y())&&Float.isFinite(p.z()));
                assertTrue(Math.abs(p.x())<2&&Math.abs(p.z())<2.5&&p.y()>-.5&&p.y()<2.5);
            }}
        }
        assertEquals(Set.of(0xFAFDFF,0xA8DFFF,0xFFB8DF),colors);
    }
    @Test void playerTranslationIsRemovedFromTheHistoricalStarPosition() {
        var star=new PixelStarsGeometry.Star(4,1,2,0,0,0,0,20,.12f,0xFAFDFF,0);
        var one=PixelStarsGeometry.render(List.of(star),3,0,0,0,0,true).getFirst().a();
        var two=PixelStarsGeometry.render(List.of(star),3,2,0,0,0,true).getFirst().a();
        assertEquals(one.x()-2,two.x(),.00001);assertEquals(one.z(),two.z());
        var turned=PixelStarsGeometry.render(List.of(star),3,0,0,0,90,true).getFirst().a();
        assertTrue(Float.isFinite(turned.x())&&Float.isFinite(turned.z()));
    }
    @Test void auraAndAnimatedWardrobeAtlasAreRegistered() throws Exception {
        assertSame(WardrobeAura.PIXEL_STARS,WardrobeAura.fromId("pixel_stars"));
        assertFalse(WardrobeAura.PIXEL_STARS.ownedBy(UUID.randomUUID()));
        try(var in=getClass().getResourceAsStream("/assets/axial_cosmetics/textures/gui/pixel_stars_preview.png")){
            assertNotNull(in);var image=javax.imageio.ImageIO.read(in);assertEquals(1024,image.getWidth());assertEquals(1024,image.getHeight());
        }
    }
}
