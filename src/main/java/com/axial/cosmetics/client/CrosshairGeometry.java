package com.axial.cosmetics.client;

import java.util.*;

/** Pixel geometry shared by game rendering, preset thumbnails and the editor preview. */
final class CrosshairGeometry {
    record Rect(int x,int y,int width,int height){}
    record Key(CrosshairConfig.CrosshairStyle style,int length,int width,int gap,boolean dot,int dotSize,int outline) {
        static Key of(CrosshairConfig c,float pulse){
            float scale=Float.isFinite(c.size)?Math.clamp(c.size,.1f,4):1;
            int boost=Float.isFinite(pulse)?Math.round(Math.clamp(pulse-1,0,1)*8):0;
            return new Key(c.style==null?CrosshairConfig.CrosshairStyle.LUNAR:c.style,
                    Math.clamp(Math.round(c.length*scale),0,128),Math.clamp(Math.round(c.width*scale),1,48),
                    Math.clamp(Math.round(c.gap*scale)+boost,0,88),c.centerDot,Math.clamp(Math.round(c.dotSize*scale),1,32),
                    c.outlineEnabled?Math.clamp(c.outlineThickness,1,4):0);
        }
    }
    record Shape(List<Rect> ink,List<Rect> outline){}
    private CrosshairGeometry(){}
    static Shape build(Key k){
        int extent=k.length()+k.gap()+k.width()+k.dotSize()+k.outline()+3;
        int side=extent*2+1;boolean[] ink=new boolean[side*side],outline=new boolean[ink.length];
        for(int y=-extent;y<=extent;y++)for(int x=-extent;x<=extent;x++){
            if(contains(k,x,y))ink[(y+extent)*side+x+extent]=true;
        }
        if(k.outline()>0)for(int y=0;y<side;y++)for(int x=0;x<side;x++)if(ink[y*side+x]){
            for(int dy=-k.outline();dy<=k.outline();dy++)for(int dx=-k.outline();dx<=k.outline();dx++){
                int nx=x+dx,ny=y+dy;if(nx>=0&&ny>=0&&nx<side&&ny<side)outline[ny*side+nx]=true;
            }
        }
        for(int i=0;i<ink.length;i++)outline[i]&=!ink[i];
        return new Shape(runs(ink,side,extent),runs(outline,side,extent));
    }
    private static boolean contains(Key k,int x,int y){
        if(k.dot()&&square(x,y,k.dotSize()))return true;
        double center=(k.width()%2==0)?-.5:0;
        double dx=x-center,dy=y-center,ax=Math.abs(dx),ay=Math.abs(dy),half=(k.width()-1)/2.0;
        int length=k.length(),gap=k.gap();
        return switch(k.style()){
            case DOT -> square(x,y,k.width());
            case SMALL_DOT -> square(x,y,Math.max(1,k.width()/2));
            case CIRCLE -> ring(Math.hypot(dx,dy),Math.max(1,length)+gap,k.width());
            case DIAMOND -> ring(ax+ay,Math.max(1,length)+gap,k.width());
            case SQUARE -> ring(Math.max(ax,ay),Math.max(1,length)+gap,k.width());
            case CHEVRON -> dy>=-half&&dy<=length+half&&ax<=length+half&&Math.abs(dy-ax)<=half+.25;
            case BRACKETS -> (Math.abs(ax-(gap+length))<=half&&ay<=length)
                    ||(ay>Math.max(0,length-k.width())&&ay<=length&&ax>=gap&&ax<=gap+length);
            case X -> ax>gap&&ax<=gap+length&&Math.abs(ax-ay)<=half+.25;
            case CLASSIC,LUNAR,PLUS,T,SMALL_CROSS -> {
                int l=k.style()==CrosshairConfig.CrosshairStyle.SMALL_CROSS?Math.max(1,length/2):length;
                int g=k.style()==CrosshairConfig.CrosshairStyle.PLUS?0:k.style()==CrosshairConfig.CrosshairStyle.SMALL_CROSS?gap/2:gap;
                if(l==0)yield square(x,y,k.width());
                boolean horizontal=ay<=half&&ax>g&&ax<=g+l;
                boolean vertical=ax<=half&&ay>g&&ay<=g+l&&(k.style()!=CrosshairConfig.CrosshairStyle.T||dy>0);
                yield horizontal||vertical||(g==0&&ax<=half&&ay<=half);
            }
        };
    }
    private static boolean ring(double distance,int radius,int stroke){return distance<=radius+.25&&distance>Math.max(-.5,radius-stroke+.25);}
    private static boolean square(int x,int y,int size){int start=-size/2;return x>=start&&x<start+size&&y>=start&&y<start+size;}
    private static List<Rect> runs(boolean[] pixels,int side,int offset){
        var rectangles=new ArrayList<Rect>();
        Map<Long,Integer> previous=new HashMap<>();
        for(int y=0;y<side;y++){
            Map<Long,Integer> row=new HashMap<>();int x=0;
            while(x<side){
                if(!pixels[y*side+x]){x++;continue;}
                int start=x;while(x<side&&pixels[y*side+x])x++;
                long key=((long)start<<32)|(x-start);Integer index=previous.get(key);
                if(index!=null){Rect old=rectangles.get(index);rectangles.set(index,new Rect(old.x(),old.y(),old.width(),old.height()+1));}
                else{index=rectangles.size();rectangles.add(new Rect(start-offset,y-offset,x-start,1));}
                row.put(key,index);
            }
            previous=row;
        }
        return List.copyOf(rectangles);
    }
}
