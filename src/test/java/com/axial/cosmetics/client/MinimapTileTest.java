package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class MinimapTileTest {
    @Test void closeZoomPreservesTextureAndUsesWorldAlignedCoordinatesAcrossNegativeBlocks() {
        int[] pixels=new int[256];
        for(int z=0;z<16;z++)for(int x=0;x<16;x++)pixels[z*16+x]=0xFF000000|(x*16<<16)|(z*16);
        var tile=new MinimapTile(pixels);
        assertEquals(0xFF400080,tile.sample(.25,.5,0,0xFFFFFF,1));
        assertEquals(tile.sample(.25,.5,0,0xFFFFFF,1),tile.sample(-.75,-1.5,0,0xFFFFFF,1));
        assertNotEquals(tile.sample(.25,.5,0,0xFFFFFF,1),tile.sample(.75,.5,0,0xFFFFFF,1));
    }
    @Test void distantPixelsAverageTextureInsteadOfAliasingItsPattern() {
        int[] pixels=new int[256];
        for(int z=0;z<16;z++)for(int x=0;x<16;x++)pixels[z*16+x]=(x+z)%2==0?0xFFFFFFFF:0xFF000000;
        var tile=new MinimapTile(pixels);
        assertEquals(0,MinimapTile.levelFor(1.0/16));
        assertEquals(1,MinimapTile.levelFor(1.0/8));
        assertEquals(4,MinimapTile.levelFor(1));
        assertEquals(4,MinimapTile.levelFor(1000));
        assertEquals(0xFF7F7F7F,tile.sample(.1,.2,4,0xFFFFFF,1));
        assertEquals(tile.sample(.1,.2,4,0xFFFFFF,1),tile.sample(.8,.9,4,0xFFFFFF,1));
    }
    @Test void biomeTintAndHeightShadingApplyToEveryTextureLevel() {
        int[] pixels=new int[256];Arrays.fill(pixels,0xFFFFFFFF);
        var tile=new MinimapTile(pixels);
        assertEquals(0xFF408020,tile.sample(.5,.5,0,0x80FF40,.5));
        assertEquals(0xFF408020,tile.average(0x80FF40,.5));
        assertEquals(0xFFFFFFFF,tile.average(0xFFFFFF,1.16));
    }
    @Test void transparentCutoutsDoNotCreateBlackOrTransparentMapHoles() {
        int[] pixels=new int[256];pixels[0]=0xFF228844;
        var tile=new MinimapTile(pixels);
        assertEquals(0xFF228844,tile.sample(.5,.5,0,0xFFFFFF,1));
        assertEquals(0xFF228844,tile.average(0xFFFFFF,1));
    }
}
