package com.axial.cosmetics.client;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;

/** Local Halloween takeover. Each UUID keeps its randomly assigned look for this client run. */
public final class HalloweenSkins {
    private static final long SESSION_SEED=ThreadLocalRandom.current().nextLong();
    private static final AssetInfo.TextureAsset[] SKINS={asset(1),asset(2),asset(3)};
    private static final PlayerSkinType[] MODELS={PlayerSkinType.SLIM,PlayerSkinType.SLIM,PlayerSkinType.WIDE};
    private HalloweenSkins() { }

    private static AssetInfo.TextureAsset asset(int number) {
        return new AssetInfo.TextureAssetInfo(Identifier.of("axial_cosmetics","halloween_skin_"+number),
                Identifier.of("axial_cosmetics","textures/entity/halloween/skin_"+number+".png"));
    }
    static int variant(UUID uuid,long seed) {
        // Mix the UUID with a per-launch random seed. No growing player cache or frame-time RNG.
        long value=uuid.getMostSignificantBits()^Long.rotateLeft(uuid.getLeastSignificantBits(),23)^seed;
        value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;
        value=(value^(value>>>27))*0x94d049bb133111ebL;
        value^=value>>>31;
        return (int)Math.floorMod(value,3L);
    }
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
    public static SkinTextures apply(UUID uuid,SkinTextures original) {
        if(!isActualPlayer(uuid)||original==null)return original;
        int selected=variant(uuid,SESSION_SEED);
        if(SKINS[selected].equals(original.body())&&MODELS[selected]==original.model())return original;
        return new SkinTextures(SKINS[selected],original.cape(),original.elytra(),MODELS[selected],original.secure());
    }
}
