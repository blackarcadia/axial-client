package com.axial.cosmetics.client;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Samples the supplied looping Bedrock animation, in seconds and model pixels. */
final class ShoulderPetAnimation {
    record Pose(BedrockCosmeticGeometry.Vec position,BedrockCosmeticGeometry.Vec rotation,BedrockCosmeticGeometry.Vec scale) {}
    private final float duration;
    private final Map<String,NavigableMap<Float,BedrockCosmeticGeometry.Vec>> tracks=new HashMap<>();
    ShoulderPetAnimation() {
        try(var in=getClass().getResourceAsStream("/assets/axial_cosmetics/animations/pink_slime.animation.json")) {
            if(in==null)throw new IllegalStateException("Missing shoulder pet animation");
            var animation=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject()
                    .getAsJsonObject("animations").getAsJsonObject("animation.pink_slime.float");
            duration=animation.get("animation_length").getAsFloat();
            var bone=animation.getAsJsonObject("bones").getAsJsonObject("slime");
            for(String channel:List.of("position","rotation","scale")) {
                var values=new TreeMap<Float,BedrockCosmeticGeometry.Vec>();
                for(var entry:bone.getAsJsonObject(channel).entrySet()) {
                    var v=entry.getValue().getAsJsonArray();
                    values.put(Float.parseFloat(entry.getKey()),new BedrockCosmeticGeometry.Vec(v.get(0).getAsFloat(),v.get(1).getAsFloat(),v.get(2).getAsFloat()));
                }
                tracks.put(channel,values);
            }
        }catch(java.io.IOException ex){throw new IllegalStateException("Could not load shoulder pet animation",ex);}
    }
    Pose sample(float seconds) {
        float time=((seconds%duration)+duration)%duration;
        return new Pose(sample("position",time),sample("rotation",time),sample("scale",time));
    }
    private BedrockCosmeticGeometry.Vec sample(String channel,float time) {
        var track=tracks.get(channel);var lo=track.floorEntry(time);var hi=track.ceilingEntry(time);
        if(lo==null)return track.firstEntry().getValue();if(hi==null)return track.lastEntry().getValue();
        float t=hi.getKey().equals(lo.getKey())?0:(time-lo.getKey())/(hi.getKey()-lo.getKey());
        var a=lo.getValue();var b=hi.getValue();
        return new BedrockCosmeticGeometry.Vec(a.x()+(b.x()-a.x())*t,a.y()+(b.y()-a.y())*t,a.z()+(b.z()-a.z())*t);
    }
}
