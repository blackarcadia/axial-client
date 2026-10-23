package com.axial.worldmap;

import java.util.*;

record MapGeometry(int minX,int minZ,int step,int width,int height) {
    MapGeometry {
        if(step<1||step>1_000_000||width<1||height<1||width>512||height>512)throw new IllegalArgumentException("Invalid map geometry");
    }
    static MapGeometry forBorder(double x,double z,double diameter,int resolution) {
        if(!Double.isFinite(x)||!Double.isFinite(z)||!Double.isFinite(diameter)||diameter<1||diameter>60_000_000)throw new IllegalArgumentException("Invalid world border");
        int edge=Math.clamp(resolution,32,512);
        double left=Math.ceil(x-diameter/2),top=Math.ceil(z-diameter/2),right=Math.ceil(x+diameter/2),bottom=Math.ceil(z+diameter/2);
        if(left<-30_000_000||top<-30_000_000||right>30_000_000||bottom>30_000_000)throw new IllegalArgumentException("Border exceeds Minecraft's world limits");
        int blocksX=Math.max(1,(int)(right-left)),blocksZ=Math.max(1,(int)(bottom-top));
        int step=Math.max(1,(int)Math.ceil(Math.max(blocksX,blocksZ)/(double)edge));
        return new MapGeometry((int)left,(int)top,step,(blocksX+step-1)/step,(blocksZ+step-1)/step);
    }
    int x(int index){return minX+(index%width)*step;}
    int z(int index){return minZ+(index/width)*step;}
    List<ChunkSamples> plan() {
        Map<Long,List<Integer>> chunks=new LinkedHashMap<>();
        for(int i=0;i<width*height;i++) {
            int cx=Math.floorDiv(x(i),16),cz=Math.floorDiv(z(i),16);
            long key=((long)cx<<32)|(cz&0xFFFFFFFFL);
            chunks.computeIfAbsent(key,k->new ArrayList<>()).add(i);
        }
        List<ChunkSamples> plan=new ArrayList<>(chunks.size());
        chunks.forEach((k,v)->plan.add(new ChunkSamples((int)(k>>32),(int)(long)k,v.stream().mapToInt(Integer::intValue).toArray())));
        return plan;
    }
    ChunkSamples samplesForChunk(int cx,int cz) {
        int firstX=(int)Math.max(0,Math.ceil(((long)cx*16-minX)/(double)step));
        int lastX=(int)Math.min(width-1,Math.floor(((long)cx*16+15-minX)/(double)step));
        int firstZ=(int)Math.max(0,Math.ceil(((long)cz*16-minZ)/(double)step));
        int lastZ=(int)Math.min(height-1,Math.floor(((long)cz*16+15-minZ)/(double)step));
        if(firstX>lastX||firstZ>lastZ)return new ChunkSamples(cx,cz,new int[0]);
        int[] indices=new int[(lastX-firstX+1)*(lastZ-firstZ+1)];int n=0;
        for(int z=firstZ;z<=lastZ;z++)for(int x=firstX;x<=lastX;x++)indices[n++]=z*width+x;
        return new ChunkSamples(cx,cz,indices);
    }
    record ChunkSamples(int x,int z,int[] indices) { }
}
