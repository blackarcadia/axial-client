package com.axial.worldmap;

import com.axial.cosmetics.client.MinimapServerOverview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the real plugin encoder through the real Fabric client decoder. */
class WorldMapClientCompatibilityTest {
    @BeforeEach void reset(){MinimapServerOverview.beginWorldChange();}
    @Test void serverReportsRenderingProgressInsteadOfSilentlyShowingAFallback() throws Exception {
        MinimapServerOverview.receiveFullWorld(MapProtocol.status("Rendering full-world map",50,100));
        assertEquals("Rendering full-world map (50%)",MinimapServerOverview.loadingMessage());
        assertFalse(MinimapServerOverview.available());
        var g=new MapGeometry(0,0,1,512,512);var colors=new int[512*512];Arrays.fill(colors,0xFF123456);
        var map=new WorldMap(UUID.randomUUID(),g,1,colors);
        var stripes=TransferPlan.stripes(map,null);
        MinimapServerOverview.receiveFullWorld(MapProtocol.snapshot(map,stripes.removeFirst()));
        assertEquals("Rendering full-world map (50%)",MinimapServerOverview.loadingMessage());
        for(int row:stripes)MinimapServerOverview.receiveFullWorld(MapProtocol.snapshot(map,row));
        assertEquals("Rendering full-world map (50%)",MinimapServerOverview.loadingMessage());
        MinimapServerOverview.receiveFullWorld(MapProtocol.status("Full map ready",0,0));
        assertEquals("",MinimapServerOverview.loadingMessage());
        MinimapServerOverview.beginWorldChange();
        assertEquals("Waiting for full-world map...",MinimapServerOverview.loadingMessage());
    }
    @Test void ineligibleWorldStatusExplainsWhyAWholeWorldMapIsUnavailable() throws Exception {
        MinimapServerOverview.receiveFullWorld(MapProtocol.status("World border exceeds the full-map limit",0,0));
        assertEquals("World border exceeds the full-map limit",MinimapServerOverview.loadingMessage());
        MinimapServerOverview.receiveFullWorld(new byte[]{3,0,5,1});
        assertEquals("World border exceeds the full-map limit",MinimapServerOverview.loadingMessage());
        assertNull(MinimapServerOverview.fullWorldBounds());
    }
    @Test void clientReceivesTheWholeBorderAndLegacyPrisonsMapsCannotReplaceIt() throws Exception {
        var g=MapGeometry.forBorder(-500,300,65536,512);int[] colors=new int[g.width()*g.height()];
        for(int i=0;i<colors.length;i++)colors[i]=0xFF000000|i;
        var map=new WorldMap(UUID.randomUUID(),g,1,colors);
        for(int row:TransferPlan.stripes(map,null))MinimapServerOverview.receiveFullWorld(MapProtocol.snapshot(map,row));
        for(int i=0;i<colors.length;i++)assertEquals(colors[i],MinimapServerOverview.colorAt(g.x(i),g.z(i)));
        var bounds=MinimapServerOverview.fullWorldBounds();assertNotNull(bounds);assertEquals(-500,bounds.centerX());assertEquals(300,bounds.centerZ());assertEquals(256,bounds.scale());
        var old=new WorldMap(UUID.randomUUID(),new MapGeometry(-1000,0,4,256,256),1,new int[256*256]);
        for(int row:TransferPlan.stripes(old,null))MinimapServerOverview.receive(MapProtocol.snapshot(old,row));
        assertEquals(colors[0],MinimapServerOverview.colorAt(g.minX(),g.minZ()));assertEquals(map.key(),MinimapServerOverview.fullWorldBounds().world());
        MinimapServerOverview.beginWorldChange();assertNull(MinimapServerOverview.fullWorldBounds());assertFalse(MinimapServerOverview.available());
    }
    @Test void malformedFullWorldPacketsDoNotReplaceValidTerrain() throws Exception {
        var g=new MapGeometry(-10,-10,1,16,16);var colors=new int[256];Arrays.fill(colors,0xFF123456);
        var map=new WorldMap(UUID.randomUUID(),g,1,colors);
        MinimapServerOverview.receiveFullWorld(MapProtocol.snapshot(map,0));
        MinimapServerOverview.receiveFullWorld(new byte[]{1,0,5,1});
        assertEquals(0xFF123456,MinimapServerOverview.colorAt(-10,-10));
    }
    @Test void progressiveMapsPublishEarlyAndIncrementalStripesEventuallyFillTheWholeBorder() throws Exception {
        var g=new MapGeometry(0,0,1,512,512);var id=UUID.randomUUID();
        int[] legacy=new int[512*512];Arrays.fill(legacy,0xFF445566);
        var fallback=new WorldMap(id,g,1,legacy);
        for(int row:TransferPlan.stripes(fallback,null))MinimapServerOverview.receive(MapProtocol.snapshot(fallback,row));
        int[] pixels=new int[512*512];pixels[0]=0xFF123456;
        var coverage=new BitSet();coverage.set(0);
        var partial=new WorldMap(id,g,2,pixels.clone(),coverage);
        for(int row:TransferPlan.stripes(partial,null))MinimapServerOverview.receiveFullWorld(MapProtocol.snapshot(partial,row));
        assertNotNull(MinimapServerOverview.fullWorldBounds());
        assertEquals(0xFF123456,MinimapServerOverview.colorAt(0,0));
        assertEquals(0xFF445566,MinimapServerOverview.colorAt(511,511));
        for(int i=0;i<pixels.length;i++)pixels[i]=0xFF000000|i;
        var completed=new WorldMap(id,g,3,pixels);
        for(int row:TransferPlan.stripes(completed,partial))MinimapServerOverview.receiveFullWorld(MapProtocol.snapshot(completed,row));
        for(int i=0;i<pixels.length;i++)assertEquals(pixels[i],MinimapServerOverview.colorAt(g.x(i),g.z(i)));
    }
    @Test void completeReloadRestoresAllRowsAfterLeavingAndRejoiningTheSameWorld() throws Exception {
        var g=new MapGeometry(-256,-256,1,512,512);
        int[] colors=new int[g.width()*g.height()];
        for(int i=0;i<colors.length;i++)colors[i]=0xFF000000|i;
        var map=new WorldMap(UUID.randomUUID(),g,1,colors);
        for(int start:TransferPlan.stripes(map,null))MinimapServerOverview.receiveFullWorld(MapProtocol.snapshot(map,start));
        MinimapServerOverview.beginWorldChange();
        assertFalse(MinimapServerOverview.available());
        // A full request discards the server's old sent-map baseline instead of diffing it.
        assertTrue(MapProtocol.requestsFullMap(new byte[]{0,1}));
        assertTrue(TransferPlan.stripes(map,map).isEmpty());
        for(int start:TransferPlan.stripes(map,null))MinimapServerOverview.receiveFullWorld(MapProtocol.snapshot(map,start));
        for(int i=0;i<colors.length;i++)assertEquals(colors[i],MinimapServerOverview.colorAt(g.x(i),g.z(i)));
        assertEquals(map.key(),MinimapServerOverview.fullWorldBounds().world());
    }
}
