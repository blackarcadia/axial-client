package com.axial.cosmetics.client;

import java.util.*;
import javax.imageio.ImageIO;
import com.axial.cosmetics.data.CosmeticSlot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BlackSneakersTest {
    @Test void importedFaceUvsKeepSignedSizesAndNativeRotations() {
        var bones=BedrockCosmeticGeometry.load("black_sneakers");
        assertEquals(6,bones.size());assertEquals(8,bones.stream().mapToInt(b->b.cubes().size()).sum());
        var boot=bones.stream().filter(b->b.name().equals("armorLeftBoot")).findFirst().orElseThrow();
        assertEquals(new BedrockCosmeticGeometry.FaceUv(5,12,-5,-7),boot.cubes().getFirst().faces().get("up"));
        assertEquals(22.5f,boot.cubes().get(3).rotation().x());
        var baked=WhiteKicksFeatureRenderer.model("black_sneakers").getChild("left").getChild("bipedLeftLeg").getChild("armorLeftBoot").getChild("cube0");
        int[] count={0};
        baked.forEachCuboid(new net.minecraft.client.util.math.MatrixStack(),(matrix,path,index,cube)->{
            for(var face:cube.sides){
                String name=face.direction().x()<-.5?"east":face.direction().x()>.5?"west":face.direction().y()<-.5?"up":face.direction().y()>.5?"down":face.direction().z()<0?"north":"south";
                var uv=boot.cubes().getFirst().faces().get(name);var vertices=face.vertices();
                assertEquals((uv.u()+uv.width())/32,vertices[0].u(),.000001);
                assertEquals(uv.v()/32,vertices[0].v(),.000001);
                assertEquals(uv.u()/32,vertices[2].u(),.000001);
                assertEquals((uv.v()+uv.height())/32,vertices[2].v(),.000001);count[0]++;
            }
        });
        assertEquals(6,count[0]);
    }
    @Test void blackShoesUseTheirOwnGrantAndReplaceOnlyTheBootSlot() throws Exception {
        assertEquals(Set.of(CosmeticSlot.BOOTS),WardrobeAccessory.BLACK_SNEAKERS.slots);
        var uuid=UUID.randomUUID();var field=CosmeticAssignments.class.getDeclaredField("grants");field.setAccessible(true);var old=field.get(null);
        try{
            field.set(null,Map.of(uuid,Set.of("black_sneakers","white_sneakers","ribcage")));
            var p=new Properties();p.setProperty(uuid+".accessories","white_sneakers,ribcage");
            var result=WardrobePreferences.accessorySelections(WardrobePreferences.accessoryToggle(p,uuid,WardrobeAccessory.BLACK_SNEAKERS),uuid);
            assertEquals(List.of(WardrobeAccessory.RIBCAGE,WardrobeAccessory.BLACK_SNEAKERS),result);
            assertFalse(WardrobeAccessory.BLACK_SNEAKERS.ownedBy(UUID.randomUUID()));
        }finally{field.set(null,old);}
        try(var in=getClass().getResourceAsStream("/assets/axial_cosmetics/textures/entity/accessories/black_sneakers.png")){
            assertNotNull(in);var texture=ImageIO.read(in);assertEquals(32,texture.getWidth());assertEquals(32,texture.getHeight());
        }
    }
}
