package com.axial.cosmetics.client;

import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class WhiteKicksTest {
    @Test void modelRetainsBothShoesEightCubesFractionalSolesAndAuthoredRotations(){
        var bones=WhiteKicksGeometry.load();assertEquals(6,bones.size());
        assertEquals(8,bones.stream().mapToInt(b->b.cubes().size()).sum());
        var left=bones.stream().filter(b->b.name().equals("bipedLeftLeg")).findFirst().orElseThrow();
        assertEquals(0,left.rotation().y());
        var offset=WhiteKicksGeometry.position(left.pivot()).subtract(new WhiteKicksGeometry.Vec(2,12,0));
        assertEquals(0,offset.y()); // Updated source pivots both shoes at the native hip.
        var boot=left;
        var sole=boot.cubes().get(1);assertEquals(new WhiteKicksGeometry.Vec(5f,1.125f,7.5f),sole.size());
        var tongue=boot.cubes().get(3);assertEquals(22.5f,tongue.rotation().x());
        assertEquals((float)Math.toRadians(22.5),WhiteKicksGeometry.rotation(tongue.rotation()).x(),.00001);
        assertEquals(new WhiteKicksGeometry.Vec(2,12,0),WhiteKicksGeometry.position(new WhiteKicksGeometry.Vec(2,12,0)));
    }
    @Test void textureAndBothThumbnailsArePackagedAtTheirDeclaredSizes() throws Exception {
        for(String path:List.of("textures/entity/accessories/white_kicks.png","textures/gui/white_kicks_preview.png")){
            try(var input=getClass().getResourceAsStream("/assets/axial_cosmetics/"+path)){
                assertNotNull(input);var image=ImageIO.read(input);int size=path.contains("preview")?WardrobeAccessory.WHITE_KICKS.previewSize:32;
                assertEquals(size,image.getWidth());assertEquals(size,image.getHeight());
                int opaque=0;for(int y=0;y<size;y++)for(int x=0;x<size;x++)if((image.getRGB(x,y)>>>24)>100)opaque++;
                assertTrue(opaque>100,"Visible textured content");
            }
        }
        assertNotEquals(WardrobeAccessory.SWIM_BANDS.preview(),WardrobeAccessory.WHITE_KICKS.preview());
    }
    @Test void wardrobeAndAccountSelectionsRequireTheExplicitServerGrant() throws Exception {
        UUID uuid=UUID.fromString("12345678-1234-4234-8234-123456789012");
        var snapshot=CosmeticAssignments.parse("{\"version\":1,\"players\":{\""+uuid+"\":[\"white_kicks\",\"swim_bands\"]}}");
        assertEquals(Set.of("white_kicks","swim_bands"),snapshot.get(uuid));
        var field=CosmeticAssignments.class.getDeclaredField("grants");field.setAccessible(true);var previous=field.get(null);
        try{
            field.set(null,snapshot);assertTrue(WardrobeAccess.allows(uuid,uuid));
            assertEquals(2,WardrobeItem.owned(uuid,WardrobeCategory.OUTFITS).size());
            var values=new Properties();assertNull(WardrobePreferences.accessorySelection(values,uuid));
            values.setProperty(uuid+".accessory","white_kicks");assertSame(WardrobeAccessory.WHITE_KICKS,WardrobePreferences.accessorySelection(values,uuid));
            field.set(null,Map.of());assertNull(WardrobePreferences.accessorySelection(values,uuid));
        }finally{field.set(null,previous);}
    }
    @Test void shoesUseTheAnimatedLegTransformsAndExistingCollisionGuard() throws Exception {
        try(var input=getClass().getResourceAsStream("/com/axial/cosmetics/client/WhiteKicksFeatureRenderer.class")){
            var node=new ClassNode();new ClassReader(input).accept(node,0);var legs=new HashSet<String>();boolean guarded=false;
            for(var method:node.methods)for(var instruction:method.instructions){
                if(instruction instanceof FieldInsnNode field&&(field.name.equals("leftLeg")||field.name.equals("rightLeg")))legs.add(field.name);
                if(instruction instanceof MethodInsnNode call&&call.owner.endsWith("ItemSkinConflict")&&call.name.equals("hidesCosmetics"))guarded=true;
            }
            assertEquals(Set.of("leftLeg","rightLeg"),legs);assertTrue(guarded);
        }
    }
}
