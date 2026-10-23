package com.axial.cosmetics.client;

import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;

/** Primitive block colours, grouped by chunk. Safe to stream to disk off the game thread. */
final class MinimapTerrain {
    private final ConcurrentHashMap<Long,Chunk> chunks=new ConcurrentHashMap<>();
    // The lookup cache is used only by the game/render thread, never by the save worker.
    private long lastKey;
    private Chunk lastChunk;
    private Chunk chunk(int x,int z) {
        long key=key(x>>4,z>>4);
        Chunk chunk=key==lastKey&&lastChunk!=null?lastChunk:chunks.get(key);
        lastKey=key;lastChunk=chunk;return chunk;
    }
    int get(int x,int z) {
        Chunk chunk=chunk(x,z);
        return chunk==null?0:chunk.colors[(z&15)*16+(x&15)];
    }
    boolean shouldSample(int x,int z,int tick) {
        Chunk chunk=chunk(x,z);
        if(chunk==null)return true;
        int index=(z&15)*16+(x&15),age=tick-chunk.sampledAt[index];
        return chunk.colors[index]==0||chunk.sampledAt[index]==0||age<0||age>=40;
    }
    void sampled(int x,int z,int tick) {
        Chunk chunk=chunk(x,z);
        if(chunk!=null)chunk.sampledAt[(z&15)*16+(x&15)]=Math.max(1,tick);
    }
    /** Each background reader has its own chunk cursor, independent of the game thread. */
    IntBinaryOperator reader() {
        return new IntBinaryOperator() {
            long key;Chunk chunk;
            public int applyAsInt(int x,int z) {
                long next=key(x>>4,z>>4);
                if(chunk==null||next!=key){key=next;chunk=chunks.get(next);}
                return chunk==null?0:chunk.colors[(z&15)*16+(x&15)];
            }
        };
    }
    boolean put(int x,int z,int color) {
        long key=key(x>>4,z>>4);
        Chunk chunk=key==lastKey&&lastChunk!=null?lastChunk:chunks.computeIfAbsent(key,k->new Chunk());
        lastKey=key;lastChunk=chunk;
        synchronized(chunk) {
            int index=(z&15)*16+(x&15);
            if(chunk.colors[index]==color)return false;
            chunk.colors[index]=color;return true;
        }
    }
    void write(JsonWriter writer) throws IOException {
        writer.beginObject();
        forEachChunk((key,colors)->{
            int minX=(int)(key>>32)*16,minZ=key.intValue()*16;
            try {
                for(int i=0;i<colors.length;i++)if(colors[i]!=0)writer.name((minX+(i&15))+","+(minZ+(i>>4))).value(colors[i]);
            } catch(IOException ex){throw new UncheckedIOException(ex);}
        });
        writer.endObject();
    }
    void forEachChunk(BiConsumer<Long,int[]> consumer) {
        chunks.forEach((key,chunk)->{
            int[] snapshot;
            synchronized(chunk){snapshot=chunk.colors.clone();}
            consumer.accept(key,snapshot);
        });
    }
    static long key(int x,int z){return ((long)x<<32)|(z&0xffffffffL);}
    private static final class Chunk { final int[] colors=new int[256],sampledAt=new int[256]; }
}
