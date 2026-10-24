package com.axial.cosmetics.client;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Atomically publishes complete, bounded directory snapshots in the server's rank order. */
final class GangDirectory {
    record Member(UUID id,String name,String role,boolean online){}
    record Gang(String id,String name,int rank,int points,UUID leader,BigDecimal wealth,List<Member> members){
        String leaderName(){return members.stream().filter(m->m.id().equals(leader)).map(Member::name).findFirst().orElse("Unknown leader");}
    }
    List<Gang> gangs=List.of();long revision;
    private long pendingRevision;private int size;private byte[][] parts;
    void receive(byte[] packet)throws IOException{
        if(packet.length<9||packet.length>28017)throw new IOException("Invalid gang packet");
        var in=new DataInputStream(new ByteArrayInputStream(packet));int type=in.readUnsignedByte();long rev=in.readLong();
        if(rev<=0)throw new IOException("Invalid gang revision");
        if(type==2){if(in.available()!=0||rev!=revision)throw new IOException("Invalid gang acknowledgement");return;}
        if(type!=1)throw new IOException("Unknown gang packet");
        int total=in.readInt(),index=in.readUnsignedShort(),count=in.readUnsignedShort();
        if(total<1||total>2_000_000||count!=(total+27999)/28000||index>=count||in.available()!=Math.min(28000,total-index*28000))throw new IOException("Invalid gang fragment");
        if(parts==null||rev!=pendingRevision){parts=new byte[count][];size=total;pendingRevision=rev;}
        if(size!=total||parts.length!=count)throw new IOException("Mismatched gang fragments");
        parts[index]=in.readAllBytes();for(byte[] part:parts)if(part==null)return;
        var bytes=new ByteArrayOutputStream(size);for(byte[] part:parts)bytes.write(part);
        var decoded=decode(bytes.toByteArray());gangs=decoded;revision=rev;parts=null;
    }
    static List<Gang> decode(byte[] compressed)throws IOException{
        byte[] raw;try(var gzip=new GZIPInputStream(new ByteArrayInputStream(compressed))){raw=gzip.readNBytes(8_000_001);}
        if(raw.length>8_000_000)throw new IOException("Gang directory too large");
        var in=new DataInputStream(new ByteArrayInputStream(raw));int count=in.readInt();
        if(count<0||count>4096)throw new IOException("Invalid gang count");
        var gangs=new ArrayList<Gang>();var ids=new HashSet<String>();int allMembers=0;
        for(int i=0;i<count;i++){
            String id=in.readUTF(),name=in.readUTF();int rank=in.readInt(),points=in.readInt();UUID leader=in.readBoolean()?uuid(in):null;
            BigDecimal wealth=null;
            if(in.readBoolean()){
                String amount=in.readUTF();if(amount.length()>512)throw new IOException("Invalid gang balance");
                try{wealth=new BigDecimal(amount);if(Math.abs((long)wealth.scale())>512||wealth.precision()>512)throw new NumberFormatException();}
                catch(NumberFormatException ex){throw new IOException("Invalid gang balance",ex);}
            }
            int members=in.readInt();
            if(id.isBlank()||id.length()>256||name.isBlank()||name.length()>512||!ids.add(id)||rank!=i+1||points<0||members<1||members>4096||(allMembers+=members)>65536)throw new IOException("Invalid gang record");
            var people=new ArrayList<Member>();var memberIds=new HashSet<UUID>();
            for(int m=0;m<members;m++){
                UUID uuid=uuid(in);String player=in.readUTF(),role=in.readUTF();boolean online=in.readBoolean();
                if(!memberIds.add(uuid)||player.isBlank()||player.length()>128||role.length()>64)throw new IOException("Invalid gang member");
                people.add(new Member(uuid,player,role,online));
            }
            if(leader!=null&&!memberIds.contains(leader))throw new IOException("Gang leader is not a member");
            gangs.add(new Gang(id,name,rank,points,leader,wealth,List.copyOf(people)));
        }
        if(in.available()!=0)throw new IOException("Trailing gang data");return List.copyOf(gangs);
    }
    private static UUID uuid(DataInputStream in)throws IOException{return new UUID(in.readLong(),in.readLong());}
}
