package com.axial.cosmetics.client;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Samples the supplied looping Bedrock animation, in seconds and model pixels. */
final class ShoulderPetAnimation {
    record Pose(BedrockCosmeticGeometry.Vec position,BedrockCosmeticGeometry.Vec rotation,BedrockCosmeticGeometry.Vec scale) {}
    private final float duration;
    private final Map<String,Map<String,NavigableMap<Float,BedrockCosmeticGeometry.Vec>>> bones=new LinkedHashMap<>();
    ShoulderPetAnimation() {this("pink_slime");}
    ShoulderPetAnimation(String id) {
        try(var in=getClass().getResourceAsStream("/assets/axial_cosmetics/animations/"+id+".animation.json")) {
            if(in==null)throw new IllegalStateException("Missing shoulder pet animation");
            var animation=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject()
                    .getAsJsonObject("animations").getAsJsonObject("animation."+id+".float");
            duration=animation.get("animation_length").getAsFloat();
            for(var boneEntry:animation.getAsJsonObject("bones").entrySet()) {
                var bone=boneEntry.getValue().getAsJsonObject();
                var tracks=new HashMap<String,NavigableMap<Float,BedrockCosmeticGeometry.Vec>>();
                for(String channel:List.of("position","rotation","scale")) {
                    if(!bone.has(channel))continue;
                    var values=new TreeMap<Float,BedrockCosmeticGeometry.Vec>();
                    for(var entry:bone.getAsJsonObject(channel).entrySet()) {
                        var v=entry.getValue().getAsJsonArray();
                        values.put(Float.parseFloat(entry.getKey()),new BedrockCosmeticGeometry.Vec(v.get(0).getAsFloat(),v.get(1).getAsFloat(),v.get(2).getAsFloat()));
                    }
                    tracks.put(channel,values);
                }
                bones.put(boneEntry.getKey(),tracks);
            }
        }catch(java.io.IOException ex){throw new IllegalStateException("Could not load shoulder pet animation",ex);}
    }
    Set<String> boneNames(){return Collections.unmodifiableSet(bones.keySet());}
    Pose sample(float seconds) {return sample("slime",seconds);}
    Pose sample(String bone,float seconds) {
        float time=((seconds%duration)+duration)%duration;
        return new Pose(sample(bone,"position",time),sample(bone,"rotation",time),sample(bone,"scale",time));
    }
    private BedrockCosmeticGeometry.Vec sample(String bone,String channel,float time) {
        var track=bones.get(bone).get(channel);
        if(track==null)return channel.equals("scale")?new BedrockCosmeticGeometry.Vec(1,1,1):new BedrockCosmeticGeometry.Vec(0,0,0);
        var lo=track.floorEntry(time);var hi=track.ceilingEntry(time);
        if(lo==null)return track.firstEntry().getValue();if(hi==null)return track.lastEntry().getValue();
        float t=hi.getKey().equals(lo.getKey())?0:(time-lo.getKey())/(hi.getKey()-lo.getKey());
        var a=lo.getValue();var b=hi.getValue();
        return new BedrockCosmeticGeometry.Vec(a.x()+(b.x()-a.x())*t,a.y()+(b.y()-a.y())*t,a.z()+(b.z()-a.z())*t);
    }
}
