package com.axial.worldmap;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RenderSchedulerTest {
    @Test void activeWorldHasPriorityWhileTheNetherAndEndStillAdvance() {
        var jobs=new LinkedHashMap<String,String>();
        for(String name:List.of("prisons","nether","end"))jobs.put(name,name);
        var counts=new HashMap<String,Integer>();
        for(int tick=1;tick<=80;tick++) {
            String selected=RenderScheduler.next(jobs,j->true,j->j.equals("prisons"),tick%4!=0);
            counts.merge(selected,1,Integer::sum);
        }
        assertTrue(counts.get("prisons")>=60);assertTrue(counts.get("nether")>0);assertTrue(counts.get("end")>0);
        assertEquals(80,counts.values().stream().mapToInt(Integer::intValue).sum());
    }
    @Test void noPlayersUsesRoundRobinAndDrainedJobsCannotBlockOtherWorlds() {
        var jobs=new LinkedHashMap<Integer,Integer>();for(int i=1;i<=3;i++)jobs.put(i,i);
        for(int i=1;i<=9;i++)assertEquals((i-1)%3+1,RenderScheduler.next(jobs,j->true,j->false,true));
        for(int i=0;i<10;i++)assertEquals(3,RenderScheduler.next(jobs,j->j==3,j->j==1,true));
        assertNull(RenderScheduler.next(jobs,j->false,j->true,false));
    }
}
