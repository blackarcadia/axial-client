package com.axial.worldmap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MapCacheTest {
    @TempDir Path dir;
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
