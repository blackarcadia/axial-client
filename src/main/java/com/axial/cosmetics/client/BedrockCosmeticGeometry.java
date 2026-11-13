package com.axial.cosmetics.client;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Lossless cube/pivot descriptions from the supplied Bedrock model, before baking. */
final class BedrockCosmeticGeometry {
    record Vec(float x,float y,float z) {
        Vec subtract(Vec other){return new Vec(x-other.x,y-other.y,z-other.z);}
    }
    record FaceUv(float u,float v,float width,float height) {}
    record Cube(Vec origin,Vec size,Vec pivot,Vec rotation,int u,int v,float inflate,boolean mirror,Map<String,FaceUv> faces) {
        Vec localOrigin(){return new Vec(origin.x-pivot.x,pivot.y-origin.y-size.y,origin.z-pivot.z);}
        // Bedrock's box UV layout floors dimensions without rounding the geometry.
        Vec uvSize(){return new Vec(texels(size.x),texels(size.y),texels(size.z));}
        Vec meshScale(){var uv=uvSize();return new Vec(size.x/uv.x,size.y/uv.y,size.z/uv.z);}
        Vec meshOrigin(){var local=localOrigin();var scale=meshScale();return new Vec(local.x/scale.x,local.y/scale.y,local.z/scale.z);}
        private static float texels(float size){return Math.max(1,(float)Math.floor(size+1e-7));}
    }
    record Bone(String name,String parent,Vec pivot,Vec rotation,List<Cube> cubes) {}
    private BedrockCosmeticGeometry() {}
    private static Vec vector(JsonObject object,String key,Vec fallback){
        if(!object.has(key))return fallback;
        var values=object.getAsJsonArray(key);return new Vec(values.get(0).getAsFloat(),values.get(1).getAsFloat(),values.get(2).getAsFloat());
    }
    static List<Bone> load(String id){
        try(var input=BedrockCosmeticGeometry.class.getResourceAsStream("/assets/axial_cosmetics/geo/"+id+".geo.json")){
            if(input==null)throw new IllegalStateException("Missing cosmetic geometry: "+id);
            var geometry=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            var result=new ArrayList<Bone>();
            for(var value:geometry.getAsJsonArray("bones")){
                var object=value.getAsJsonObject();var pivot=vector(object,"pivot",new Vec(0,0,0));
                var cubes=new ArrayList<Cube>();
                if(object.has("cubes"))for(var item:object.getAsJsonArray("cubes")){
                    var cube=item.getAsJsonObject();var mapping=cube.get("uv");
                    var uv=mapping.isJsonArray()?mapping.getAsJsonArray():null;
                    var faces=new HashMap<String,FaceUv>();
                    if(mapping.isJsonObject())for(var face:mapping.getAsJsonObject().entrySet()){
                        var definition=face.getValue().getAsJsonObject();var start=definition.getAsJsonArray("uv");var span=definition.getAsJsonArray("uv_size");
                        faces.put(face.getKey(),new FaceUv(start.get(0).getAsFloat(),start.get(1).getAsFloat(),span.get(0).getAsFloat(),span.get(1).getAsFloat()));
                    }
                    cubes.add(new Cube(vector(cube,"origin",null),vector(cube,"size",null),vector(cube,"pivot",pivot),vector(cube,"rotation",new Vec(0,0,0)),uv==null?0:uv.get(0).getAsInt(),uv==null?0:uv.get(1).getAsInt(),
                            cube.has("inflate")?cube.get("inflate").getAsFloat():object.has("inflate")?object.get("inflate").getAsFloat():0,
                            cube.has("mirror")?cube.get("mirror").getAsBoolean():object.has("mirror")&&object.get("mirror").getAsBoolean(),Map.copyOf(faces)));
                }
                result.add(new Bone(object.get("name").getAsString(),object.has("parent")?object.get("parent").getAsString():null,pivot,vector(object,"rotation",new Vec(0,0,0)),List.copyOf(cubes)));
            }
            return List.copyOf(result);
        }catch(java.io.IOException ex){throw new IllegalStateException("Could not load cosmetic geometry: "+id,ex);}
    }
    /** Convert Y-up Bedrock coordinates to the player's Y-down model space. */
    static Vec position(Vec value){return new Vec(value.x,24-value.y,value.z);}
    // Blockbench imports Bedrock rotations as (-X,-Y,+Z) in its X-reflected
    // space. Turning that space into Java's Y-down space gives (+X,+Y,+Z).
    static Vec rotation(Vec degrees){return new Vec((float)Math.toRadians(degrees.x),(float)Math.toRadians(degrees.y),(float)Math.toRadians(degrees.z));}
}
