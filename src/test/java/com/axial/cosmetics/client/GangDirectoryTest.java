package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class GangDirectoryTest {
    private byte[] fixture()throws IOException{try(var in=getClass().getResourceAsStream("/gangs/prisonscore-directory.gz")){assertNotNull(in);return in.readAllBytes();}}
    private byte[] packet(byte[] data,long revision,int index)throws IOException{
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeByte(1);out.writeLong(revision);out.writeInt(data.length);out.writeShort(index);out.writeShort((data.length+27999)/28000);out.write(data,index*28000,Math.min(28000,data.length-index*28000));return bytes.toByteArray();
    }
    @Test void realPrisonsCoreEncoderProvidesRanksLeaderMembersPointsAndCombinedWealth()throws Exception{
        var gangs=GangDirectory.decode(fixture());assertEquals(2,gangs.size());
        assertEquals("Alpha",gangs.getFirst().name());assertEquals(1,gangs.getFirst().rank());assertEquals(9000,gangs.getFirst().points());
        assertEquals("Beta",gangs.get(1).name());assertEquals(2,gangs.get(1).rank());assertEquals(125,gangs.get(1).points());
        var gang=gangs.getFirst();assertEquals(new BigDecimal("350.35"),gang.wealth());assertEquals(2,gang.members().size());
        assertEquals("LeaderName",gang.leaderName());assertEquals(gang.leader(),gang.members().getFirst().id());assertEquals("LEADER",gang.members().getFirst().role());
        assertTrue(gang.members().getFirst().online());assertEquals("OfflineMember",gang.members().get(1).name());assertFalse(gang.members().get(1).online());
    }
    @Test void updatesReplaceSnapshotsAndMalformedTransfersCannotDestroyGoodData()throws Exception{
        var directory=new GangDirectory();directory.receive(packet(fixture(),1,0));var before=directory.gangs;
        directory.receive(new byte[]{2,0,0,0,0,0,0,0,1});assertSame(before,directory.gangs);
        byte[] truncated=Arrays.copyOf(packet(fixture(),2,0),25);assertThrows(IOException.class,()->directory.receive(truncated));assertSame(before,directory.gangs);
        directory.receive(packet(fixture(),3,0));assertEquals(2,directory.gangs.size());assertEquals(3,directory.revision);
        assertEquals(0,new GangDirectory().revision);assertTrue(new GangDirectory().gangs.isEmpty());
    }
    @Test void incompleteAndCorruptFragmentsKeepThePreviousSnapshot()throws Exception{
        var directory=new GangDirectory();directory.receive(packet(fixture(),1,0));var good=directory.gangs;
        byte[] corrupt=new byte[40000];new Random(9).nextBytes(corrupt);
        directory.receive(packet(corrupt,2,0));assertSame(good,directory.gangs);
        assertThrows(IOException.class,()->directory.receive(packet(corrupt,2,1)));assertSame(good,directory.gangs);
        directory.receive(packet(fixture(),4,0));assertEquals(4,directory.revision);
    }
    private byte[] data(int count,boolean duplicateMembers,boolean missingMoney)throws IOException{
        var bytes=new ByteArrayOutputStream();var random=new Random(5);
        try(var out=new DataOutputStream(new GZIPOutputStream(bytes))){
            out.writeInt(count);
            for(int i=0;i<count;i++){
                out.writeUTF("id"+i);out.writeUTF("Gang "+i);out.writeInt(i+1);out.writeInt(1000-i);out.writeBoolean(true);out.writeLong(0);out.writeLong(i+1);
                out.writeBoolean(!missingMoney);if(!missingMoney)out.writeUTF("123.45");out.writeInt(2);
                for(int m=0;m<2;m++){
                    out.writeLong(0);out.writeLong(i+1+(duplicateMembers?0:m*10000));byte[] name=new byte[50];random.nextBytes(name);out.writeUTF(Base64.getEncoder().encodeToString(name));out.writeUTF(m==0?"LEADER":"MEMBER");out.writeBoolean(m==0);
                }
            }
        }
        return bytes.toByteArray();
    }
    @Test void multipleFragmentsCanArriveOutOfOrder()throws Exception{
        byte[] compressed=data(500,false,false);assertTrue(compressed.length>28000);var directory=new GangDirectory();
        for(int i=(compressed.length+27999)/28000-1;i>=0;i--)directory.receive(packet(compressed,7,i));
        assertEquals(500,directory.gangs.size());assertEquals(500,directory.gangs.getLast().rank());assertEquals(7,directory.revision);
    }
    @Test void unavailableBalancesAreDistinctFromZeroAndDuplicateMembersAreRejected()throws Exception{
        assertNull(GangDirectory.decode(data(1,false,true)).getFirst().wealth());assertThrows(IOException.class,()->GangDirectory.decode(data(1,true,false)));
    }
    @Test void compressionAndCountLimitsRejectOversizedOrInvalidDirectories()throws Exception{
        var compressed=new ByteArrayOutputStream();try(var gzip=new GZIPOutputStream(compressed)){gzip.write(new byte[8_000_001]);}
        assertThrows(IOException.class,()->GangDirectory.decode(compressed.toByteArray()));
        compressed.reset();try(var out=new DataOutputStream(new GZIPOutputStream(compressed))){out.writeInt(4097);}
        assertThrows(IOException.class,()->GangDirectory.decode(compressed.toByteArray()));
    }
}
