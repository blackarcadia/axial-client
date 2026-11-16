package com.axial.cosmetics.client;

import io.github.kosmx.emotes.server.serializer.type.impl.BinaryFormat;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NpcEmotePacketTest {
    @Test void replayDefinitionsRetainRealAnimationIdentityAndRejectSpoofedMetadata()throws Exception {
        for(String name:List.of("gangnam_style","rat_dance","death","spooky_dance")) {
            try(var input=getClass().getResourceAsStream("/emotes/"+name+".emotecraft")) {
                var animation=new BinaryFormat().read(input,name).values().iterator().next();
                var clip=NpcEmoteDirector.pack(animation,name);
                var timeline=new NpcEmoteTimeline();timeline.addClip(clip);
                var actor=UUID.randomUUID();
                timeline.addAll(List.of(new NpcEmoteTimeline.Cue(0,actor,clip.id(),true,0)));
                var restored=NpcEmoteTimeline.decode(timeline.encode());
                assertEquals(animation.uuid(),NpcEmoteDirector.unpack(restored.clips().get(clip.id())).uuid());
                // PlayerAnimCore compares its tick clock directly with Animation.length().
                // One native cycle must wrap immediately, not hold the final pose for 19 cycles.
                assertEquals(animation.length(),clip.durationTicks());
                for(int cycle=1;cycle<=3;cycle++) {
                    long time=(long)Math.ceil(animation.length()*50*cycle)+100;
                    float expected=(float)((time/50d)%animation.length());
                    assertEquals(expected,restored.pose(actor,time).ticks(),.001f);
                    assertTrue(restored.pose(actor,time).ticks()<animation.length());
                }
                assertThrows(IllegalArgumentException.class,()->NpcEmoteDirector.unpack(new NpcEmoteTimeline.Clip(UUID.randomUUID().toString(),name,clip.packet(),clip.durationTicks())));
                assertThrows(IllegalArgumentException.class,()->NpcEmoteDirector.unpack(new NpcEmoteTimeline.Clip(clip.id(),name,clip.packet(),clip.durationTicks()+1)));

            }
        }
    }
}
