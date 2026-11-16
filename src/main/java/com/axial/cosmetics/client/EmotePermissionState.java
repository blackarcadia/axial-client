package com.axial.cosmetics.client;

import io.github.kosmx.emotes.main.EmoteHolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Server-provided Emotecraft entitlements for the current connection. */
public final class EmotePermissionState {
    private static volatile Set<UUID> allowed = Set.of();
    private static volatile boolean received;
    private static volatile boolean allowAll;

    private EmotePermissionState() { }

    public static void reset() {
        received=false;allowAll=false;allowed=Set.of();
    }

    public static void update(String payload) {
        if ("*".equals(payload)) {
            allowAll = true;
            allowed = Set.of();
            received = true;
            return;
        }

        Set<UUID> updated = new HashSet<>();
        for (String value : payload.split(",")) {
            try {
                if (!value.isBlank()) updated.add(UUID.fromString(value.trim()));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed entries so one bad entry cannot expose all emotes.
            }
        }
        allowAll = false;
        allowed = Set.copyOf(updated);
        received = true;
    }

    public static boolean canUse(EmoteHolder emote) {
        return canUse(emote.getUuid(),net.minecraft.client.MinecraftClient.getInstance().getSession().getUuidOrNull());
    }
    static boolean canUse(UUID animation,UUID account) {
        var managed=WardrobeEmote.fromAnimation(animation);
        return (managed==null||managed.ownedBy(account))&&(!received||allowAll||allowed.contains(animation));
    }
    public static Iterable<EmoteHolder> filter(Iterable<EmoteHolder> emotes) {
        List<EmoteHolder> permitted = new ArrayList<>();
        for (EmoteHolder emote : emotes) {
            if(canUse(emote))permitted.add(emote);
        }
        return permitted;
    }
}
