package com.axial.cosmetics.client;

import io.github.kosmx.emotes.common.network.EmotePacket;
import io.github.kosmx.emotes.common.network.PacketBound;
import io.github.kosmx.emotes.common.network.PacketTask;
import io.github.kosmx.emotes.server.serializer.type.impl.BinaryFormat;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmoteRelayPacketTest {
    @Test void bundledAnimationsRoundTripThroughTheActualClientWireFormat() throws Exception {
        UUID author=UUID.fromString("6a9cccc8-8e00-4a73-bd37-1121f81b0674");
        for(String name:new String[]{"gangnam_style","rat_dance","death","fear","keep_your_head_off","spooky_dance"}) {
            try(var input=getClass().getResourceAsStream("/emotes/"+name+".emotecraft")) {
                assertNotNull(input);
                var animation=new BinaryFormat().read(input,name).values().iterator().next();
                var original=new EmotePacket.Builder().configureToStreamEmote(animation,author).configureEmoteTick(3.5f)
                        .setSizeLimit(EmoteRelayProtocol.MAX_PACKET,false).build();
                var bytes=Unpooled.buffer();
                try {
                    original.write(bytes,PacketBound.CLIENT);
                    assertTrue(bytes.readableBytes()<=EmoteRelayProtocol.MAX_PACKET,name+" must fit relay packet limit");
                    var received=new EmotePacket(bytes,PacketBound.CLIENT);
                    assertEquals(PacketTask.STREAM,received.data.purpose,name);
                    assertNotNull(received.data.emoteData,name);
                    assertEquals(3.5f,received.data.tick,name);
                    assertEquals(author,received.data.player,name);
                }finally{bytes.release();}
            }
        }
    }
    @Test void stopsRoundTripThroughTheSameTransport() {
        UUID author=UUID.randomUUID(),emote=UUID.randomUUID();
        var original=new EmotePacket.Builder().configureToSendStop(emote,author).build();
        var bytes=Unpooled.buffer();
        try {
            original.write(bytes,PacketBound.CLIENT);var received=new EmotePacket(bytes,PacketBound.CLIENT);
            assertEquals(PacketTask.STOP,received.data.purpose);assertEquals(emote,received.data.stopEmoteID);assertEquals(author,received.data.player);
        }finally{bytes.release();}
    }
}
