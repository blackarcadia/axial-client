package com.axial.worldmap;

/** Bound outgoing traffic while allowing fast delivery of a prepared snapshot. */
final class MapRefreshPolicy {
    private MapRefreshPolicy() { }
    static int packetBudget(int normal,int initial,boolean freshTransfer){return freshTransfer?Math.max(normal,initial):normal;}
}
