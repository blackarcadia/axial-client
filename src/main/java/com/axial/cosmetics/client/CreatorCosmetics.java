package com.axial.cosmetics.client;

import java.util.UUID;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;

/** Creator identity is checked by account UUID, never by display name. */
public final class CreatorCosmetics {
    public static final UUID OWNER = UUID.fromString("6a9cccc8-8e00-4a73-bd37-1121f81b0674");
    public static final AssetInfo.TextureAsset CAPE = new AssetInfo.TextureAssetInfo(
            Identifier.of("axial_cosmetics", "creator_cape"),
            Identifier.of("axial_cosmetics", "textures/entity/creator_cape.png"));

    private CreatorCosmetics() { }

    public static SkinTextures applyCape(UUID uuid, SkinTextures original) {
        if (!OWNER.equals(uuid) || original == null) return original;
        return new SkinTextures(original.body(), CAPE, original.elytra(), original.model(), original.secure());
    }

    public static boolean hasCreatorCape(SkinTextures textures) {
        return textures != null && CAPE.equals(textures.cape());
    }
}
