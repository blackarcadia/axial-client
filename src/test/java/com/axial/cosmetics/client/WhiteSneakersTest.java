package com.axial.cosmetics.client;

import com.axial.cosmetics.data.CosmeticSlot;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WhiteSneakersTest {
    @Test void suppliedModelTextureAndPreviewAreAvailableSeparatelyFromWhiteKicks() throws Exception {
        var bones=WhiteKicksGeometry.load("white_sneakers");
        assertEquals(6,bones.size());
        assertEquals(8,bones.stream().mapToInt(b->b.cubes().size()).sum());
        var left=bones.stream().filter(b->b.name().equals("bipedLeftLeg")).findFirst().orElseThrow();
        assertEquals(new WhiteKicksGeometry.Vec(2,3,0),left.pivot());
        assertEquals(180,left.rotation().y());
        var boot=bones.stream().filter(b->b.name().equals("armorLeftBoot")).findFirst().orElseThrow();
        assertEquals(4,boot.cubes().size());
        assertEquals(new WhiteKicksGeometry.Vec(5.25f,1.25f,7.125f),boot.cubes().get(2).size());
        assertEquals(-22.5f,boot.cubes().get(3).rotation().x());
        var right=bones.stream().filter(b->b.name().equals("armorRightBoot")).findFirst().orElseThrow();
        assertEquals(new WhiteKicksGeometry.Vec(157.5f,0,-180),right.cubes().getFirst().rotation());
        assertEquals(Set.of(CosmeticSlot.BOOTS),WardrobeAccessory.WHITE_SNEAKERS.slots);
        assertSame(WardrobeAccessory.WHITE_SNEAKERS,WardrobeAccessory.fromId("white_sneakers"));
        assertNotEquals(WardrobeAccessory.WHITE_KICKS.preview(),WardrobeAccessory.WHITE_SNEAKERS.preview());
        for(var path:List.of("textures/entity/accessories/white_sneakers.png","textures/gui/white_sneakers_preview.png")) {
            try(var input=getClass().getResourceAsStream("/assets/axial_cosmetics/"+path)) {
                assertNotNull(input);var image=ImageIO.read(input);
                int size=path.contains("preview")?128:32;
                assertEquals(size,image.getWidth());assertEquals(size,image.getHeight());
            }
        }
    }

    @Test void grantIsIndependentAndChangingShoesPreservesOtherSlots() throws Exception {
        var uuid=UUID.fromString("12345678-1234-4234-8234-123456789012");
        var field=CosmeticAssignments.class.getDeclaredField("grants");field.setAccessible(true);var previous=field.get(null);
        try {
            field.set(null,Map.of(uuid,Set.of("white_kicks","swim_bands")));
            assertFalse(WardrobeAccessory.WHITE_SNEAKERS.ownedBy(uuid));
            field.set(null,Map.of(uuid,Set.of("white_kicks","white_sneakers","swim_bands")));
            assertTrue(WardrobeAccessory.WHITE_SNEAKERS.ownedBy(uuid));
            assertEquals(3,WardrobeItem.owned(uuid,WardrobeCategory.OUTFITS).size());
            var values=new Properties();values.setProperty(uuid+".accessories","swim_bands,white_kicks");
            var changed=WardrobePreferences.accessoryToggle(values,uuid,WardrobeAccessory.WHITE_SNEAKERS);
            assertEquals(List.of(WardrobeAccessory.SWIM_BANDS,WardrobeAccessory.WHITE_SNEAKERS),WardrobePreferences.accessorySelections(changed,uuid));
            assertEquals(List.of(WardrobeAccessory.SWIM_BANDS,WardrobeAccessory.WHITE_KICKS),WardrobePreferences.accessorySelections(values,uuid));
            field.set(null,Map.of(uuid,Set.of("swim_bands")));
            assertEquals(List.of(WardrobeAccessory.SWIM_BANDS),WardrobePreferences.accessorySelections(changed,uuid));
        } finally {field.set(null,previous);}
    }
}
