package com.axial.worldmap;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MapGeometryTest {
    @Test void samplesCoverTheBorderAtNegativeAndOffsetCoordinatesWithoutDuplicateChunks() {
        for(double[] input:new double[][]{{0,0,128},{-123.5,837.25,513},{400,-900,4096},{0,0,65536}}) {
            var g=MapGeometry.forBorder(input[0],input[1],input[2],512);
            assertTrue(g.width()<=512&&g.height()<=512);
            assertTrue(g.minX()<=input[0]-input[2]/2+1);
            assertTrue(g.minX()+(long)g.width()*g.step()>=input[0]+input[2]/2);
            boolean[] seen=new boolean[g.width()*g.height()];Set<Long> chunks=new HashSet<>();
            for(var task:g.plan()) {
                assertTrue(chunks.add(((long)task.x()<<32)|(task.z()&0xFFFFFFFFL)));
                assertArrayEquals(task.indices(),g.samplesForChunk(task.x(),task.z()).indices());
                for(int i:task.indices()) {
                    assertFalse(seen[i]);seen[i]=true;
                    assertEquals(task.x(),Math.floorDiv(g.x(i),16));assertEquals(task.z(),Math.floorDiv(g.z(i),16));
                    assertTrue(g.x(i)<Math.ceil(input[0]+input[2]/2));assertTrue(g.z(i)<Math.ceil(input[1]+input[2]/2));
                }
            }
            for(boolean sample:seen)assertTrue(sample);
        }
    }
    @Test void outsideChunksHaveNoSamplesAndInvalidBordersAreRejected() {
        var g=MapGeometry.forBorder(0,0,256,128);assertEquals(0,g.samplesForChunk(2000,2000).indices().length);
        assertThrows(IllegalArgumentException.class,()->MapGeometry.forBorder(0,0,Double.NaN,512));
        assertThrows(IllegalArgumentException.class,()->MapGeometry.forBorder(30_000_000,0,100,512));
        assertThrows(IllegalArgumentException.class,()->new MapGeometry(0,0,0,10,10));
    }
}
