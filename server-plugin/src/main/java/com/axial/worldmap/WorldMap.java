package com.axial.worldmap;

import java.util.UUID;
import java.util.BitSet;

record WorldMap(UUID world,MapGeometry geometry,long renderedAt,int[] colors,BitSet sampled) {
    WorldMap(UUID world,MapGeometry geometry,long renderedAt,int[] colors){this(world,geometry,renderedAt,colors,null);}
    WorldMap {
        if(colors.length!=geometry.width()*geometry.height())throw new IllegalArgumentException("Map pixel count mismatch");
        if(sampled!=null){sampled=(BitSet)sampled.clone();if(sampled.length()>colors.length)throw new IllegalArgumentException("Invalid map coverage");}
    }
    boolean complete(){return sampled==null;}
    boolean hasChunk(MapGeometry.ChunkSamples chunk) {
        if(sampled==null)return true;
        for(int index:chunk.indices())if(!sampled.get(index))return false;
        return true;
    }
    String key(){return "world:"+world;}
}
