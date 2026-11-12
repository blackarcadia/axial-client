package com.axial.cosmetics.client;

import com.google.gson.*;
import java.io.IOException;
import java.util.*;

/** Additive wire format: updated peers use a list, old peers retain the first accessory. */
final class AccessoryAppearance {
    private AccessoryAppearance() {}
    static List<String> read(JsonObject message)throws IOException{
        var result=new ArrayList<String>();
        try{
            if(message.has("accessories")){
                var list=message.getAsJsonArray("accessories");if(list.size()>16)throw new IllegalArgumentException();
                for(var value:list){
                    if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString())throw new IllegalArgumentException();
                    result.add(value.getAsString());
                }
            }else{
                var value=message.get("accessory");
                if(value!=null&&!value.isJsonNull()){
                    if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString())throw new IllegalArgumentException();
                    result.add(value.getAsString());
                }
            }
            var unique=new HashSet<String>();
            for(String id:result)if(!id.matches("[a-z0-9_]{1,64}")||!unique.add(id))throw new IllegalArgumentException();
            return List.copyOf(result);
        }catch(RuntimeException ex){throw new IOException("Invalid accessory state",ex);}
    }
    static void write(JsonObject message,List<WardrobeAccessory> items){
        var list=new JsonArray();for(var item:items)list.add(item.id);message.add("accessories",list);
        if(items.isEmpty())message.add("accessory",JsonNull.INSTANCE);else message.addProperty("accessory",items.getFirst().id);
    }
}
