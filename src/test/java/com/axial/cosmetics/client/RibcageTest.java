package com.axial.cosmetics.client;

import com.axial.cosmetics.data.CosmeticSlot;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RibcageTest {
    @Test void mirroredRibsReverseTheNativeFrontFaceTextureDirection() {
        var cubes=new HashMap<String,net.minecraft.client.model.ModelPart.Cuboid>();
        RibcageFeatureRenderer.model().forEachCuboid(new net.minecraft.client.util.math.MatrixStack(),(entry,path,index,cube)->{
            if(path.endsWith("/cube10"))cubes.put("normal",cube);
            if(path.endsWith("/cube41"))cubes.put("mirrored",cube);
        });
        assertEquals(2,cubes.size());
        for(var entry:cubes.entrySet()) {
            var cube=entry.getValue();
            var front=Arrays.stream(cube.sides).filter(side->side.direction().z()==-1).findFirst().orElseThrow();
            var left=Arrays.stream(front.vertices()).min(Comparator.comparingDouble(v->v.x())).orElseThrow();
            assertEquals(entry.getKey().equals("normal")?9f/32:12f/32,left.u(),.00001f);
        }
    }
    @Test void sourceBonesMirroringInflationAndTexturesArePreserved() throws Exception {
        var bones=BedrockCosmeticGeometry.load("ribcage");
        assertEquals(2,bones.size());
        var body=bones.stream().filter(b->b.name().equals("bipedBody")).findFirst().orElseThrow();
        assertEquals(new BedrockCosmeticGeometry.Vec(0,24,0),body.pivot());
        assertEquals(42,body.cubes().size());
        assertEquals(16,body.cubes().stream().filter(BedrockCosmeticGeometry.Cube::mirror).count());
        assertEquals(16,body.cubes().stream().filter(c->c.inflate()==.01f).count());
        assertEquals(Set.of(CosmeticSlot.CHEST),WardrobeAccessory.RIBCAGE.slots);
        for(String path:List.of("textures/entity/accessories/ribcage.png","textures/gui/ribcage_preview.png")) {
            try(var input=getClass().getResourceAsStream("/assets/axial_cosmetics/"+path)) {
                assertNotNull(input);var image=ImageIO.read(input);int size=path.contains("preview")?128:32;
                assertEquals(size,image.getWidth());assertEquals(size,image.getHeight());
            }
        }
    }
    @Test void chestGrantStacksWithShoesAndArmBandsAndRevocationRemovesOnlyChest() throws Exception {
        var uuid=UUID.fromString("12345678-1234-4234-8234-123456789012");
        var field=CosmeticAssignments.class.getDeclaredField("grants");field.setAccessible(true);var previous=field.get(null);
        try {
            field.set(null,Map.of(uuid,Set.of("swim_bands","white_sneakers")));
            assertFalse(WardrobeAccessory.RIBCAGE.ownedBy(uuid));
            field.set(null,Map.of(uuid,Set.of("swim_bands","white_sneakers","ribcage")));
            assertTrue(WardrobeAccessory.RIBCAGE.ownedBy(uuid));
            var values=new Properties();values.setProperty(uuid+".accessories","swim_bands,white_sneakers");
            values.setProperty(uuid+".cape","purrr");values.setProperty(uuid+".aura","gold_spark");
            var updated=WardrobePreferences.accessoryToggle(values,uuid,WardrobeAccessory.RIBCAGE);
            assertEquals(List.of(WardrobeAccessory.SWIM_BANDS,WardrobeAccessory.WHITE_SNEAKERS,WardrobeAccessory.RIBCAGE),WardrobePreferences.accessorySelections(updated,uuid));
            assertEquals("purrr",updated.getProperty(uuid+".cape"));assertEquals("gold_spark",updated.getProperty(uuid+".aura"));
            var removed=WardrobePreferences.accessoryToggle(updated,uuid,WardrobeAccessory.RIBCAGE);
            assertEquals(List.of(WardrobeAccessory.SWIM_BANDS,WardrobeAccessory.WHITE_SNEAKERS),WardrobePreferences.accessorySelections(removed,uuid));
            field.set(null,Map.of(uuid,Set.of("swim_bands","white_sneakers")));
            assertEquals(List.of(WardrobeAccessory.SWIM_BANDS,WardrobeAccessory.WHITE_SNEAKERS),WardrobePreferences.accessorySelections(updated,uuid));
        } finally {field.set(null,previous);}
    }
}
