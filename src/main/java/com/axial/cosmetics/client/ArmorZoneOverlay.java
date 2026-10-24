package com.axial.cosmetics.client;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Compact continuous armour outlines, independent of terrain-map pixels or zoom. */
final class ArmorZoneOverlay {
    record Region(String name,int color,double labelX,double labelZ,List<double[]> rings) { }
    record MapData(String world,List<Region> regions) {
        String zoneAt(double x,double z) {
            for(var region:regions) {
                boolean inside=false;
                for(double[] ring:region.rings) {
                    for(int i=0,j=ring.length-2;i<ring.length;j=i,i+=2) {
                        double ax=ring[i],az=ring[i+1],bx=ring[j],bz=ring[j+1];
                        if((az>z)!=(bz>z)&&x<(bx-ax)*(z-az)/(bz-az)+ax)inside=!inside;
                    }
                }
                if(inside)return region.name;
            }
            return null;
        }
    }
    private ArmorZoneOverlay() { }
    static MapData read(byte[] payload) throws IOException {
        if(payload.length>30_000)throw new IOException("Oversized armour overlay");
        try(var input=new DataInputStream(new ByteArrayInputStream(payload))) {
            if(input.readUnsignedByte()!=3)throw new IOException("Wrong armour packet type");
            String world=string(input);int count=input.readUnsignedShort();if(count>16)throw new IOException("Too many armour tiers");
            var regions=new ArrayList<Region>();
            for(int n=0;n<count;n++) {
                String name=string(input);int color=input.readInt();double x=coordinate(input),z=coordinate(input);
                int ringCount=input.readUnsignedShort();if(ringCount>256)throw new IOException("Too many armour contours");
                var rings=new ArrayList<double[]>();
                for(int ring=0;ring<ringCount;ring++) {
                    int points=input.readUnsignedShort();
                    if(points<3||points>4096||input.available()<points*16)throw new IOException("Invalid armour contour");
                    double[] coordinates=new double[points*2];for(int i=0;i<coordinates.length;i++)coordinates[i]=coordinate(input);rings.add(coordinates);
                }
                regions.add(new Region(name,color,x,z,List.copyOf(rings)));
            }
            if(input.available()!=0)throw new IOException("Unexpected armour geometry data");
            return new MapData(world,List.copyOf(regions));
        }
    }
    private static double coordinate(DataInputStream input) throws IOException {
        double value=input.readDouble();if(!Double.isFinite(value)||Math.abs(value)>30_000_001)throw new IOException("Invalid armour coordinate");return value;
    }
    private static String string(DataInputStream input) throws IOException {
        int length=input.readUnsignedShort();if(length>255)throw new IOException("Oversized armour label");
        byte[] bytes=input.readNBytes(length);if(bytes.length!=length)throw new EOFException();return new String(bytes,StandardCharsets.UTF_8);
    }
}
