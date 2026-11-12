package com.axial.cosmetics.client;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Lossless cube/pivot descriptions from the supplied Bedrock model, before baking. */
final class WhiteKicksGeometry {
    record Vec(float x,float y,float z) {
        Vec subtract(Vec other){return new Vec(x-other.x,y-other.y,z-other.z);}
    }
    record Cube(Vec origin,Vec size,Vec pivot,Vec rotation,int u,int v) {
        Vec localOrigin(){return new Vec(origin.x-pivot.x,pivot.y-origin.y-size.y,origin.z-pivot.z);}
    }
    record Bone(String name,String parent,Vec pivot,Vec rotation,List<Cube> cubes) {}
    private WhiteKicksGeometry() {}
    private static Vec vector(JsonObject object,String key,Vec fallback){
        if(!object.has(key))return fallback;
        var values=object.getAsJsonArray(key);return new Vec(values.get(0).getAsFloat(),values.get(1).getAsFloat(),values.get(2).getAsFloat());
    }
    static List<Bone> load(){return load("white_kicks");}
    static List<Bone> load(String id){
        try(var input=WhiteKicksGeometry.class.getResourceAsStream("/assets/axial_cosmetics/geo/"+id+".geo.json")){
            if(input==null)throw new IllegalStateException("Missing shoe geometry: "+id);
            var geometry=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            var result=new ArrayList<Bone>();
            for(var value:geometry.getAsJsonArray("bones")){
                var object=value.getAsJsonObject();var pivot=vector(object,"pivot",new Vec(0,0,0));
                var cubes=new ArrayList<Cube>();
                if(object.has("cubes"))for(var item:object.getAsJsonArray("cubes")){
                    var cube=item.getAsJsonObject();var uv=cube.getAsJsonArray("uv");
                    cubes.add(new Cube(vector(cube,"origin",null),vector(cube,"size",null),vector(cube,"pivot",pivot),vector(cube,"rotation",new Vec(0,0,0)),uv.get(0).getAsInt(),uv.get(1).getAsInt()));
                }
                result.add(new Bone(object.get("name").getAsString(),object.has("parent")?object.get("parent").getAsString():null,pivot,vector(object,"rotation",new Vec(0,0,0)),List.copyOf(cubes)));
            }
            return List.copyOf(result);
        }catch(java.io.IOException ex){throw new IllegalStateException("Could not load shoe geometry: "+id,ex);}
    }
    /** Convert Y-up Bedrock coordinates to the player's Y-down model space. */
    static Vec position(Vec value){return new Vec(value.x,24-value.y,value.z);}
    static Vec rotation(Vec degrees){return new Vec((float)Math.toRadians(-degrees.x),(float)Math.toRadians(degrees.y),(float)Math.toRadians(-degrees.z));}
}
