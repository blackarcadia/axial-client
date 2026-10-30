package com.axial.cosmetics.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

/** Bounded relay envelopes. The relay supplies an authenticated author outside Emotecraft data. */
final class EmoteRelayProtocol {
    static final int MAX_PACKET=262144;
    static final int MAX_MESSAGE=360000;
    static final int MAX_WATCH=256;
    private EmoteRelayProtocol() { }
    static UUID uuid(String text) throws IOException {
        if(text==null||!text.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"))throw new IOException("Invalid relay UUID");
        return UUID.fromString(text);
    }
    static JsonObject parse(String text) throws IOException {
        if(text.length()>MAX_MESSAGE)throw new IOException("Relay message too large");
        try{return JsonParser.parseString(text).getAsJsonObject();}
        catch(RuntimeException ex){throw new IOException("Invalid relay envelope",ex);}
    }
    static byte[] decode(String text) throws IOException {
        if(text.length()>349528)throw new IOException("Emote packet too large");
        try {
            byte[] bytes=Base64.getDecoder().decode(text);
            if(bytes.length==0||bytes.length>MAX_PACKET)throw new IOException("Invalid emote packet size");
            return bytes;
        }catch(IllegalArgumentException ex){throw new IOException("Invalid emote packet",ex);}
    }
}
