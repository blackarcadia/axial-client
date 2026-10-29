package com.axial.cosmetics.client;

import java.util.UUID;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CreatorCosmeticsTest {
    @Test void unequippingCustomCapeRestoresOriginalMinecraftCapeAcrossRepeatedToggles() {
        var original=new SkinTextures(texture("skin"),texture("minecraft_cape"),texture("elytra"),PlayerSkinType.SLIM,true);
        for(boolean equipped:new boolean[]{false,true,false,true,false}) {
            var result=CreatorCosmetics.applyCape(CreatorCosmetics.OWNER,original,17,equipped);
            if(equipped)assertTrue(CreatorCosmetics.hasCreatorCape(result));
            else assertSame(original,result,"disabled custom cape must retain the complete original skin texture record");
            assertSame(original.body(),result.body());
            assertSame(original.elytra(),result.elytra());
        }
        var noMojangCape=new SkinTextures(texture("skin"),null,null,PlayerSkinType.WIDE,true);
        assertNull(CreatorCosmetics.applyCape(CreatorCosmetics.OWNER,noMojangCape,0,false).cape());
    }
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

    @Test void otherPlayersKeepTheirOwnCosmetics() {
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

}
