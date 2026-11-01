package com.axial.cosmetics.client;

import java.nio.file.Path;
import java.util.UUID;
import java.util.jar.JarFile;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerProfilesTest {
    @Test void auraRenderingStillIncludesOnlineAndOfflinePlayersAndExcludesNpcs() {
        UUID player=UUID.fromString("8ef7a848-7c77-4449-92a9-b2fb30b4cfad");
        UUID offline=UUID.nameUUIDFromBytes("OfflinePlayer:RemotePlayer".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        UUID npc=new UUID((player.getMostSignificantBits()&~0xf000L)|0x2000L,player.getLeastSignificantBits());
        assertTrue(PlayerProfiles.isActualPlayer(player));
        assertTrue(PlayerProfiles.isActualPlayer(offline));
        assertFalse(PlayerProfiles.isActualPlayer(npc));
        assertFalse(PlayerProfiles.isActualPlayer(null));
        assertFalse(PlayerProfiles.isActualPlayer(new UUID(0,0)));
    }
    @Test void builtClientContainsNoHalloweenSkinOverridesOrTextures() throws Exception {
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())) {
            var entry=jar.getJarEntry("axial-cosmetics.mixins.json");
            var config=JsonParser.parseString(new String(jar.getInputStream(entry).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            config.getAsJsonArray("client").forEach(mixin->assertFalse(mixin.getAsString().startsWith("HalloweenPlayer")));
            for(String name:new String[]{"HalloweenPlayerSkinMixin","HalloweenPlayerListSkinMixin"})
                assertNull(jar.getJarEntry("com/axial/cosmetics/mixin/"+name+".class"));
            assertNull(jar.getJarEntry("com/axial/cosmetics/client/HalloweenSkins.class"));
            for(int i=1;i<=3;i++)
                assertNull(jar.getJarEntry("assets/axial_cosmetics/textures/entity/halloween/skin_"+i+".png"));
        }
    }
}
