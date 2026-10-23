package com.axial.worldmap;

import java.util.UUID;

record WorldMap(UUID world,MapGeometry geometry,long renderedAt,int[] colors) {
    WorldMap {
        if(colors.length!=geometry.width()*geometry.height())throw new IllegalArgumentException("Map pixel count mismatch");
    }
    String key(){return "world:"+world;}
}
