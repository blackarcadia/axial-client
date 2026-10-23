package com.axial.worldmap;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MapProtocolTest {
    @Test void fullReloadRequestsAreDistinctFromRegularIncrementalRequests() {
        assertTrue(MapProtocol.isRequest(new byte[]{0}));
        assertFalse(MapProtocol.requestsFullMap(new byte[]{0}));
        assertTrue(MapProtocol.isRequest(new byte[]{0,1}));
        assertTrue(MapProtocol.requestsFullMap(new byte[]{0,1}));
        for(byte[] invalid:List.of(new byte[0],new byte[]{1},new byte[]{0,0},new byte[]{0,2},new byte[]{0,1,1})) {
            assertFalse(MapProtocol.isRequest(invalid));assertFalse(MapProtocol.requestsFullMap(invalid));
        }
    }
    @Test void stripesStayWithinMessageLimitsAndReconstructEveryPixel() throws Exception {
        var g=MapGeometry.forBorder(0,0,16384,512);int[] pixels=new int[g.width()*g.height()];
        for(int i=0;i<pixels.length;i++)pixels[i]=0xFF000000|i;
        var map=new WorldMap(UUID.randomUUID(),g,1,pixels);int[] restored=new int[pixels.length];int rows=0;
        for(int start:TransferPlan.stripes(map,null)) {
            byte[] packet=MapProtocol.snapshot(map,start);assertTrue(packet.length<=MapProtocol.MAX_PACKET);
            try(var in=new DataInputStream(new ByteArrayInputStream(packet))) {
                assertEquals(1,in.readUnsignedByte());assertEquals(map.key(),in.readUTF());
                assertEquals(g.minX(),in.readInt());assertEquals(g.minZ(),in.readInt());assertEquals(g.step(),in.readInt());
                assertEquals(g.width(),in.readUnsignedShort());assertEquals(g.height(),in.readUnsignedShort());
                int first=in.readUnsignedShort(),count=in.readUnsignedShort();rows+=count;
                for(int i=first*g.width();i<(first+count)*g.width();i++)restored[i]=in.readInt();assertEquals(0,in.available());
            }
        }
        assertEquals(g.height(),rows);assertArrayEquals(pixels,restored);
    }
    @Test void preparedSnapshotsCarryTheirRevisionWithinThePacketLimit() throws Exception {
        var g=new MapGeometry(0,0,1,512,512);var map=new WorldMap(UUID.randomUUID(),g,987654321,new int[512*512]);
        int rows=0;
        for(int row:TransferPlan.stripes(map,null)) {
            byte[] packet=MapProtocol.completeSnapshot(map,row);assertTrue(packet.length<=MapProtocol.MAX_PACKET);
            try(var in=new DataInputStream(new ByteArrayInputStream(packet))) {
                assertEquals(4,in.readUnsignedByte());assertEquals(map.key(),in.readUTF());assertEquals(map.renderedAt(),in.readLong());
                in.skipNBytes(12);assertEquals(512,in.readUnsignedShort());assertEquals(512,in.readUnsignedShort());
                assertEquals(row,in.readUnsignedShort());int count=in.readUnsignedShort();rows+=count;assertEquals(count*512*4,in.available());
            }
        }
        assertEquals(512,rows);
    }
    @Test void unchangedMapsAreNotRetransmittedAndLocalChangesSendOnlyTheirStripe() {
        var g=new MapGeometry(0,0,16,512,512);var id=UUID.randomUUID();var first=new WorldMap(id,g,1,new int[512*512]);
        var same=new WorldMap(id,g,2,first.colors().clone());assertTrue(TransferPlan.stripes(same,first).isEmpty());
        var changed=first.colors().clone();changed[90*512+12]=0xFF123456;
        assertEquals(1,TransferPlan.stripes(new WorldMap(id,g,3,changed),first).size());
        assertTrue(TransferPlan.stripes(new WorldMap(UUID.randomUUID(),g,4,changed),first).size()>1);
    }
}
