package com.axial.worldmap;

import java.util.*;

/** Diff complete immutable maps into protocol stripes; first contact always gets every row. */
final class TransferPlan {
    private TransferPlan(){ }
    static Deque<Integer> stripes(WorldMap map,WorldMap old) {
        Deque<Integer> stripes=new ArrayDeque<>();int rows=MapProtocol.rowsPerPacket(map),width=map.geometry().width();
        boolean compatible=old!=null&&old.world().equals(map.world())&&old.geometry().equals(map.geometry());
        for(int row=0;row<map.geometry().height();row+=rows) {
            int start=row*width,end=Math.min(map.colors().length,(row+rows)*width);
            if(!compatible||!Arrays.equals(old.colors(),start,end,map.colors(),start,end))stripes.add(row);
        }
        return stripes;
    }
}
