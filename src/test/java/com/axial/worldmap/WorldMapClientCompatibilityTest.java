package com.axial.worldmap;

import com.axial.cosmetics.client.MinimapServerOverview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the real plugin encoder through the real Fabric client decoder. */
class WorldMapClientCompatibilityTest {
    @BeforeEach void reset(){MinimapServerOverview.beginWorldChange();}
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
}
