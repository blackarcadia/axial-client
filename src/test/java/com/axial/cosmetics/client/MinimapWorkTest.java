package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MinimapWorkTest {
    @Test void disabledOrHiddenMapsDoNoPollingOrTerrainSampling() {
        assertFalse(MinimapWork.visible(false,false,false,false,false));
        assertFalse(MinimapWork.visible(false,true,true,false,false));
        assertFalse(MinimapWork.visible(false,true,false,true,false));
        assertFalse(MinimapWork.sample(false,false,128,false));
        assertFalse(MinimapWork.sample(false,true,7500,true));
    }
    @Test void fullscreenAndAnExplicitSettingsPreviewWorkWhenTheHudToggleIsOff() {
        assertTrue(MinimapWork.visible(true,false,false,true,false));
        assertTrue(MinimapWork.visible(false,false,false,true,true));
        assertTrue(MinimapWork.visible(false,true,false,false,false));
    }
    @Test void fullscreenOverviewUsesItsCompleteServerMapWhileNearbyDetailStillSamples() {
        assertFalse(MinimapWork.sample(true,true,7500,true));
        assertTrue(MinimapWork.sample(true,true,2048,true));
        assertTrue(MinimapWork.sample(true,true,7500,false));
        assertTrue(MinimapWork.sample(true,false,128,true));
    }
    @Test void oldPreviewTicksCannotEnableHiddenWorkAfterRespawn() {
        assertFalse(MinimapWork.recentPreview(1,1000));
        assertFalse(MinimapWork.recentPreview(10,Integer.MIN_VALUE));
        assertTrue(MinimapWork.recentPreview(100,100));
        assertTrue(MinimapWork.recentPreview(101,100));
        assertFalse(MinimapWork.recentPreview(102,100));
    }
}
