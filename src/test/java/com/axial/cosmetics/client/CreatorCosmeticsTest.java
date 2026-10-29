package com.axial.cosmetics.client;

import java.util.UUID;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CreatorCosmeticsTest {
    private static AssetInfo.TextureAsset texture(String name) {
        return new AssetInfo.TextureAssetInfo(Identifier.of("test", name));
    }

    @Test void creatorGetsCapeWithoutChangingSkinElytraOrAccountMetadata() {
        var original = new SkinTextures(texture("skin"), texture("cape"), texture("elytra"), PlayerSkinType.SLIM, true);
        var result = CreatorCosmetics.applyCape(CreatorCosmetics.OWNER, original);
        assertEquals(CreatorCosmetics.CAPE, result.cape());
        assertSame(original.body(), result.body());
        assertSame(original.elytra(), result.elytra());
        assertEquals(original.model(), result.model());
        assertEquals(original.secure(), result.secure());
        assertTrue(CreatorCosmetics.hasCreatorCape(result));
    }

    @Test void otherPlayersKeepTheirOwnCosmeticsAndDoNotGetSigil() {
        var original = new SkinTextures(texture("skin"), texture("cape"), null, PlayerSkinType.WIDE, false);
        assertSame(original, CreatorCosmetics.applyCape(UUID.fromString("6a9cccc8-8e00-4a73-bd37-1121f81b0675"), original));
        assertFalse(CreatorCosmetics.hasCreatorCape(original));
        assertNull(CreatorCosmetics.applyCape(CreatorCosmetics.OWNER, null));
        assertFalse(CreatorCosmetics.hasCreatorCape(null));
    }

    @Test void creatorDoesNotNeedAnExistingMojangCape() {
        var original = new SkinTextures(texture("skin"), null, null, PlayerSkinType.WIDE, false);
        assertTrue(CreatorCosmetics.hasCreatorCape(CreatorCosmetics.applyCape(CreatorCosmetics.OWNER, original)));
        assertFalse(CreatorCosmetics.hasCreatorCape(original));
    }

    @Test void animationLoopsAndEveryFrameRetainsCreatorIdentity() {
        var original = new SkinTextures(texture("skin"), null, texture("elytra"), PlayerSkinType.WIDE, true);
        for (int i = 0; i < CreatorCosmetics.FRAME_COUNT; i++) {
            var frame = CreatorCosmetics.applyCape(CreatorCosmetics.OWNER, original, i * 5f);
            assertTrue(CreatorCosmetics.hasCreatorCape(frame));
            assertSame(original.elytra(), frame.elytra());
            assertSame(original, CreatorCosmetics.applyCape(new UUID(0, 0), original, i * 5f));
        }
        assertEquals(0, CreatorCosmetics.frameIndex(0));
        assertEquals(0, CreatorCosmetics.frameIndex(4.99f));
        assertEquals(1, CreatorCosmetics.frameIndex(5));
        assertEquals(15, CreatorCosmetics.frameIndex(79.99f));
        assertEquals(0, CreatorCosmetics.frameIndex(80));
        assertEquals(15, CreatorCosmetics.frameIndex(-1));
    }

    @Test void sigilTravelsAlongSquareRatherThanCuttingAcrossCorners() {
        assertArrayEquals(new float[]{-5.5f, -5.5f}, CreatorSigilFeatureRenderer.perimeterPosition(0));
        assertArrayEquals(new float[]{0, -5.5f}, CreatorSigilFeatureRenderer.perimeterPosition(20));
        assertArrayEquals(new float[]{5.5f, -5.5f}, CreatorSigilFeatureRenderer.perimeterPosition(40));
        assertArrayEquals(new float[]{5.5f, 5.5f}, CreatorSigilFeatureRenderer.perimeterPosition(80));
        assertArrayEquals(new float[]{-5.5f, 5.5f}, CreatorSigilFeatureRenderer.perimeterPosition(120));
        assertArrayEquals(CreatorSigilFeatureRenderer.perimeterPosition(0), CreatorSigilFeatureRenderer.perimeterPosition(160));
    }
}
