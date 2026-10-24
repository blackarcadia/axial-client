package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ScoreboardHudConfigTest {
    @TempDir Path directory;
    @Test void untouchedPlayersKeepVanillaPlacementAndDraggedCoordinatesSurviveRestart(){
        Path file=directory.resolve("scoreboard.json");ScoreboardHudConfig.load(file);
        assertEquals(480,ScoreboardHudConfig.x(480,640,150));assertEquals(110,ScoreboardHudConfig.y(110,360,100));
        ScoreboardHudConfig.setPosition(35,62);ScoreboardHudConfig.save(file);
        ScoreboardHudConfig.load(directory.resolve("other.json"));assertEquals(480,ScoreboardHudConfig.x(480,640,150));
        ScoreboardHudConfig.load(file);assertEquals(35,ScoreboardHudConfig.x(480,640,150));assertEquals(62,ScoreboardHudConfig.y(110,360,100));
    }
    @Test void windowResizeAndGrowingScoreboardClampDisplayWithoutLosingSavedPosition(){
        ScoreboardHudConfig.setPosition(500,240);
        assertEquals(170,ScoreboardHudConfig.x(0,320,150));assertEquals(120,ScoreboardHudConfig.y(0,220,100));
        assertEquals(0,ScoreboardHudConfig.x(0,100,150));assertEquals(0,ScoreboardHudConfig.y(0,80,100));
        assertEquals(500,ScoreboardHudConfig.x(0,1000,150));assertEquals(240,ScoreboardHudConfig.y(0,600,100));
    }
    @Test void invalidOrPartialSettingsFallBackToVanilla()throws Exception{
        Path file=directory.resolve("scoreboard.json");
        for(String json:new String[]{"{bad","null","{}","{\"x\":20}","{\"x\":-8,\"y\":40}"}){
            Files.writeString(file,json);ScoreboardHudConfig.load(file);
            assertEquals(480,ScoreboardHudConfig.x(480,640,150));assertEquals(110,ScoreboardHudConfig.y(110,360,100));
        }
    }
    @Test void dragHitAreaUsesTheEntireMeasuredSidebarWithExclusiveEdges(){
        var bounds=new ScoreboardHud.Bounds(40,60,190,145);
        assertTrue(bounds.contains(40,60));assertTrue(bounds.contains(229.9,204.9));
        assertFalse(bounds.contains(230,100));assertFalse(bounds.contains(50,205));assertFalse(bounds.contains(39.9,70));
    }
}
