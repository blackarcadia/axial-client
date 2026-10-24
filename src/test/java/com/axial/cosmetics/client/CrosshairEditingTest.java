package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class CrosshairEditingTest {
    @TempDir Path dir;
    @Test void oldConfigsLoadAndOnePixelEditsSurviveRestart()throws Exception{
        Path path=dir.resolve("crosshair.json");Files.writeString(path,"{\"style\":\"SMALL_DOT\",\"width\":1,\"length\":3,\"gap\":1,\"color\":-16711936}");
        var c=CrosshairConfigManager.load(path);assertEquals(1,c.width);assertEquals(3,c.length);assertEquals(1,c.gap);assertEquals(0xFF00FF00,c.color);
        assertEquals(1,c.opacity);assertEquals(1,c.outlineThickness);
        c.centerDot=true;c.dotSize=3;c.outlineThickness=2;c.opacity=.6f;
        CrosshairConfigManager.save(path);c=CrosshairConfigManager.load(path);
        assertTrue(c.centerDot);assertEquals(3,c.dotSize);assertEquals(2,c.outlineThickness);assertEquals(.6f,c.opacity);assertEquals(1,c.width);
    }
    @Test void malformedAndUnknownConfigurationsRecoverSafely()throws Exception{
        Path path=dir.resolve("crosshair.json");Files.writeString(path,"{\"style\":\"REMOVED\",\"width\":-2,\"size\":999,\"opacity\":0}");
        var c=CrosshairConfigManager.load(path);assertNotNull(c.style);assertEquals(1,c.width);assertEquals(4,c.size);assertEquals(.1f,c.opacity);
        Files.writeString(path,"{invalid");assertNotNull(CrosshairConfigManager.load(path).style);
    }
    @Test void templatesPreservePersonalColorsAndUndoRestoresEverySetting(){
        var c=new CrosshairConfig();c.enabled=false;c.color=0xFF123456;c.outlineColor=0xFFABCDEF;c.opacity=.7f;c.dynamicEnabled=true;c.size=2;c.centerDot=true;c.dotSize=5;
        var history=new CrosshairHistory();history.checkpoint(c);
        var preset=CrosshairPresets.templates().get(9);CrosshairPresets.applyTemplate(c,preset);
        assertEquals(0xFF123456,c.color);assertEquals(0xFFABCDEF,c.outlineColor);assertEquals(.7f,c.opacity);assertTrue(c.dynamicEnabled);assertFalse(c.enabled);
        assertTrue(CrosshairPresets.matches(c,preset));history.undo(c);
        assertEquals(2,c.size);assertTrue(c.centerDot);assertEquals(5,c.dotSize);assertFalse(c.enabled);assertFalse(history.available());
    }
    @Test void undoHistoryIsBoundedAndSnapshotsDoNotShareMutableState(){
        var c=new CrosshairConfig();var history=new CrosshairHistory();
        for(int i=0;i<40;i++){c.length=i;history.checkpoint(c);}
        c.length=99;for(int i=39;i>=8;i--){history.undo(c);assertEquals(i,c.length);}
        assertFalse(history.available());
    }
    @Test void dynamicPulseSettlesWithoutChangingTheSavedGeometry(){
        assertEquals(1,CrosshairDynamicState.atElapsedMillis(0));assertTrue(CrosshairDynamicState.atElapsedMillis(110)>1);
        assertEquals(1,CrosshairDynamicState.atElapsedMillis(220));assertEquals(1,CrosshairDynamicState.atElapsedMillis(-1));
    }
}
