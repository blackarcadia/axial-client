package com.axial.cosmetics.client;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ChromaColorTest {
    @TempDir Path directory;
    @Test void modeRetainsItsSolidColourAndProducesASeamlessRainbow() {
        int solid=0xFF123456,encoded=ChromaColor.withChroma(solid,true);
        assertTrue(ChromaColor.enabled(encoded));assertEquals(encoded,ChromaColor.opaque(encoded));
        assertEquals(solid,ChromaColor.withChroma(encoded,false));
        assertFalse(ChromaColor.enabled(solid));assertEquals(solid,ChromaColor.resolve(solid,1234));
        assertEquals(ChromaColor.resolve(encoded,0),ChromaColor.resolve(encoded,6000));
        assertEquals(ChromaColor.resolve(encoded,-1),ChromaColor.resolve(encoded,5999));
        var colors=new java.util.HashSet<Integer>();
        for(long t=0;t<6000;t+=1000){int color=ChromaColor.resolve(encoded,t);assertEquals(255,color>>>24);colors.add(color);}
        assertEquals(6,colors.size());assertEquals(encoded,ChromaColor.withChroma(solid,true));
        assertEquals(0x80123456,ChromaColor.resolve(0x80123456,1000));
    }
    @Test void crosshairAndOutlineModesPersistIndependently() throws Exception {
        Path file=directory.resolve("crosshair.json");
        int encoded=ChromaColor.withChroma(0xFF334455,true);
        Files.writeString(file,"{\"color\":"+encoded+",\"outlineColor\":-16777216}");
        var config=CrosshairConfigManager.load(file);CrosshairConfigManager.save(file);config=CrosshairConfigManager.load(file);
        assertEquals(encoded,config.color);assertEquals(0xFF000000,config.outlineColor);
        config.color=ChromaColor.withChroma(config.color,false);CrosshairConfigManager.save(file);
        assertEquals(0xFF334455,CrosshairConfigManager.load(file).color);
    }
    @Test void fpsAndIndividualKeystrokePreferencesSurviveReload() throws Exception {
        int encoded=ChromaColor.withChroma(0xFFABCDEF,true);
        Path fps=directory.resolve("fps.json"),keys=directory.resolve("keys.json");
        Files.writeString(fps,"{\"fpsColor\":"+encoded+"}");
        PerformanceConfig.load(fps);PerformanceConfig.save(fps);PerformanceConfig.load(fps);assertEquals(encoded,PerformanceConfig.fpsColor());
        Files.writeString(keys,"{\"pressedColor\":"+encoded+",\"keyColors\":["+encoded+",-1]}");
        KeystrokesConfig.load(keys);KeystrokesConfig.save(keys);KeystrokesConfig.load(keys);
        assertEquals(encoded,KeystrokesConfig.textColor(0));assertEquals(-1,KeystrokesConfig.textColor(1));assertEquals(encoded,KeystrokesConfig.pressedColor());
    }
    @Test void waypointModeAndFallbackColourPersist() {
        int encoded=ChromaColor.withChroma(0xFF112233,true);
        Path file=directory.resolve("waypoints.json");var store=new WaypointStore(file);
        store.add(new WaypointConfig.Entry("Rainbow","server:test","minecraft:overworld",0,64,0,encoded));
        assertEquals(encoded,new WaypointStore(file).entries().getFirst().color());
    }
}
