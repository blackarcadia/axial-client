package com.axial.worldmap;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.BitSet;
import java.util.zip.*;

/** Versioned, compressed cache with atomic replacement and strict size validation. */
final class MapCache {
    private static final int LEGACY_MAGIC=0x41574D31,MAGIC=0x41574D32;
    private final Path directory;
    MapCache(Path directory){this.directory=directory;}
    WorldMap read(UUID expected,MapGeometry geometry) throws IOException {
        Path file=directory.resolve(expected+".map.gz");if(!Files.exists(file))return null;
        try(var in=new DataInputStream(new GZIPInputStream(Files.newInputStream(file)))) {
            int magic=in.readInt();if(magic!=MAGIC&&magic!=LEGACY_MAGIC)throw new IOException("Unknown map cache format");
            UUID world=new UUID(in.readLong(),in.readLong());
            if(!world.equals(expected))throw new IOException("Map cache world mismatch");
            MapGeometry stored;
            try{stored=new MapGeometry(in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt());}
            catch(IllegalArgumentException ex){throw new IOException("Invalid map cache geometry",ex);}
            long time=in.readLong();
            if(!stored.equals(geometry))return null;
            int[] pixels=new int[stored.width()*stored.height()];
            for(int i=0;i<pixels.length;i++)pixels[i]=in.readInt();
            BitSet sampled=null;
            if(magic==MAGIC) {
                int length=in.readInt();
                if(length< -1||length>(pixels.length+7)/8)throw new IOException("Invalid map coverage size");
                if(length>=0) {
                    byte[] bytes=in.readNBytes(length);if(bytes.length!=length)throw new EOFException("Truncated map coverage");
                    sampled=BitSet.valueOf(bytes);if(sampled.length()>pixels.length)throw new IOException("Invalid map coverage");
                }
            }
            if(in.read()!=-1)throw new IOException("Unexpected map cache data");
            return new WorldMap(world,stored,time,pixels,sampled);
        }
    }
    void write(WorldMap map) throws IOException {
        Files.createDirectories(directory);Path temp=Files.createTempFile(directory,"world-map-",".tmp");
        try {
            try(var out=new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(temp)))) {
                var g=map.geometry();out.writeInt(MAGIC);out.writeLong(map.world().getMostSignificantBits());out.writeLong(map.world().getLeastSignificantBits());
                out.writeInt(g.minX());out.writeInt(g.minZ());out.writeInt(g.step());out.writeInt(g.width());out.writeInt(g.height());out.writeLong(map.renderedAt());
                for(int color:map.colors())out.writeInt(color);
                byte[] sampled=map.complete()?null:map.sampled().toByteArray();
                out.writeInt(sampled==null?-1:sampled.length);if(sampled!=null)out.write(sampled);
            }
            Path dest=directory.resolve(map.world()+".map.gz");
            try{Files.move(temp,dest,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ex){Files.move(temp,dest,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temp);}
    }
}
