package com.axial.cosmetics.client;

import com.google.gson.JsonParser;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class SwimBandsTest {
    @Test void suppliedAssetHasTwoArmBandsAndMatchingTexture() throws Exception {
        try(var input=getClass().getResourceAsStream("/assets/axial_cosmetics/geo/swim_bands.geo.json")){
            var geometry=JsonParser.parseString(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            var description=geometry.getAsJsonObject("description");assertEquals(32,description.get("texture_width").getAsInt());
            int cubes=0;
            for(var bone:geometry.getAsJsonArray("bones"))if(bone.getAsJsonObject().has("cubes"))for(var cube:bone.getAsJsonObject().getAsJsonArray("cubes")){
                var size=cube.getAsJsonObject().getAsJsonArray("size");assertEquals(6,size.get(0).getAsInt());assertEquals(3,size.get(1).getAsInt());assertEquals(6,size.get(2).getAsInt());cubes++;
            }
            assertEquals(2,cubes);
        }
        try(var input=getClass().getResourceAsStream("/assets/axial_cosmetics/textures/entity/accessories/swim_bands.png")){
            var image=ImageIO.read(input);assertEquals(32,image.getWidth());assertEquals(32,image.getHeight());
        }
    }
    @Test void texturedCosmeticsUseWhiteTintRatherThanOverlayCoordinates() throws Exception {
        for(String renderer:List.of("SwimBandsFeatureRenderer","WhiteKicksFeatureRenderer","CosmeticFeatureRenderer")){
            try(var input=getClass().getResourceAsStream("/com/axial/cosmetics/client/"+renderer+".class")){
                var node=new ClassNode();new ClassReader(input).accept(node,0);int calls=0;
                for(var method:node.methods)for(var instruction:method.instructions){
                    if(instruction instanceof MethodInsnNode call&&call.owner.equals("net/minecraft/client/render/entity/feature/FeatureRenderer")&&call.name.equals("render")){
                        assertEquals(Opcodes.ICONST_0,call.getPrevious().getOpcode(),"Default render queue");
                        assertEquals(Opcodes.ICONST_M1,call.getPrevious().getPrevious().getOpcode(),renderer+" must preserve texture colours with opaque white ARGB");
                        calls++;
                    }
                }
                assertTrue(calls>0,renderer);
            }
        }
    }
    @Test void bandsAreAssignableAndRequireAnExplicitOwnedSelection() throws Exception {
        UUID uuid=UUID.fromString("12345678-1234-4234-8234-123456789012");
        var snapshot=CosmeticAssignments.parse("{\"version\":1,\"players\":{\""+uuid+"\":[\"swim_bands\"]}}");
        assertEquals(Set.of("swim_bands"),snapshot.get(uuid));
        var field=CosmeticAssignments.class.getDeclaredField("grants");field.setAccessible(true);var previous=field.get(null);
        try{field.set(null,snapshot);assertTrue(WardrobeAccess.allows(uuid,uuid));
            assertEquals(List.of(WardrobeItem.of(WardrobeAccessory.SWIM_BANDS)),WardrobeItem.owned(uuid,WardrobeCategory.OUTFITS));
            var values=new Properties();assertNull(WardrobePreferences.accessorySelection(values,uuid));
            values.setProperty(uuid+".accessory","swim_bands");assertEquals(WardrobeAccessory.SWIM_BANDS,WardrobePreferences.accessorySelection(values,uuid));
            field.set(null,Map.of());assertNull(WardrobePreferences.accessorySelection(values,uuid));
        }finally{field.set(null,previous);}
    }
    @Test void collisionGuardChecksAllWornSlotsAgainstTheActualGeoRegistry() throws Exception {
        try(var input=getClass().getResourceAsStream("/com/axial/cosmetics/client/ItemSkinConflict.class")){
            var node=new ClassNode();new ClassReader(input).accept(node,0);Set<String> slots=new HashSet<>();boolean registry=false;
            for(var method:node.methods)for(var insn:method.instructions){
                if(insn instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.GETSTATIC&&f.owner.endsWith("EquipmentSlot"))slots.add(f.name);
                if(insn instanceof MethodInsnNode call&&call.owner.endsWith("ArmorCosmeticRegistry")&&call.name.equals("find"))registry=true;
            }
            assertEquals(Set.of("HEAD","CHEST","LEGS","FEET"),slots);assertTrue(registry);
        }
    }
}
