package com.axial.cosmetics.client;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.UUID;
import java.util.jar.JarFile;
import javax.imageio.ImageIO;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HalloweenSkinsTest {
    @Test void assignmentsAreStableAndUseAllThreeVariantsWithoutAPlayerCache() {
        var variants=new HashSet<Integer>();
        for(int n=0;n<1000;n++) {
            UUID id=new UUID(0,n);int expected=HalloweenSkins.variant(id,12345L);
            assertTrue(expected>=0&&expected<3);variants.add(expected);
            assertEquals(expected,HalloweenSkins.variant(id,12345L));
        }
        assertEquals(3,variants.size());
    }
    @Test void skinAndArmModelChangeTogetherWhileCapesElytraAndSecurityRemainIntact() {
        var cape=new AssetInfo.TextureAssetInfo(Identifier.of("test","cape"));
        var elytra=new AssetInfo.TextureAssetInfo(Identifier.of("test","elytra"));
        var original=new SkinTextures(new AssetInfo.TextureAssetInfo(Identifier.of("test","original")),cape,elytra,PlayerSkinType.WIDE,false);
        var assigned=new HashSet<String>();
        for(int n=0;n<100;n++) {
            UUID id=new UUID(0x4000L,0x8000000000000000L|n);var skin=HalloweenSkins.apply(id,original);String texture=skin.body().texturePath().getPath();assigned.add(texture);
            assertTrue(texture.startsWith("textures/entity/halloween/skin_"));
            assertEquals(texture.endsWith("skin_3.png")?PlayerSkinType.WIDE:PlayerSkinType.SLIM,skin.model());
            assertSame(cape,skin.cape());assertSame(elytra,skin.elytra());assertFalse(skin.secure());
            assertSame(skin,HalloweenSkins.apply(id,skin)); // Entry and entity hooks agree without reallocating.
            assertEquals(skin,HalloweenSkins.apply(id,original));
        }
        assertEquals(3,assigned.size());assertSame(original,HalloweenSkins.apply(null,original));assertNull(HalloweenSkins.apply(UUID.randomUUID(),null));
    }
    @Test void remoteOnlineAndOfflinePlayersDoNotDependOnTabListVisibility() {
        UUID remote=UUID.fromString("8ef7a848-7c77-4449-92a9-b2fb30b4cfad");
        UUID offline=UUID.nameUUIDFromBytes("OfflinePlayer:RemotePlayer".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var original=new SkinTextures(new AssetInfo.TextureAssetInfo(Identifier.of("test","remote_skin")),null,null,PlayerSkinType.WIDE,true);
        for(UUID player:java.util.List.of(remote,offline)) {
            assertTrue(HalloweenSkins.isActualPlayer(player));
            assertNotEquals(original.body(),HalloweenSkins.apply(player,original).body());
            assertTrue(HalloweenSkins.apply(player,original).secure());
        }
    }
    @Test void citizensNpcProfilesKeepTheirOriginalTextures() {
        UUID source=UUID.fromString("8ef7a848-7c77-4449-92a9-b2fb30b4cfad");
        UUID npc=new UUID((source.getMostSignificantBits()&~0xf000L)|0x2000L,source.getLeastSignificantBits());
        var original=new SkinTextures(new AssetInfo.TextureAssetInfo(Identifier.of("test","npc_skin")),null,null,PlayerSkinType.SLIM,true);
        assertEquals(2,npc.version());assertFalse(HalloweenSkins.isActualPlayer(npc));
        assertSame(original,HalloweenSkins.apply(npc,original));
        assertFalse(HalloweenSkins.isActualPlayer(null));
        assertFalse(HalloweenSkins.isActualPlayer(new UUID(0,0)));
    }
    @Test void originalUploadedTexturesArePackagedWithTheirCorrectArmLayouts() throws Exception {
        String[] hashes={"dfa9071b6fc8cf1ce9716ee14e5b8972708462eb6c7d7f89e81fd94c393d5d96", "806fad2ee42278534fd8ec9b67a4bd16e314436edbbb86313953358ebcaa23cf", "dc4440b7f74ec9d763c0e413d3115ca8907270866282b0cd772f55c4e5408ce5"};
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())) {
            for(int i=1;i<=3;i++) {
                var entry=jar.getJarEntry("assets/axial_cosmetics/textures/entity/halloween/skin_"+i+".png");assertNotNull(entry);
                byte[] bytes;try(var input=jar.getInputStream(entry)){bytes=input.readAllBytes();}
                assertEquals(hashes[i-1],HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
                var image=ImageIO.read(new java.io.ByteArrayInputStream(bytes));assertEquals(64,image.getWidth());assertEquals(64,image.getHeight());
                int armPixels=0;
                for(int y=20;y<32;y++)for(int x=54;x<56;x++)if((image.getRGB(x,y)>>>24)!=0)armPixels++;
                for(int y=52;y<64;y++)for(int x=46;x<48;x++)if((image.getRGB(x,y)>>>24)!=0)armPixels++;
                assertEquals(i==3?48:0,armPixels);
            }
        }
    }
}
