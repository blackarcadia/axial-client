package com.axial.cosmetics.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class MinimapServerOverviewTest {
    @BeforeEach void reset(){MinimapServerOverview.beginWorldChange();}
    @Test void everyWorldReplacementRequestsAFullMapThenReturnsToIncrementalPolling() throws IOException {
        long generation=MinimapServerOverview.worldGeneration();
        assertArrayEquals(new byte[]{0,1},MinimapServerOverview.nextWorldRequest());
        assertArrayEquals(new byte[]{0},MinimapServerOverview.nextWorldRequest());
        MinimapServerOverview.receive(snapshot(4));
        MinimapServerOverview.receiveFullWorld(snapshot(4));
        assertTrue(MinimapServerOverview.available());
        MinimapServerOverview.beginWorldChange();
        assertEquals(generation+1,MinimapServerOverview.worldGeneration());
        assertFalse(MinimapServerOverview.available());
        assertNull(MinimapServerOverview.fullWorldBounds());
        assertEquals(0,MinimapServerOverview.colorAt(-20,30));
        assertArrayEquals(new byte[]{0,1},MinimapServerOverview.nextWorldRequest());
        // Returning to the same world name must still start a new complete transfer.
        MinimapServerOverview.receiveFullWorld(snapshot(4));
        MinimapServerOverview.beginWorldChange();
        assertArrayEquals(new byte[]{0,1},MinimapServerOverview.nextWorldRequest());
    }
    private byte[] prepared(long revision,int row,int a,int b) throws IOException {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
        out.writeByte(4);out.writeUTF("prepared-world");out.writeLong(revision);
        out.writeInt(-20);out.writeInt(30);out.writeInt(4);
        out.writeShort(2);out.writeShort(2);out.writeShort(row);out.writeShort(1);out.writeInt(a);out.writeInt(b);
        return bytes.toByteArray();
    }
    @Test void reusableTextureChangesOnlyOnACompleteNewServerSnapshot() throws IOException {
        MinimapServerOverview.receiveFullWorld(prepared(100,0,0xFF123456,0xFF654321));
        assertNull(MinimapServerOverview.textureSnapshot());
        MinimapServerOverview.receiveFullWorld(prepared(100,1,0xFF112233,0xFF445566));
        var first=MinimapServerOverview.textureSnapshot();assertNotNull(first);assertTrue(first.opaque);
        assertEquals(-20,first.minX);assertEquals(30,first.minZ);assertEquals(4,first.step);
        assertArrayEquals(new int[]{0xFF123456,0xFF654321,0xFF112233,0xFF445566},first.colors);
        MinimapServerOverview.receive(snapshot(4));assertSame(first,MinimapServerOverview.textureSnapshot());
        MinimapServerOverview.receiveFullWorld(prepared(200,0,0xFF998877,0xFF998877));assertSame(first,MinimapServerOverview.textureSnapshot());
        MinimapServerOverview.receiveFullWorld(prepared(200,1,0xFF998877,0xFF998877));
        assertNotSame(first,MinimapServerOverview.textureSnapshot());assertEquals(0xFF123456,first.colors[0]);
        MinimapServerOverview.beginWorldChange();assertNull(MinimapServerOverview.textureSnapshot());
    }
    @Test void mapsWithMissingServerPixelsKeepTheDetailedFallbackPath() throws IOException {
        MinimapServerOverview.receiveFullWorld(prepared(100,0,0xFF123456,0));
        MinimapServerOverview.receiveFullWorld(prepared(100,1,0,0));
        assertFalse(MinimapServerOverview.textureSnapshot().opaque);
    }
    private byte[] snapshot(int step) throws IOException {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
        out.writeByte(1);out.writeUTF("test-world");out.writeInt(-20);out.writeInt(30);out.writeInt(step);
        out.writeShort(2);out.writeShort(1);out.writeShort(0);out.writeShort(1);
        out.writeInt(0xFF123456);out.writeInt(0xFF654321);return bytes.toByteArray();
    }
    @Test void packetTypeIsConsumedBeforeWorldNameAndTerrainCoordinatesAreProjected() throws IOException {
        MinimapServerOverview.receive(snapshot(4));assertTrue(MinimapServerOverview.available());
        assertEquals(0xFF123456,MinimapServerOverview.colorAt(-20,30));
        assertEquals(0xFF123456,MinimapServerOverview.colorAt(-17,33));
        assertEquals(0xFF654321,MinimapServerOverview.colorAt(-16,30));
        assertEquals(0,MinimapServerOverview.colorAt(-21,30));
        assertEquals(0,MinimapServerOverview.colorAt(-12,30));
    }
    @Test void invalidAndTruncatedPacketsDoNotActivateAnOverview() throws IOException {
        MinimapServerOverview.receive(snapshot(0));assertFalse(MinimapServerOverview.available());
        byte[] valid=snapshot(4);MinimapServerOverview.receive(Arrays.copyOf(valid,valid.length-1));
        assertFalse(MinimapServerOverview.available());
        MinimapServerOverview.receive(new byte[]{1,0,20,1});assertFalse(MinimapServerOverview.available());
    }
}
