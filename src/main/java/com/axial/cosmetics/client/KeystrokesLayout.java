package com.axial.cosmetics.client;

import java.util.*;

final class KeystrokesLayout {
    static final int WIDTH=70;
    static final String[] LABELS={"W","A","S","D","LFT","RHT"};
    record Key(String label,int x,int y,int width,int height,int bit){}
    record Arrangement(int width,int height,List<Key> keys){}
    static Arrangement arrange(boolean stacked,int visibleMask){
        int[][] groups=stacked?new int[][]{{0},{1,2},{3},{4,5}}:new int[][]{{0},{1,2,3},{4,5}};
        var rows=new ArrayList<List<Integer>>();int width=0;
        for(var group:groups){
            var row=new ArrayList<Integer>();int rowWidth=0;
            for(int bit:group)if((visibleMask&(1<<bit))!=0){row.add(bit);rowWidth+=(bit<4?22:34)+2;}
            if(!row.isEmpty()){rows.add(row);width=Math.max(width,rowWidth-2);}
        }
        var keys=new ArrayList<Key>();int y=0;
        for(var row:rows){
            int rowWidth=row.stream().mapToInt(bit->(bit<4?22:34)+2).sum()-2;
            int x=(width-rowWidth)/2,height=row.getFirst()<4?22:20;
            for(int bit:row){int keyWidth=bit<4?22:34;keys.add(new Key(LABELS[bit],x,y,keyWidth,height,bit));x+=keyWidth+2;}
            y+=height+2;
        }
        return new Arrangement(width,rows.isEmpty()?0:y-2,List.copyOf(keys));
    }
    static List<Key> keys(boolean stacked,boolean mouse){return arrange(stacked,mouse?63:15).keys();}
    static int height(boolean stacked,boolean mouse){return arrange(stacked,mouse?63:15).height();}
}
