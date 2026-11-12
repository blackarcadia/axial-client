package com.axial.cosmetics.client;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientPollTest {
    private static String json(String vote){return "{\"id\":1,\"question\":\"Next feature?\",\"closes_at\":2800,\"voted\":"+vote+",\"options\":[{\"id\":1,\"label\":\"Mining\"},{\"id\":2,\"label\":\"Building\"}]}";}
    @Test void countdownUsesServerDurationAndMonotonicElapsedTime(){
        var poll=ClientPoll.parse(JsonParser.parseString(json("null")).getAsJsonObject(),1000,5_000_000_000L);
        assertEquals(1800,poll.remaining(5_000_000_000L));
        assertEquals(1740,poll.remaining(65_000_000_000L));
        assertEquals(0,poll.remaining(1805_000_000_000L));
        assertNull(poll.voted());
    }
    @Test void voteReceiptMustReferToAnExistingOption(){
        assertEquals(2,ClientPoll.parse(JsonParser.parseString(json("2")).getAsJsonObject(),1000,0).voted());
        assertThrows(IllegalArgumentException.class,()->ClientPoll.parse(JsonParser.parseString(json("3")).getAsJsonObject(),1000,0));
    }
    @Test void duplicateOptionsAreRejected(){
        var object=JsonParser.parseString(json("null")).getAsJsonObject();
        object.getAsJsonArray("options").get(1).getAsJsonObject().addProperty("id",1);
        assertThrows(IllegalArgumentException.class,()->ClientPoll.parse(object,1000,0));
    }
    @Test void durationsRemainReadableAtRequestedScales(){
        assertEquals("30m 0s left",ClientPoll.formatTime(1800));
        assertEquals("1d 0h left",ClientPoll.formatTime(86400));
        assertEquals("29s left",ClientPoll.formatTime(29));
        assertEquals("Ended",ClientPoll.formatTime(0));
    }
}
