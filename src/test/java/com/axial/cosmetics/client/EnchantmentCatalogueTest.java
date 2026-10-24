package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class EnchantmentCatalogueTest {
    private byte[] fixture()throws IOException{
        try(var in=getClass().getResourceAsStream("/enchantments/prisonscore-catalogue.gz")){return Objects.requireNonNull(in).readAllBytes();}
    }
    private byte[] packet(byte[] data,long revision,int index)throws IOException{
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
        out.writeByte(1);out.writeLong(revision);out.writeInt(data.length);out.writeShort(index);out.writeShort((data.length+27999)/28000);
        out.write(data,index*28000,Math.min(28000,data.length-index*28000));return bytes.toByteArray();
    }
    @Test void actualServerEncoderExportsBothCompleteRegistriesAndSpecialApplicability()throws Exception{
        var entries=EnchantmentCatalogue.decode(fixture());assertEquals(99,entries.size());
        assertEquals(33,entries.stream().filter(e->e.key().startsWith("mining:")).count());
        assertEquals(66,entries.stream().filter(e->e.key().startsWith("gear:")).count());
        var buster=entries.stream().filter(e->e.key().equals("mining:buster_efficiency")).findFirst().orElseThrow();
        assertEquals(List.of("CELL_BUSTER"),buster.targets());
        for(var entry:entries){assertTrue(entry.maxLevel()>0);assertFalse(entry.name().contains("&l"));assertFalse(entry.description().isBlank());assertFalse(entry.rarity().isBlank());}
        assertTrue(entries.stream().anyMatch(e->e.targets().contains("ARMOR")));
        assertTrue(entries.stream().anyMatch(e->e.targets().contains("SPEAR")));
    }
    @Test void updatedRevisionReplacesRatherThanAppendsAndAckKeepsTheSnapshot()throws Exception{
        var catalogue=new EnchantmentCatalogue();catalogue.receive(packet(fixture(),1,0));var before=catalogue.entries;
        catalogue.receive(new byte[]{2,0,0,0,0,0,0,0,1});assertSame(before,catalogue.entries);
        catalogue.receive(packet(fixture(),2,0));assertEquals(99,catalogue.entries.size());assertEquals(2,catalogue.revision);
        var newConnection=new EnchantmentCatalogue();assertTrue(newConnection.entries.isEmpty());assertEquals(0,newConnection.revision);
    }
    @Test void incompleteOrMalformedRefreshCannotDestroyTheLastGoodCatalogue()throws Exception{
        var catalogue=new EnchantmentCatalogue();catalogue.receive(packet(fixture(),1,0));var good=catalogue.entries;
        byte[] large=new byte[40000];new Random(42).nextBytes(large);
        catalogue.receive(packet(large,2,0));assertSame(good,catalogue.entries);assertEquals(1,catalogue.revision);
        assertThrows(IOException.class,()->catalogue.receive(packet(large,2,1)));assertSame(good,catalogue.entries);
        byte[] truncated=Arrays.copyOf(packet(fixture(),3,0),20);
        assertThrows(IOException.class,()->catalogue.receive(truncated));assertSame(good,catalogue.entries);
        catalogue.receive(packet(fixture(),4,0));assertEquals(4,catalogue.revision);
    }
    @Test void multipartTransferCanArriveOutOfOrder()throws Exception{
        var raw=new ByteArrayOutputStream();var out=new DataOutputStream(raw);out.writeInt(300);
        var random=new Random(4);
        for(int i=0;i<300;i++){
            byte[] desc=new byte[250];random.nextBytes(desc);
            out.writeUTF("gear:"+i);out.writeUTF("Enchant "+i);out.writeInt(10);out.writeUTF("RARE");out.writeInt(3);
            out.writeUTF(Base64.getEncoder().encodeToString(desc));out.writeInt(1);out.writeUTF("SWORD");
        }
        var zipped=new ByteArrayOutputStream();try(var gzip=new GZIPOutputStream(zipped)){gzip.write(raw.toByteArray());}
        byte[] data=zipped.toByteArray();assertTrue(data.length>28000);
        var catalogue=new EnchantmentCatalogue();int count=(data.length+27999)/28000;
        for(int i=count-1;i>=0;i--)catalogue.receive(packet(data,5,i));
        assertEquals(300,catalogue.entries.size());assertEquals(5,catalogue.revision);
    }
    @Test void decompressionLimitAndInvalidEntryCountsAreRejected()throws Exception{
        var compressed=new ByteArrayOutputStream();try(var gzip=new GZIPOutputStream(compressed)){gzip.write(new byte[8_000_001]);}
        assertThrows(IOException.class,()->EnchantmentCatalogue.decode(compressed.toByteArray()));
        compressed.reset();try(var gzip=new GZIPOutputStream(compressed)){gzip.write(new byte[]{0,0,32,0});}
        assertThrows(IOException.class,()->EnchantmentCatalogue.decode(compressed.toByteArray()));
    }
}
