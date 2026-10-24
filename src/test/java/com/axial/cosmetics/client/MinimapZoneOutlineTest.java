package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MinimapZoneOutlineTest {
    private List<int[]> edges(int left,int top,int size,boolean circle,double x1,double y1,double x2,double y2) {
        var edges=new ArrayList<int[]>();MinimapZoneOutline.draw(left,top,size,circle,x1,y1,x2,y2,(a,b,c,d)->edges.add(new int[]{a,b,c,d}));return edges;
    }
    @Test void squareHudDrawsAllFourActualRegionBoundaries() {
        var lines=edges(0,0,200,false,50,60,150,160);assertEquals(4,lines.size());
        assertArrayEquals(new int[]{50,60,51,160},lines.get(0));assertArrayEquals(new int[]{149,60,150,160},lines.get(1));
        assertArrayEquals(new int[]{50,60,150,61},lines.get(2));assertArrayEquals(new int[]{50,159,150,160},lines.get(3));
    }
    @Test void offscreenBoundariesDoNotBecomeFakeEdgesAtTheViewportBorder() {
        var lines=edges(100,100,200,false,0,150,400,250);assertEquals(2,lines.size());
        assertArrayEquals(new int[]{100,150,300,151},lines.get(0));assertArrayEquals(new int[]{100,249,300,250},lines.get(1));
        assertTrue(edges(100,100,200,false,0,0,400,400).isEmpty());
        assertTrue(edges(100,100,200,false,-100,-100,0,0).isEmpty());
    }
    @Test void circularHudClipsEveryBoundaryPixelToTheExistingTerrainMask() {
        var lines=edges(30,40,200,true,-100,70,400,180);assertEquals(2,lines.size());
        for(var line:lines)for(int y=line[1];y<line[3];y++)for(int x=line[0];x<line[2];x++) {
            assertTrue(Math.hypot(x+.5-130,y+.5-140)<=99);
            assertTrue(x>=30&&x<230&&y>=40&&y<240);
        }
        assertTrue(lines.stream().allMatch(line->line[0]>30&&line[2]<230));
    }
    @Test void tinyAndInvalidRegionsStayBoundedToFourDraws() {
        assertTrue(edges(0,0,0,true,0,0,10,10).isEmpty());assertTrue(edges(0,0,200,false,10,20,5,2).isEmpty());
        assertTrue(edges(0,0,200,true,99,99,100,100).size()<=4);
    }
}
