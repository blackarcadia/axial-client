package com.axial.cosmetics.client;

import com.google.gson.*;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Static GEO bounds in Bedrock editor space, including bone/cube rotations and inflation. */
final class CapeObstructionGeometry {
    private CapeObstructionGeometry() {}
    static boolean intersectsBack(JsonObject document,Set<String> visibleBones){
        for(var geometry:document.getAsJsonArray("minecraft:geometry")){
            var bones=new LinkedHashMap<String,JsonObject>();
            for(var value:geometry.getAsJsonObject().getAsJsonArray("bones")){var b=value.getAsJsonObject();bones.put(b.get("name").getAsString(),b);}
            var transforms=new HashMap<String,Matrix4f>();
            for(var entry:bones.entrySet()){
                var bone=entry.getValue();if(!visible(entry.getKey(),bones,visibleBones,new HashSet<>())||!bone.has("cubes"))continue;
                var pivot=editor(vector(bone,"pivot",new Vector3f()));
                var matrix=transform(entry.getKey(),bones,transforms,new HashSet<>());
                for(var value:bone.getAsJsonArray("cubes")){
                    var cube=value.getAsJsonObject();var origin=vector(cube,"origin",new Vector3f());var size=vector(cube,"size",new Vector3f());
                    var cp=editor(vector(cube,"pivot",vector(bone,"pivot",new Vector3f())));
                    var local=new Matrix4f(matrix).translate(new Vector3f(cp).sub(pivot));rotate(local,vector(cube,"rotation",new Vector3f()));
                    float inflate=cube.has("inflate")?cube.get("inflate").getAsFloat():bone.has("inflate")?bone.get("inflate").getAsFloat():0;
                    var min=new Vector3f(Float.POSITIVE_INFINITY);var max=new Vector3f(Float.NEGATIVE_INFINITY);
                    for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++){
                        var point=new Vector3f(-origin.x+inflate-x*(size.x+2*inflate),origin.y-inflate+y*(size.y+2*inflate),origin.z-inflate+z*(size.z+2*inflate)).sub(cp);
                        local.transformPosition(point);min.min(point);max.max(point);
                    }
                    // The back surface is z=2; leave a small tolerance for flush trim.
                    // Include the waist because a resting cape hangs below the torso.
                    if(max.x> -4.5f&&min.x<4.5f&&max.y>10&&min.y<24&&max.z>2.25f&&min.z<6)return true;
                }
            }
        }
        return false;
    }
    private static boolean visible(String name,Map<String,JsonObject> bones,Set<String> selected,Set<String> seen){
        if(selected.isEmpty()||selected.contains(name))return true;
        if(!seen.add(name))return false;
        var bone=bones.get(name);return bone!=null&&bone.has("parent")&&visible(bone.get("parent").getAsString(),bones,selected,seen);
    }
    private static Matrix4f transform(String name,Map<String,JsonObject> bones,Map<String,Matrix4f> cache,Set<String> seen){
        var saved=cache.get(name);if(saved!=null)return saved;
        if(!seen.add(name))throw new IllegalArgumentException("Cyclic GEO bones");
        var bone=bones.get(name);if(bone==null)throw new IllegalArgumentException("Missing GEO parent");
        var pivot=editor(vector(bone,"pivot",new Vector3f()));var matrix=new Matrix4f();
        if(bone.has("parent")){
            String parent=bone.get("parent").getAsString();matrix.set(transform(parent,bones,cache,seen));
            pivot.sub(editor(vector(bones.get(parent),"pivot",new Vector3f())));
        }
        matrix.translate(pivot);rotate(matrix,vector(bone,"rotation",new Vector3f()));cache.put(name,matrix);return matrix;
    }
    private static Vector3f vector(JsonObject object,String key,Vector3f fallback){
        if(!object.has(key))return new Vector3f(fallback);var a=object.getAsJsonArray(key);return new Vector3f(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat());
    }
    private static Vector3f editor(Vector3f p){return new Vector3f(-p.x,p.y,p.z);}
    private static void rotate(Matrix4f matrix,Vector3f r){matrix.rotateZ((float)Math.toRadians(r.z)).rotateY((float)Math.toRadians(-r.y)).rotateX((float)Math.toRadians(-r.x));}
}
