package com.axial.worldmap;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MapRefreshPolicyTest {
    private WorldMap cached() { return new WorldMap(UUID.randomUUID(),new MapGeometry(0,0,1,16,16),1,new int[256]); }
    @Test void worldArrivalsRefreshCompleteMapsButCoalesceRunningAndRecentScans() {
        var map=cached();
        assertTrue(MapRefreshPolicy.rescanOnArrival(true,map,false,100_000,null,30_000));
        assertTrue(MapRefreshPolicy.rescanOnArrival(true,map,false,100_000,70_000L,30_000));
        assertFalse(MapRefreshPolicy.rescanOnArrival(true,map,true,100_000,null,30_000));
        assertFalse(MapRefreshPolicy.rescanOnArrival(true,map,false,100_000,99_000L,30_000));
        assertFalse(MapRefreshPolicy.rescanOnArrival(false,map,false,100_000,null,30_000));
    }
    @Test void unfinishedAndMissingMapsContinueTheirInitialRenderInsteadOfRestartingOnEachJoin() {
        assertFalse(MapRefreshPolicy.rescanOnArrival(true,null,false,100_000,null,30_000));
        var complete=cached();var partial=new WorldMap(complete.world(),complete.geometry(),1,complete.colors(),new BitSet());
        assertFalse(MapRefreshPolicy.rescanOnArrival(true,partial,false,100_000,null,30_000));
    }
    @Test void cachedMapBurstSchedulesAllRowsWithinFourTicksWithoutRaisingTheIncrementalBudget() {
        var g=new MapGeometry(0,0,16,512,512);var map=new WorldMap(UUID.randomUUID(),g,1,new int[512*512]);
        int packets=TransferPlan.stripes(map,null).size();
        int burst=MapRefreshPolicy.packetBudget(2,12,true),regular=MapRefreshPolicy.packetBudget(2,12,false);
        assertTrue((packets+burst-1)/burst<=4);assertEquals(2,regular);
        assertTrue((packets+regular-1)/regular>4);
    }
}
