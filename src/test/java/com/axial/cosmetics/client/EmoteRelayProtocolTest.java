package com.axial.cosmetics.client;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmoteRelayProtocolTest {
    @Test void validatesAuthenticatedAuthorIdentifiers() throws Exception {
        String author="6a9cccc8-8e00-4a73-bd37-1121f81b0674";
        assertEquals(UUID.fromString(author),EmoteRelayProtocol.uuid(author));
        for(String bad:new String[]{"1-1-1-1-1","not-a-uuid",author.toUpperCase(),author+"\n"})
            assertThrows(IOException.class,()->EmoteRelayProtocol.uuid(bad));
    }
    @Test void boundsPacketsAndRejectsInvalidBase64() throws Exception {
        byte[] bytes=new byte[EmoteRelayProtocol.MAX_PACKET];
        assertArrayEquals(bytes,EmoteRelayProtocol.decode(Base64.getEncoder().encodeToString(bytes)));
        assertThrows(IOException.class,()->EmoteRelayProtocol.decode(Base64.getEncoder().encodeToString(new byte[bytes.length+1])));
        assertThrows(IOException.class,()->EmoteRelayProtocol.decode(""));
        assertThrows(IOException.class,()->EmoteRelayProtocol.decode("bad$base64"));
    }
    @Test void rejectsMalformedAndOversizedEnvelopes() throws Exception {
        assertEquals("ready",EmoteRelayProtocol.parse("{\"type\":\"ready\"}").get("type").getAsString());
        assertThrows(IOException.class,()->EmoteRelayProtocol.parse("[]"));
        assertThrows(IOException.class,()->EmoteRelayProtocol.parse("{"));
        assertThrows(IOException.class,()->EmoteRelayProtocol.parse(" ".repeat(EmoteRelayProtocol.MAX_MESSAGE+1)));
    }
}
