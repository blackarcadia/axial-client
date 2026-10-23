package com.axial.worldmap;

import java.io.*;
import java.nio.charset.StandardCharsets;

final class MapProtocol {
    static final String CHANNEL="axial_cosmetics:world_map";
    static final int MAX_PACKET=29_000;
    private MapProtocol(){ }
    static boolean isRequest(byte[] data) {
        return data.length>=1&&data[0]==0&&(data.length==1||data.length==2&&data[1]==1);
    }
    static boolean requestsFullMap(byte[] data){return isRequest(data)&&data.length==2;}
    static int rowsPerPacket(WorldMap map) {
        int header=1+2+map.key().getBytes(StandardCharsets.UTF_8).length+12+8;
        return Math.max(1,(MAX_PACKET-header)/(map.geometry().width()*4));
    }
    static byte[] snapshot(WorldMap map,int startRow) throws IOException {
        MapGeometry g=map.geometry();
        if(startRow<0||startRow>=g.height())throw new IllegalArgumentException("Invalid stripe row");
        int rows=Math.min(rowsPerPacket(map),g.height()-startRow);
        var bytes=new ByteArrayOutputStream();
        try(var out=new DataOutputStream(bytes)) {
            byte[] key=map.key().getBytes(StandardCharsets.UTF_8);
            out.writeByte(1);out.writeShort(key.length);out.write(key);
            out.writeInt(g.minX());out.writeInt(g.minZ());out.writeInt(g.step());
            out.writeShort(g.width());out.writeShort(g.height());out.writeShort(startRow);out.writeShort(rows);
            for(int i=startRow*g.width();i<(startRow+rows)*g.width();i++)out.writeInt(map.colors()[i]);
        }
        return bytes.toByteArray();
    }
}
