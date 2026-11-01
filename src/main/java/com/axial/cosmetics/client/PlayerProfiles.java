package com.axial.cosmetics.client;

import java.util.UUID;

/** Player UUID classification shared by cosmetic renderers. */
public final class PlayerProfiles {
    private PlayerProfiles() { }

    /**
     * Do not use tab-list membership: custom tab plugins can hide real players there.
     * Online accounts use v4 UUIDs; vanilla offline profiles use v3. Citizens converts
     * its player NPC UUIDs to v2, so those retain their original skins even while
     * temporarily present in player-info packets during skin loading.
     * This is a rendering heuristic, not an account-authentication check.
     * Citizens reference: CitizensAPI AbstractNPC#getMinecraftUniqueId.
     */
    public static boolean isActualPlayer(UUID uuid) {
        return uuid!=null&&(uuid.version()==4||uuid.version()==3);
    }
}
