package com.axial.cosmetics.client;

import io.github.kosmx.emotes.common.network.*;
import io.github.kosmx.emotes.server.serializer.type.impl.BinaryFormat;
import io.netty.buffer.Unpooled;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NpcEmotePacketTest {
    @Test void replayDefinitionsRetainRealAnimationIdentityAndRejectSpoofedMetadata()throws Exception {
        for(String name:List.of("gangnam_style","rat_dance","death","spooky_dance")) {
            try(var input=getClass().getResourceAsStream("/emotes/"+name+".emotecraft")) {
                var animation=new BinaryFormat().read(input,name).values().iterator().next();var bytes=Unpooled.buffer();
                try {
                    new EmotePacket.Builder().configureToStreamEmote(animation).setSizeLimit(NpcEmoteTimeline.MAX_PACKET,true).build().write(bytes,PacketBound.CLIENT);
                    var payload=new byte[bytes.readableBytes()];bytes.readBytes(payload);
                    var clip=new NpcEmoteTimeline.Clip(animation.uuid().toString(),name,Base64.getEncoder().encodeToString(payload),animation.length()*20);
                    var timeline=new NpcEmoteTimeline();timeline.addClip(clip);
                    var restored=NpcEmoteTimeline.decode(timeline.encode()).clips().get(clip.id());
                    assertEquals(animation.uuid(),NpcEmoteDirector.unpack(restored).uuid());
                    assertThrows(IllegalArgumentException.class,()->NpcEmoteDirector.unpack(new NpcEmoteTimeline.Clip(UUID.randomUUID().toString(),name,clip.packet(),clip.durationTicks())));
                    assertThrows(IllegalArgumentException.class,()->NpcEmoteDirector.unpack(new NpcEmoteTimeline.Clip(clip.id(),name,clip.packet(),clip.durationTicks()+1)));
                } finally {bytes.release();}
            }
        }
    }
}
