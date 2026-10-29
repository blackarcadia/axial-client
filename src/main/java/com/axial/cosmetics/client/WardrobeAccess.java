package com.axial.cosmetics.client;

import java.util.UUID;
import net.minecraft.client.MinecraftClient;

/** Local account gate; never grants access based on a name or a cosmetic setting. */
public final class WardrobeAccess {
    private WardrobeAccess() { }

    public static boolean allows(UUID account, UUID player) {
        return CreatorCosmetics.OWNER.equals(account) && (player == null || account.equals(player));
    }

    public static boolean allowed() {
        MinecraftClient client = MinecraftClient.getInstance();
        return allows(client.getSession().getUuidOrNull(), client.player == null ? null : client.player.getUuid());
    }
}
