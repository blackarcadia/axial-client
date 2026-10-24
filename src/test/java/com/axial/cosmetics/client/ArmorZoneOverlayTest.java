package com.axial.cosmetics.client;

import org.junit.jupiter.api.*;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ArmorZoneOverlayTest {
    @BeforeEach void reset(){MinimapServerOverview.beginWorldChange();}
    private byte[] fixture() throws IOException {
        try(var input=getClass().getResourceAsStream("/minimap/armor-zones-prisons.bin")){assertNotNull(input);return input.readAllBytes();}
    }
    private byte[] terrain(String world) throws IOException {
        var bytes=new ByteArrayOutputStream();try(var out=new DataOutputStream(bytes)){
            out.writeByte(1);out.writeUTF(world);out.writeInt(0);out.writeInt(0);out.writeInt(1);
            out.writeShort(1);out.writeShort(1);out.writeShort(0);out.writeShort(1);out.writeInt(0xFF123456);
        }return bytes.toByteArray();
    }
    @Test void realServerPacketCoversTheReportedDistantPositionsAndAllFourTierColours() throws Exception {
        var data=ArmorZoneOverlay.read(fixture());assertEquals("prisons",data.world());assertEquals(4,data.regions().size());
        assertEquals("Gold Zone",data.zoneAt(2000,2000));assertEquals("Diamond Zone",data.zoneAt(-2000,-2000));assertEquals("Iron Zone",data.zoneAt(1000,-1000));
        assertEquals("Chain Zone",data.zoneAt(-86,-81));assertEquals("Chain Zone",data.zoneAt(-12,376));
        var expected=Map.of("Chain Zone",0xFF555555,"Diamond Zone",0xFF80D8FF,"Gold Zone",0xFFFFD54F,"Iron Zone",0xFFFFFFFF);
        for(var region:data.regions())assertEquals(expected.get(region.name()).intValue(),region.color());
        for(double x:new double[]{-3749,3749})for(double z:new double[]{-3749,3749})assertNotNull(data.zoneAt(x,z));
        assertNull(data.zoneAt(3751,3751));
    }
    @Test void failedPacketsCannotReplaceValidOutlinesAndWorldChangesDiscardThem() throws Exception {
        MinimapServerOverview.receive(terrain("prisons"));MinimapServerOverview.receive(fixture());
        var valid=MinimapServerOverview.armorForActiveWorld();assertNotNull(valid);
        byte[] packet=fixture();MinimapServerOverview.receive(Arrays.copyOf(packet,packet.length-1));
        assertSame(valid,MinimapServerOverview.armorForActiveWorld());
        MinimapServerOverview.beginWorldChange();assertNull(MinimapServerOverview.armorForActiveWorld());
        MinimapServerOverview.receive(terrain("different-world"));assertNull(MinimapServerOverview.armorForActiveWorld());
    }
    @Test void earlyGeometryWaitsForItsMatchingWorldIdentity() throws Exception {
        MinimapServerOverview.receive(fixture());assertNull(MinimapServerOverview.armorForActiveWorld());
        MinimapServerOverview.receive(terrain("different-world"));assertNull(MinimapServerOverview.armorForActiveWorld());
        MinimapServerOverview.receive(terrain("prisons"));assertNotNull(MinimapServerOverview.armorForActiveWorld());
    }
    @Test void truncatedOversizedAndNonfiniteContoursAreRejected() throws Exception {
        assertThrows(IOException.class,()->ArmorZoneOverlay.read(new byte[30_001]));
        assertThrows(IOException.class,()->ArmorZoneOverlay.read(new byte[]{3,0,50,1}));
        byte[] packet=fixture();assertThrows(IOException.class,()->ArmorZoneOverlay.read(Arrays.copyOf(packet,packet.length-1)));
        var bytes=new ByteArrayOutputStream();try(var out=new DataOutputStream(bytes)){
            out.writeByte(3);out.writeUTF("prisons");out.writeShort(1);out.writeUTF("Chain Zone");out.writeInt(0xFF555555);
            out.writeDouble(Double.NaN);out.writeDouble(0);out.writeShort(0);
        }
        assertThrows(IOException.class,()->ArmorZoneOverlay.read(bytes.toByteArray()));
    }
}
