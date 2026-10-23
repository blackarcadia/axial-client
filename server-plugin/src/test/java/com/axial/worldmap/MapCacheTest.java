package com.axial.worldmap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MapCacheTest {
    @TempDir Path dir;
    @Test void unfinishedMapsResumeCompletedChunksIncludingEmptyTerrain() throws Exception {
        var id=UUID.randomUUID();var g=new MapGeometry(-16,-16,1,32,32);
        var chunks=g.plan();var first=chunks.getFirst();var coverage=new BitSet();
        for(int index:first.indices())coverage.set(index);
        var colors=new int[32*32]; // A sampled void chunk is still complete, despite zero colours.
        var partial=new WorldMap(id,g,12345,colors,coverage);
        coverage.clear();assertTrue(partial.hasChunk(first));assertFalse(partial.complete());
        var cache=new MapCache(dir);cache.write(partial);var restored=cache.read(id,g);
        assertNotNull(restored);assertFalse(restored.complete());assertTrue(restored.hasChunk(first));
        assertFalse(restored.hasChunk(chunks.getLast()));
        assertEquals(1,chunks.stream().filter(restored::hasChunk).count());
        cache.write(new WorldMap(id,g,23456,colors));assertTrue(cache.read(id,g).complete());
    }
    @Test void completedCachesFromThePreviousPluginFormatStillLoad() throws Exception {
        var id=UUID.randomUUID();var g=new MapGeometry(-2,3,1,2,2);
        try(var out=new java.io.DataOutputStream(new java.util.zip.GZIPOutputStream(Files.newOutputStream(dir.resolve(id+".map.gz"))))) {
            out.writeInt(0x41574D31);out.writeLong(id.getMostSignificantBits());out.writeLong(id.getLeastSignificantBits());
            out.writeInt(g.minX());out.writeInt(g.minZ());out.writeInt(g.step());out.writeInt(g.width());out.writeInt(g.height());out.writeLong(12345);
            for(int i=0;i<4;i++)out.writeInt(0xFF123456);
        }
        var restored=new MapCache(dir).read(id,g);assertNotNull(restored);assertTrue(restored.complete());
        assertEquals(0xFF123456,restored.colors()[3]);
    }
    @Test void completedMapsSurviveRestartAndChangedBordersInvalidateTheCache() throws Exception {
        UUID id=UUID.randomUUID();var geometry=MapGeometry.forBorder(-100,200,1024,256);
        int[] pixels=new int[geometry.width()*geometry.height()];
        for(int i=0;i<pixels.length;i++)pixels[i]=0xFF000000|i;
        var map=new WorldMap(id,geometry,12345,pixels);var cache=new MapCache(dir);cache.write(map);
        var restored=new MapCache(dir).read(id,geometry);assertNotNull(restored);
        assertEquals(map.world(),restored.world());assertEquals(map.renderedAt(),restored.renderedAt());assertArrayEquals(pixels,restored.colors());
        assertNull(cache.read(id,MapGeometry.forBorder(0,0,512,128)));
        try(var files=Files.list(dir)){assertEquals(1,files.count());}
    }
    @Test void truncatedAndWrongWorldCachesAreRejected() throws Exception {
        UUID id=UUID.randomUUID(),other=UUID.randomUUID();var g=new MapGeometry(0,0,1,2,2);var cache=new MapCache(dir);
        cache.write(new WorldMap(id,g,1,new int[4]));
        Files.copy(dir.resolve(id+".map.gz"),dir.resolve(other+".map.gz"));
        assertThrows(IOException.class,()->cache.read(other,g));
        Files.write(dir.resolve(id+".map.gz"),new byte[]{1,2,3});
        assertThrows(IOException.class,()->cache.read(id,g));
    }
}
