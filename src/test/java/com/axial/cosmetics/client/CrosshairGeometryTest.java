package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.axial.cosmetics.client.CrosshairConfig.CrosshairStyle.*;

class CrosshairGeometryTest {
    private Set<String> pixels(List<CrosshairGeometry.Rect> rectangles){
        Set<String> pixels=new HashSet<>();for(var r:rectangles)for(int y=r.y();y<r.y()+r.height();y++)for(int x=r.x();x<r.x()+r.width();x++)assertTrue(pixels.add(x+","+y),"Overlapping rectangle");return pixels;
    }
    private CrosshairGeometry.Shape shape(CrosshairConfig c){return CrosshairGeometry.build(CrosshairGeometry.Key.of(c,1));}
    @Test void dotsHaveExactDimensionsIncludingSinglePixels(){
        var c=new CrosshairConfig();c.style=DOT;c.width=1;c.outlineEnabled=false;
        assertEquals(Set.of("0,0"),pixels(shape(c).ink()));
        c.width=2;assertEquals(4,pixels(shape(c).ink()).size());
        c.style=SMALL_DOT;c.width=1;assertEquals(1,pixels(shape(c).ink()).size());
    }
    @Test void circleIsRoundAndHollowAndTLeavesTheTopOpen(){
        var c=new CrosshairConfig();c.style=CIRCLE;c.length=5;c.width=1;c.gap=0;
        var ring=pixels(shape(c).ink());assertTrue(ring.contains("0,-5"));assertTrue(ring.contains("3,4"));assertFalse(ring.contains("5,5"));assertFalse(ring.contains("0,0"));
        c.style=T;c.length=4;c.gap=2;var t=pixels(shape(c).ink());
        assertTrue(t.contains("-4,0"));assertTrue(t.contains("4,0"));assertTrue(t.contains("0,4"));assertFalse(t.contains("0,-4"));
    }
    @Test void outlinesAreSeparateFromInkAndCenterDotsDoNotFillGaps(){
        var c=new CrosshairConfig();c.style=LUNAR;c.width=1;c.gap=4;c.centerDot=true;c.dotSize=1;
        var s=shape(c);var ink=pixels(s.ink());var outline=pixels(s.outline());
        assertTrue(ink.contains("0,0"));assertFalse(ink.contains("0,3"));
        assertTrue(Collections.disjoint(ink,outline));assertTrue(outline.contains("1,0"));
        c.outlineEnabled=false;assertTrue(shape(c).outline().isEmpty());
    }
    @Test void allTemplatesRenderDistinctVisibleShapes(){
        Set<Set<String>> unique=new HashSet<>();
        for(var preset:CrosshairPresets.templates()){
            var c=preset.preview(new CrosshairConfig());var ink=pixels(shape(c).ink());
            assertFalse(ink.isEmpty(),preset.name());assertTrue(unique.add(ink),"Duplicate template: "+preset.name());
            assertTrue(shape(c).ink().size()<100,preset.name());
        }
        assertEquals(20,unique.size());
    }
    @Test void dynamicGapIsBoundedAndRepeatedStatesHaveIdenticalKeys(){
        var c=new CrosshairConfig();var idle=CrosshairGeometry.Key.of(c,1);var active=CrosshairGeometry.Key.of(c,1.4f);
        assertTrue(active.gap()>idle.gap());assertEquals(idle,CrosshairGeometry.Key.of(c,1));
        c.size=Float.NaN;c.length=Float.POSITIVE_INFINITY;
        var key=CrosshairGeometry.Key.of(c,Float.NaN);assertTrue(key.length()<=128);assertTrue(key.gap()<=88);
    }
}
