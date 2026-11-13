package com.axial.cosmetics.client;

import java.util.UUID;
import net.minecraft.client.MinecraftClient;

/** Open the wardrobe for the signed-in account, including an empty collection. */
public final class WardrobeAccess {
    private WardrobeAccess() { }

    public static boolean allows(UUID account, UUID player) {
        return account!=null&&(player==null||account.equals(player));
    }

    public static boolean allowed() {
        MinecraftClient client = MinecraftClient.getInstance();
        return allows(client.getSession().getUuidOrNull(), client.player == null ? null : client.player.getUuid());
    }
}
