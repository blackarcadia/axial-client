package com.axial.cosmetics.client;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Bounded, atomic assembly: an incomplete transfer never replaces the current guide. */
final class EnchantmentCatalogue {
    record Entry(String key,String name,int maxLevel,String rarity,int rank,String description,List<String> targets) {}
    List<Entry> entries=List.of();
    long revision;
    private long pendingRevision;
    private int size;
    private byte[][] parts;
    void receive(byte[] packet)throws IOException {
        if(packet.length>28017||packet.length<9)throw new IOException("Invalid catalogue packet");
        var in=new DataInputStream(new ByteArrayInputStream(packet));int type=in.readUnsignedByte();long rev=in.readLong();
        if(type==2){if(in.available()!=0||rev!=revision)throw new IOException("Invalid acknowledgement");return;}
        if(type!=1)throw new IOException("Unknown catalogue packet");
        int total=in.readInt(),index=in.readUnsignedShort(),count=in.readUnsignedShort();
        if(total<1||total>2_000_000||count!=(total+27999)/28000||index>=count||in.available()!=Math.min(28000,total-index*28000))throw new IOException("Invalid catalogue fragment");
        if(parts==null||pendingRevision!=rev){pendingRevision=rev;size=total;parts=new byte[count][];}
        if(size!=total||parts.length!=count)throw new IOException("Mismatched catalogue fragments");
        parts[index]=in.readAllBytes();for(byte[] part:parts)if(part==null)return;
        var bytes=new ByteArrayOutputStream(size);for(byte[] part:parts)bytes.write(part);
        List<Entry> decoded=decode(bytes.toByteArray());
        entries=decoded;revision=rev;parts=null;
    }
    static List<Entry> decode(byte[] compressed)throws IOException {
        byte[] raw;
        try(var gzip=new GZIPInputStream(new ByteArrayInputStream(compressed))){raw=gzip.readNBytes(8_000_001);}
        if(raw.length>8_000_000)throw new IOException("Catalogue exceeds limit");
        var in=new DataInputStream(new ByteArrayInputStream(raw));int count=in.readInt();
        if(count<0||count>4096)throw new IOException("Too many enchantments");
        var result=new ArrayList<Entry>();var keys=new HashSet<String>();
        for(int i=0;i<count;i++){
            String key=in.readUTF(),name=in.readUTF();int level=in.readInt();String rarity=in.readUTF();int rank=in.readInt();String description=in.readUTF();int targets=in.readInt();
            if(!keys.add(key)||key.isBlank()||name.isBlank()||level<1||targets<0||targets>128||rank<0||rank>100)throw new IOException("Invalid enchantment");
            var applies=new ArrayList<String>();for(int t=0;t<targets;t++)applies.add(in.readUTF());
            result.add(new Entry(key,name,level,rarity,rank,description,List.copyOf(applies)));
        }
        if(in.available()!=0)throw new IOException("Trailing catalogue data");
        return List.copyOf(result);
    }
}
