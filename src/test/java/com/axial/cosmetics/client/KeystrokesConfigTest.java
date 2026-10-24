package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class KeystrokesConfigTest {
    @TempDir Path directory;
    @Test void hudStartsOffWithBothMouseButtonsAvailable(){
        KeystrokesConfig.load(directory.resolve("missing.json"));assertFalse(KeystrokesConfig.enabled());assertTrue(KeystrokesConfig.mouse());assertEquals(1,KeystrokesConfig.scale());
    }
    @Test void preferencesAndDraggedPositionSurviveReload()throws Exception{
        Path file=directory.resolve("keystrokes.json");
        Files.writeString(file,"{\"enabled\":true,\"mouse\":false,\"stacked\":false,\"scale\":2,\"opacity\":0.5,\"textColor\":1193046,\"pressedColor\":6636321}");
        KeystrokesConfig.load(file);KeystrokesConfig.setPosition(120,45);KeystrokesConfig.save(file);KeystrokesConfig.load(file);
        assertTrue(KeystrokesConfig.enabled());assertFalse(KeystrokesConfig.mouse());assertFalse(KeystrokesConfig.stacked());
        assertEquals(140,KeystrokesConfig.width());assertEquals(92,KeystrokesConfig.height());
        assertEquals(120,KeystrokesConfig.x(640));assertEquals(45,KeystrokesConfig.y(480));
        assertEquals(0xFF123456,KeystrokesConfig.textColor());assertEquals(0xFF654321,KeystrokesConfig.pressedColor());assertEquals(.5f,KeystrokesConfig.opacity());
    }
    @Test void damagedFilesAndOversizedPositionsStaySafe()throws Exception{
        Path file=directory.resolve("keystrokes.json");Files.writeString(file,"{\"scale\":99,\"opacity\":-2,\"x\":99999,\"y\":99999}");
        KeystrokesConfig.load(file);assertEquals(3,KeystrokesConfig.scale());assertEquals(.2f,KeystrokesConfig.opacity());
        assertEquals(0,KeystrokesConfig.x(60));assertEquals(0,KeystrokesConfig.y(50));
        Files.writeString(file,"{bad");KeystrokesConfig.load(file);assertEquals(1,KeystrokesConfig.scale());assertFalse(KeystrokesConfig.enabled());
    }
    @Test void individualColorsAndVisibilitySurviveReloadAndResizeTheUnit()throws Exception{
        Path file=directory.resolve("individual.json");
        Files.writeString(file,"{\"enabled\":true,\"stacked\":false,\"visibleMask\":33,\"keyColors\":[16711680,65280,255,16776960,65535,16711935],\"scale\":2}");
        KeystrokesConfig.load(file);KeystrokesConfig.save(file);KeystrokesConfig.load(file);
        assertEquals(33,KeystrokesConfig.visibleMask());assertTrue(KeystrokesConfig.visible(0));assertTrue(KeystrokesConfig.visible(5));assertFalse(KeystrokesConfig.visible(4));
        assertEquals(68,KeystrokesConfig.width());assertEquals(88,KeystrokesConfig.height());
        int[] colors={0xFFFF0000,0xFF00FF00,0xFF0000FF,0xFFFFFF00,0xFF00FFFF,0xFFFF00FF};
        for(int i=0;i<6;i++)assertEquals(colors[i],KeystrokesConfig.textColor(i));
    }
    @Test void oldSharedColorAndMousePreferenceMigrateWithoutLosingChoices()throws Exception{
        Path file=directory.resolve("old.json");Files.writeString(file,"{\"mouse\":false,\"textColor\":1193046}");
        KeystrokesConfig.load(file);assertEquals(15,KeystrokesConfig.visibleMask());
        for(int i=0;i<6;i++)assertEquals(0xFF123456,KeystrokesConfig.textColor(i));
    }
    @Test void noVisibleKeysProducesAnEmptyHud()throws Exception{
        Path file=directory.resolve("empty.json");Files.writeString(file,"{\"visibleMask\":0}");
        KeystrokesConfig.load(file);assertEquals(0,KeystrokesConfig.width());assertEquals(0,KeystrokesConfig.height());assertFalse(KeystrokesConfig.mouse());
    }
}
