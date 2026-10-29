package com.axial.cosmetics.client;

import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.nio.file.Path;
import java.util.jar.JarFile;
import javax.imageio.ImageIO;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WardrobeCapeTest {
    private static final UUID STAFF_ONE=UUID.fromString("8ef7a848-7c77-4449-92a9-b2fb30b4cfad");
    private static final UUID STAFF_TWO=UUID.fromString("a0e00d14-fce1-4ed2-9577-d83a3feedfee");
    private static final UUID STAFF_THREE=UUID.fromString("8d79d555-75e5-4456-bbc4-0268e1d21659");
    @Test void lunarRavenIsExclusiveToItsRecipientAndGrantsWardrobeAccess() {
        UUID recipient=UUID.fromString("499b254f-45e3-4ad9-b1ed-6a388b56410c");
        assertEquals(List.of(WardrobeCape.LUNAR_RAVEN),WardrobeCape.owned(recipient));
        assertTrue(WardrobeAccess.allows(recipient,null));
        assertTrue(WardrobeAccess.allows(recipient,recipient));
        assertFalse(WardrobeAccess.allows(recipient,CreatorCosmetics.OWNER));
        for(UUID other:List.of(CreatorCosmetics.OWNER,STAFF_ONE,STAFF_TWO,STAFF_THREE,new UUID(0,1)))
            assertFalse(WardrobeCape.LUNAR_RAVEN.ownedBy(other));
        assertEquals(WardrobeCape.LUNAR_RAVEN,WardrobePreferences.selection(new Properties(),recipient));
        var values=new Properties();values.setProperty(recipient.toString(),"minecraft");
        assertNull(WardrobePreferences.selection(values,recipient));
        values.setProperty(recipient.toString(),"admin");
        assertEquals(WardrobeCape.LUNAR_RAVEN,WardrobePreferences.selection(values,recipient));
    }
    @Test void exactlyTheGrantedAccountsOwnAdminAndCanOpenTheirWardrobes() {
        assertEquals(List.of(WardrobeCape.GOLD_OWNER,WardrobeCape.ADMIN,WardrobeCape.STAFF),WardrobeCape.owned(CreatorCosmetics.OWNER));
        for(UUID staff:List.of(STAFF_ONE,STAFF_TWO)) {
            assertEquals(List.of(WardrobeCape.ADMIN,WardrobeCape.STAFF),WardrobeCape.owned(staff));
            assertTrue(WardrobeAccess.allows(staff,null));assertTrue(WardrobeAccess.allows(staff,staff));
            assertFalse(WardrobeAccess.allows(staff,CreatorCosmetics.OWNER));
        }
        assertTrue(WardrobeCape.owned(new UUID(0,1)).isEmpty());
        assertFalse(WardrobeAccess.allows(new UUID(0,1),null));
        assertTrue(WardrobeCape.owned(null).isEmpty());
    }
    @Test void staffCapeIsGrantedToAllFourAccountsWithoutGrantingAdminToTheNewStaffMember() {
        assertEquals(List.of(WardrobeCape.STAFF),WardrobeCape.owned(STAFF_THREE));
        assertTrue(WardrobeAccess.allows(STAFF_THREE,null));
        assertTrue(WardrobeAccess.allows(STAFF_THREE,STAFF_THREE));
        assertFalse(WardrobeCape.ADMIN.ownedBy(STAFF_THREE));
        assertFalse(WardrobeCape.GOLD_OWNER.ownedBy(STAFF_THREE));
        for(UUID uuid:List.of(CreatorCosmetics.OWNER,STAFF_ONE,STAFF_TWO,STAFF_THREE))assertTrue(WardrobeCape.STAFF.ownedBy(uuid));
        assertFalse(WardrobeCape.STAFF.ownedBy(new UUID(0,1)));
        var values=new Properties();values.setProperty(STAFF_THREE.toString(),"admin");
        assertEquals(WardrobeCape.STAFF,WardrobePreferences.selection(values,STAFF_THREE));
        values.setProperty(STAFF_ONE.toString(),"staff");
        assertEquals(WardrobeCape.STAFF,WardrobePreferences.selection(values,STAFF_ONE));
        assertNull(WardrobePreferences.nextSelection(WardrobeCape.STAFF,WardrobeCape.STAFF));
        assertEquals(WardrobeCape.STAFF,WardrobePreferences.nextSelection(WardrobeCape.ADMIN,WardrobeCape.STAFF));
    }
    @Test void perAccountSelectionsCannotGrantSomeoneElsesCapeAndRemainIndependent() {
        var values=new Properties();
        values.setProperty(CreatorCosmetics.OWNER.toString(),"admin");
        values.setProperty(STAFF_ONE.toString(),"minecraft");
        values.setProperty(STAFF_TWO.toString(),"gold_owner");
        assertEquals(WardrobeCape.ADMIN,WardrobePreferences.selection(values,CreatorCosmetics.OWNER));
        assertNull(WardrobePreferences.selection(values,STAFF_ONE));
        assertEquals(WardrobeCape.ADMIN,WardrobePreferences.selection(values,STAFF_TWO));
        assertNull(WardrobePreferences.selection(values,new UUID(0,1)));
        assertNull(WardrobePreferences.selection(values,null));
        assertEquals(WardrobeCape.GOLD_OWNER,WardrobePreferences.selection(new Properties(),CreatorCosmetics.OWNER));
    }
    @Test void adminReplacesOnlyTheCapeAndNeverTouchesSkinElytraOrUnentitledAccounts() {
        var body=new AssetInfo.TextureAssetInfo(Identifier.of("test","skin"));
        var vanilla=new AssetInfo.TextureAssetInfo(Identifier.of("test","mojang_cape"));
        var elytra=new AssetInfo.TextureAssetInfo(Identifier.of("test","elytra"));
        var original=new SkinTextures(body,vanilla,elytra,PlayerSkinType.SLIM,true);
        for(UUID uuid:List.of(CreatorCosmetics.OWNER,STAFF_ONE,STAFF_TWO)) {
            for(int frame=0;frame<16;frame++) {
                var result=WardrobeCape.ADMIN.apply(uuid,original,frame*5);
                assertTrue(WardrobeCape.ADMIN.hasTexture(result));
                assertSame(body,result.body());assertSame(elytra,result.elytra());
                assertEquals(original.model(),result.model());assertTrue(result.secure());
            }
        }
        assertSame(original,WardrobeCape.ADMIN.apply(new UUID(0,1),original,0));
        assertSame(original,WardrobeCape.GOLD_OWNER.apply(STAFF_ONE,original,0));
    }
    @Test void everyAnimatedCapeFrameIsInTheProductionJarWithCorrectUvDimensions() throws Exception {
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())) {
            for(var cape:WardrobeCape.values())for(int frame=0;frame<16;frame++) {
                String path="assets/"+cape.texture(frame*5).getNamespace()+"/"+cape.texture(frame*5).getPath();
                var entry=jar.getJarEntry(path);assertNotNull(entry,path);
                try(var input=jar.getInputStream(entry)) {
                    var image=ImageIO.read(input);assertNotNull(image);
                    assertEquals(2048,image.getWidth());assertEquals(1024,image.getHeight());
                }
            }
        }
    }
}
