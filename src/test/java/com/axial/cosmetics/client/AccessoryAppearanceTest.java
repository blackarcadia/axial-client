package com.axial.cosmetics.client;

import com.google.gson.*;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccessoryAppearanceTest {
    @Test void combinedLoadoutAndLegacyFallbackRoundTrip()throws Exception{
        var message=new JsonObject();AccessoryAppearance.write(message,List.of(WardrobeAccessory.SWIM_BANDS,WardrobeAccessory.WHITE_KICKS));
        assertEquals(List.of("swim_bands","white_kicks"),AccessoryAppearance.read(message));
        assertEquals("swim_bands",message.get("accessory").getAsString());
        message.remove("accessories");assertEquals(List.of("swim_bands"),AccessoryAppearance.read(message));
        message.add("accessories",new JsonArray());assertTrue(AccessoryAppearance.read(message).isEmpty());
        AccessoryAppearance.write(message,List.of());assertTrue(message.get("accessory").isJsonNull());assertTrue(AccessoryAppearance.read(message).isEmpty());
    }
    @Test void malformedAndOversizedLoadoutsAreRejected(){
        for(String json:List.of("{\"accessories\":null}","{\"accessories\":[1]}","{\"accessories\":[\"swim_bands\",\"swim_bands\"]}","{\"accessories\":[\"../bad\"]}"))
            assertThrows(IOException.class,()->AccessoryAppearance.read(JsonParser.parseString(json).getAsJsonObject()));
        var object=new JsonObject();var values=new JsonArray();for(int i=0;i<17;i++)values.add("item_"+i);object.add("accessories",values);
        assertThrows(IOException.class,()->AccessoryAppearance.read(object));
    }
}
