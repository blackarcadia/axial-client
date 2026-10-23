package com.axial.cosmetics.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.io.IOException;
import java.util.*;

/** Session detail, bounded to 512 chunks; texture data is shared between block columns. */
final class MinimapSurfaceCache {
    record Surface(MinimapTile tile,int tint,double shade) {
        int color(double x,double z,int level){return tile.sample(x,z,level,tint,shade);}
        int average(){return tile.average(tint,shade);}
    }
    private record Face(MinimapTile tile,int tintIndex) { }
    private final Map<BlockState,Face> faces=new HashMap<>();
    private final Map<Identifier,MinimapTile> tiles=new HashMap<>();
    private final LinkedHashMap<Long,Surface[]> chunks=new LinkedHashMap<>(64,.75f,true);
    private Object modelMarker;
    private long revision;

    long revision(){return revision;}
    void clear(){chunks.clear();revision++;}
    void checkResources(MinecraftClient client) {
        Object current=client.getBlockRenderManager().getModel(Blocks.GRASS_BLOCK.getDefaultState());
        if(modelMarker!=current){modelMarker=current;faces.clear();tiles.clear();clear();}
    }
    Surface get(int x,int z) {
        Surface[] chunk=chunks.get(key(x>>4,z>>4));
        return chunk==null?null:chunk[(z&15)*16+(x&15)];
    }
    void remove(int x,int z) {
        Surface[] chunk=chunks.get(key(x>>4,z>>4));
        int index=(z&15)*16+(x&15);
        if(chunk!=null&&chunk[index]!=null){chunk[index]=null;revision++;}
    }
    Surface capture(MinecraftClient client,BlockState state,BlockPos pos,double shade) {
        Face face=faces.computeIfAbsent(state,s->face(client,s));
        if(face.tile==null){remove(pos.getX(),pos.getZ());return null;}
        int tint=face.tintIndex<0?0xFFFFFF:client.getBlockColors().getColor(state,client.world,pos,face.tintIndex);
        Surface surface=new Surface(face.tile,tint,shade);
        Surface[] chunk=chunks.computeIfAbsent(key(pos.getX()>>4,pos.getZ()>>4),ignored->new Surface[256]);
        int index=(pos.getZ()&15)*16+(pos.getX()&15);
        if(!surface.equals(chunk[index])){chunk[index]=surface;revision++;}
        if(chunks.size()>512){chunks.remove(chunks.keySet().iterator().next());revision++;}
        return surface;
    }
    private Face face(MinecraftClient client,BlockState state) {
        // Fluid models have no block quads; their still texture is the visible top surface.
        if(state.isOf(Blocks.WATER))return new Face(tile(client,Identifier.ofVanilla("block/water_still")),0);
        if(state.isOf(Blocks.LAVA))return new Face(tile(client,Identifier.ofVanilla("block/lava_still")),-1);
        var model=client.getBlockRenderManager().getModel(state);
        for(var part:model.getParts(Random.create(0))) {
            for(var quad:part.getQuads(Direction.UP))return face(client,quad);
            for(var quad:part.getQuads(null))if(quad.face()==Direction.UP)return face(client,quad);
        }
        return new Face(tile(client,model.particleSprite().getContents().getId()),0);
    }
    private Face face(MinecraftClient client,BakedQuad quad) {
        return new Face(tile(client,quad.sprite().getContents().getId()),quad.hasTint()?quad.tintIndex():-1);
    }
    private MinimapTile tile(MinecraftClient client,Identifier sprite) {
        if(tiles.containsKey(sprite))return tiles.get(sprite);
        MinimapTile result=null;
        Identifier path=Identifier.of(sprite.getNamespace(),"textures/"+sprite.getPath()+".png");
        var resource=client.getResourceManager().getResource(path);
        if(resource.isPresent())try(var stream=resource.get().getInputStream();var image=NativeImage.read(stream)) {
            // First frame for animated textures; keep a compact 16x16 tile even for HD packs.
            int width=image.getWidth(),height=Math.min(width,image.getHeight());
            int[] pixels=new int[256];
            for(int z=0;z<16;z++)for(int x=0;x<16;x++)pixels[z*16+x]=image.getColorArgb(x*width/16,z*height/16);
            result=new MinimapTile(pixels);
        } catch(IOException|RuntimeException ex) {
            org.slf4j.LoggerFactory.getLogger(MinimapSurfaceCache.class).debug("Cannot sample map texture {}",path,ex);
        }
        tiles.put(sprite,result);
        return result;
    }
    private static long key(int x,int z){return ((long)x<<32)|(z&0xffffffffL);}
}
