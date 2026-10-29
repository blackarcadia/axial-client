package com.axial.cosmetics.client;

import java.util.UUID;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;

/** Creator identity is checked by account UUID, never by display name. */
public final class CreatorCosmetics {
    public static final UUID OWNER = UUID.fromString("6a9cccc8-8e00-4a73-bd37-1121f81b0674");
    public static final int FRAME_COUNT = 16;
    private static final List<AssetInfo.TextureAsset> FRAMES = IntStream.range(0, FRAME_COUNT)
            .mapToObj(i -> (AssetInfo.TextureAsset) new AssetInfo.TextureAssetInfo(
                    Identifier.of("axial_cosmetics", "creator_cape_" + i),
                    Identifier.of("axial_cosmetics", "textures/entity/creator_cape" + (i == 0 ? "" : "_" + i) + ".png")))
            .toList();
    public static final AssetInfo.TextureAsset CAPE = FRAMES.getFirst();

    private CreatorCosmetics() { }

    public static SkinTextures applyCape(UUID uuid, SkinTextures original) {
        return applyCape(uuid, original, 0);
    }

    public static SkinTextures applyCape(UUID uuid, SkinTextures original, float age) {
        if (!OWNER.equals(uuid) || original == null) return original;
        // Entity textures do not consume atlas .mcmeta animation. Select preloaded frames instead.
        return new SkinTextures(original.body(), FRAMES.get(frameIndex(age)), original.elytra(), original.model(), original.secure());
    }

    public static int frameIndex(float age) {
        return Math.floorMod((int) Math.floor(age / 5f), FRAME_COUNT);
    }

    public static Identifier capeTexture(float age) {
        return FRAMES.get(frameIndex(age)).texturePath();
    }

    public static boolean hasCreatorCape(SkinTextures textures) {
        return textures != null && textures.cape() != null && FRAMES.contains(textures.cape());
    }
}
