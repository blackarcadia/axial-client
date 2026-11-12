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
    @Test void resultsUseAggregateCountsAndPercentageRoundingTotalsOneHundred(){
        var object=JsonParser.parseString(json("null")).getAsJsonObject();
        object.getAsJsonArray("options").get(0).getAsJsonObject().addProperty("votes",64);
        object.getAsJsonArray("options").get(1).getAsJsonObject().addProperty("votes",36);
        var poll=ClientPoll.parse(object,1000,0);
        assertTrue(poll.resultsAvailable());assertEquals(100,poll.totalVotes());
        assertEquals(64,poll.percentage(poll.options().get(0)));assertEquals(36,poll.percentage(poll.options().get(1)));
        var third=JsonParser.parseString("{\"id\":3,\"label\":\"Other\",\"votes\":1}");
        object.getAsJsonArray("options").add(third);
        for(var option:object.getAsJsonArray("options"))option.getAsJsonObject().addProperty("votes",1);
        poll=ClientPoll.parse(object,1000,0);
        assertEquals(34,poll.percentage(poll.options().get(0)));
        assertEquals(33,poll.percentage(poll.options().get(1)));assertEquals(33,poll.percentage(poll.options().get(2)));
    }
    @Test void noVotesShowsZeroAndLegacyResponsesRemainUnknown(){
        var object=JsonParser.parseString(json("null")).getAsJsonObject();
        assertFalse(ClientPoll.parse(object,1000,0).resultsAvailable());
        for(var row:object.getAsJsonArray("options"))row.getAsJsonObject().addProperty("votes",0);
        var poll=ClientPoll.parse(object,1000,0);
        assertTrue(poll.resultsAvailable());assertEquals(0,poll.totalVotes());
        assertEquals(0,poll.percentage(poll.options().getFirst()));
        object.getAsJsonArray("options").get(0).getAsJsonObject().addProperty("votes",-1);
        assertThrows(IllegalArgumentException.class,()->ClientPoll.parse(object,1000,0));
    }
    @Test void voteReceiptUsesServerTotalsAndRejectsMismatchedChoices(){
        var poll=ClientPoll.parse(JsonParser.parseString(json("null")).getAsJsonObject(),1000,0);
        var receipt=JsonParser.parseString("{\"optionId\":2,\"options\":[{\"id\":1,\"label\":\"Mining\",\"votes\":3},{\"id\":2,\"label\":\"Building\",\"votes\":2}]}").getAsJsonObject();
        var updated=poll.recordVote(receipt);
        assertEquals(2,updated.voted());assertEquals(5,updated.totalVotes());
        assertEquals(40,updated.percentage(updated.options().get(1)));
        assertEquals(5,updated.recordVote(receipt).totalVotes(),"idempotent receipts must not increment counts locally");
        assertNull(poll.voted());assertEquals(poll.deadlineNanos(),updated.deadlineNanos());
        receipt.getAsJsonArray("options").get(1).getAsJsonObject().addProperty("id",3);
        assertThrows(IllegalArgumentException.class,()->poll.recordVote(receipt));
    }
}
