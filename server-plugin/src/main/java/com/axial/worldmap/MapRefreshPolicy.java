package com.axial.worldmap;

/** Reload a cached image immediately; coalesce expensive background rescans. */
final class MapRefreshPolicy {
    private MapRefreshPolicy() { }
    static boolean rescanOnArrival(boolean enabled,WorldMap cached,boolean rendering,long now,Long lastScan,long interval) {
        return enabled&&cached!=null&&cached.complete()&&!rendering&&(lastScan==null||now-lastScan>=interval);
    }
    static int packetBudget(int normal,int initial,boolean freshTransfer){return freshTransfer?Math.max(normal,initial):normal;}
}
