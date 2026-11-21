package com.axial.cosmetics.client;

import com.google.gson.JsonParser;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;

/** VPS roles are deliberately never loaded from the editable cosmetics disk cache. */
public final class DeveloperAccess {
    private static volatile Set<UUID> developers=Set.of();
    private DeveloperAccess() {}
    public static boolean allowed() {
        UUID account=MinecraftClient.getInstance().getSession().getUuidOrNull();
        return account!=null&&developers.contains(account);
    }
    static void update(String json) {
        Set<UUID> next=new HashSet<>();
        try {
            var values=JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("developers");
            if(values!=null)for(var value:values)next.add(UUID.fromString(value.getAsString()));
        } catch(RuntimeException ex){next.clear();}
        developers=Set.copyOf(next);
    }
}
