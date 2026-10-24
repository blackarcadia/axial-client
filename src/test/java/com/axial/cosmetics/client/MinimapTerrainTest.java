package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import org.junit.jupiter.api.Test;
import java.io.StringWriter;
import java.io.StringReader;
import com.google.gson.stream.JsonReader;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class MinimapTerrainTest {
    @Test void chunkCoordinatesRetainEveryColourAcrossNegativeBoundaries() {
        var terrain=new MinimapTerrain();
        for(int z=-33;z<=33;z++)for(int x=-33;x<=33;x++)assertTrue(terrain.put(x,z,0xFF000000|((x+33)<<8)|(z+33)));
        var reader=terrain.reader();
        for(int z=-33;z<=33;z++)for(int x=-33;x<=33;x++) {
            int expected=0xFF000000|((x+33)<<8)|(z+33);
            assertEquals(expected,terrain.get(x,z));assertEquals(expected,reader.applyAsInt(x,z));assertFalse(terrain.put(x,z,expected));
        }
        assertEquals(0,reader.applyAsInt(4000,-4000));
        terrain.put(4000,-4000,0xFF123456);assertEquals(0xFF123456,reader.applyAsInt(4000,-4000));
    }
    @Test void cachedColumnsRefreshAfterTwoSecondsAndAfterAWorldTickReset() {
        var terrain=new MinimapTerrain();
        assertTrue(terrain.shouldSample(-1,16,100));
        terrain.put(-1,16,0xFF123456);terrain.sampled(-1,16,100);
        assertFalse(terrain.shouldSample(-1,16,101));assertFalse(terrain.shouldSample(-1,16,139));
        assertTrue(terrain.shouldSample(-1,16,140));assertTrue(terrain.shouldSample(-1,16,1));
    }
    @Test void backgroundSaveKeepsTheExistingJsonCoordinateFormatAndStableChunkSnapshots() throws Exception {
        var terrain=new MinimapTerrain();terrain.put(-17,32,0xFF123456);terrain.put(16,-1,0xFF654321);
        var chunks=new HashMap<Long,int[]>();terrain.forEachChunk(chunks::put);
        terrain.put(-17,32,0xFF112233);
        assertEquals(0xFF123456,chunks.get(MinimapTerrain.key(-2,2))[15]);
        var buffer=new StringWriter();try(var writer=new JsonWriter(buffer)){terrain.write(writer);}
        Map<String,Integer> restored=new Gson().fromJson(buffer.toString(),new TypeToken<Map<String,Integer>>(){}.getType());
        assertEquals(Map.of("-17,32",0xFF112233,"16,-1",0xFF654321),restored);
    }
    @Test void streamingLoadRetainsOldCoordinateFilesWithoutMaterialisingAStringMap() throws Exception {
        String json="{\"-17,32\":-15584170,\"16,-1\":-10140895,\"bad\":9,\"4,5\":null,\"oops,1\":3}";
        MinimapTerrain terrain;
        try(var reader=new JsonReader(new StringReader(json))){terrain=MinimapTerrain.read(reader);}
        assertEquals(0xFF123456,terrain.get(-17,32));assertEquals(0xFF654321,terrain.get(16,-1));assertEquals(0,terrain.get(4,5));
    }
    @Test void backgroundLoadOnlyFillsMissingPixelsAndPreservesNewlyObservedTerrain() {
        var current=new MinimapTerrain();current.put(-17,32,0xFF112233);current.sampled(-17,32,50);
        var saved=new MinimapTerrain();saved.put(-17,32,0xFF123456);saved.put(16,-1,0xFF654321);
        current.mergeMissing(saved);
        assertEquals(0xFF112233,current.get(-17,32));assertEquals(0xFF654321,current.reader().applyAsInt(16,-1));
        assertFalse(current.shouldSample(-17,32,51));assertTrue(current.shouldSample(16,-1,51));
    }
    @Test void workerCanSaveWhileTheGameThreadDiscoversChunks() throws Exception {
        var terrain=new MinimapTerrain();
        try(var worker=Executors.newSingleThreadExecutor()) {
            var save=worker.submit(()->{for(int n=0;n<100;n++)terrain.forEachChunk((key,colors)->assertEquals(256,colors.length));});
            for(int x=-512;x<512;x++)terrain.put(x,x,0xFF123456);
            save.get(5,TimeUnit.SECONDS);
            for(int x=-512;x<512;x++)assertEquals(0xFF123456,terrain.get(x,x));
        }
    }
}
