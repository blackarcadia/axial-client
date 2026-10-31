package com.axial.cosmetics.client;

import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HexwindAuraTest {
    @Test void geometryIsFiniteBoundedAnimatedAndSeamless() {
        for(float t:new float[]{0,.25f,19,60,119.9f,120,240,-1}) {
            var mesh=HexwindGeometry.frame(t,true);assertTrue(mesh.size()>150&&mesh.size()<650);
            for(var quad:mesh)for(var p:List.of(quad.a(),quad.b(),quad.c(),quad.d())) {
                assertTrue(Float.isFinite(p.x())&&Float.isFinite(p.y())&&Float.isFinite(p.z()));
                assertTrue(Math.abs(p.x())<1.4&&Math.abs(p.z())<1.4);assertTrue(p.y()>-.1&&p.y()<2.1);
            }
        }
        assertEquals(HexwindGeometry.frame(0,true),HexwindGeometry.frame(HexwindGeometry.PERIOD,true));
        assertNotEquals(HexwindGeometry.frame(0,true),HexwindGeometry.frame(15,true));
        assertTrue(HexwindGeometry.frame(0,false).size()<HexwindGeometry.frame(0,true).size());
    }
    @Test void auraGrantsAreDistinctFromCapeGrantsAndNoImplicitGrantExists() throws Exception {
        UUID uuid=UUID.fromString("12345678-1234-4234-8234-123456789012");
        var grants=CosmeticAssignments.parse("{\"version\":1,\"players\":{\""+uuid+"\":[\"halloween_hexwind\",\"staff\"]}}");
        assertEquals(java.util.Set.of("halloween_hexwind","staff"),grants.get(uuid));
        assertEquals(WardrobeAura.HEXWIND,WardrobeAura.fromId("halloween_hexwind"));
        assertFalse(WardrobeAura.HEXWIND.ownedBy(CreatorCosmetics.OWNER));
        var preferences=new java.util.Properties();preferences.setProperty(uuid+".aura","none");
        assertNull(WardrobePreferences.auraSelection(preferences,uuid));
        assertNull(WardrobePreferences.auraSelection(preferences,null));
    }
    @Test void auraOnlyOwnersCanOpenTheWardrobeAndRevocationClosesAccess() throws Exception {
        UUID owner=UUID.fromString("12345678-1234-4234-8234-123456789012");
        var field=CosmeticAssignments.class.getDeclaredField("grants");field.setAccessible(true);Object previous=field.get(null);
        try {
            field.set(null,java.util.Map.of(owner,java.util.Set.of("halloween_hexwind")));
            assertTrue(WardrobeAccess.allows(owner,null));assertTrue(WardrobeAccess.allows(owner,owner));
            assertFalse(WardrobeAccess.allows(owner,UUID.randomUUID()));assertTrue(WardrobeCape.owned(owner).isEmpty());
            assertEquals(java.util.List.of(WardrobeAura.HEXWIND),WardrobeAura.owned(owner));
            assertEquals(1,WardrobeItem.owned(owner,WardrobeCategory.AURAS).size());
            assertTrue(WardrobeItem.owned(owner,WardrobeCategory.CAPES).isEmpty());
            var preferences=new java.util.Properties();preferences.setProperty(owner.toString(),"minecraft");
            assertEquals(WardrobeAura.HEXWIND,WardrobePreferences.auraSelection(preferences,owner));
            preferences.setProperty(owner+".aura","none");assertNull(WardrobePreferences.auraSelection(preferences,owner));
            field.set(null,java.util.Map.of(owner,java.util.Set.of()));assertFalse(WardrobeAccess.allows(owner,null));
        }finally{field.set(null,previous);}
    }
    @Test void previewFramesAndUtilityTextureAreBundledAndReadable() throws Exception {
        try(var input=getClass().getResourceAsStream("/assets/axial_cosmetics/textures/gui/hexwind_preview.png")) {
            assertNotNull(input);var atlas=ImageIO.read(input);assertEquals(1024,atlas.getWidth());assertEquals(1024,atlas.getHeight());
            boolean animated=false;
            for(int y=0;y<128;y++)for(int x=0;x<128;x++)animated|=atlas.getRGB(x,y)!=atlas.getRGB(x+128,y);
            assertTrue(animated);
        }
        try(var input=getClass().getResourceAsStream("/assets/axial_cosmetics/textures/entity/auras/white.png")) {
            assertNotNull(input);assertEquals(0xffffffff,ImageIO.read(input).getRGB(0,0));
        }
    }
}
