package com.axial.worldmap;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MapRefreshPolicyTest {
    @Test void cachedMapBurstSchedulesAllRowsWithinFourTicksWithoutRaisingTheIncrementalBudget() {
        var g=new MapGeometry(0,0,16,512,512);var map=new WorldMap(UUID.randomUUID(),g,1,new int[512*512]);
        int packets=TransferPlan.stripes(map,null).size();
        int burst=MapRefreshPolicy.packetBudget(2,12,true),regular=MapRefreshPolicy.packetBudget(2,12,false);
        assertTrue((packets+burst-1)/burst<=4);assertEquals(2,regular);
        assertTrue((packets+regular-1)/regular>4);
    }
}
