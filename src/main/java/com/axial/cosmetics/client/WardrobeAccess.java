package com.axial.cosmetics.client;

import java.util.UUID;
import net.minecraft.client.MinecraftClient;

/** Local UUID gate for accounts with bundled wardrobe entitlements. */
public final class WardrobeAccess {
    private WardrobeAccess() { }

    public static boolean allows(UUID account, UUID player) {
        return !WardrobeCape.owned(account).isEmpty() && (player == null || account.equals(player));
    }

    public static boolean allowed() {
        MinecraftClient client = MinecraftClient.getInstance();
        return allows(client.getSession().getUuidOrNull(), client.player == null ? null : client.player.getUuid());
    }
}
