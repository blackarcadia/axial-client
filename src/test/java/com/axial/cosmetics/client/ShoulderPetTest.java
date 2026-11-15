package com.axial.cosmetics.client;

import com.axial.cosmetics.data.CosmeticSlot;
import java.util.*;
import net.minecraft.client.util.math.MatrixStack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShoulderPetTest {
    @Test void importedModelFloatsClearOfShoulderAndUsesItsNonSquareTexture() {
        var points=new ArrayList<org.joml.Vector3f>();
        var model=ShoulderPetFeatureRenderer.model();model.collectVertices(new MatrixStack(),v->points.add(new org.joml.Vector3f(v)));
        assertFalse(points.isEmpty());
        assertTrue(points.stream().allMatch(v->v.y()<-.19f&&v.x()>.33f));
        var count=new int[1];model.forEachCuboid(new MatrixStack(),(entry,path,index,cube)->{
            count[0]++;
            for(var face:cube.sides)for(var vertex:face.vertices()) {
                assertTrue(vertex.u()>=0&&vertex.u()<=1);assertTrue(vertex.v()>=0&&vertex.v()<=1);
            }
        });assertEquals(3,count[0]);
        var body=model.getChild("torso").getChild("body").getChild("slime").getChild("cube1");
        var vs=new ArrayList<Float>();body.forEachCuboid(new MatrixStack(),(entry,path,index,cube)->{
            for(var side:cube.sides)for(var vertex:side.vertices())vs.add(vertex.v());
        });assertEquals(1f,Collections.max(vs));
    }
    @Test void suppliedAnimationInterpolatesAndLoopsWithoutJumping() {
        var animation=new ShoulderPetAnimation();
        assertEquals(.82f,animation.sample(.8f).position().y(),.0001f);
        assertEquals(.08f,animation.sample(.05f).position().y(),.0001f);
        assertEquals(animation.sample(0).scale().y(),animation.sample(3.2f).scale().y(),.0001f);
        assertEquals(animation.sample(.8f).rotation().z(),animation.sample(4f).rotation().z(),.0001f);
        assertTrue(animation.sample(.5f).scale().y()>1);
    }
    @Test void petGrantStacksWithOutfitAndAppearsInPetsRatherThanOutfits()throws Exception {
        var uuid=UUID.randomUUID();var field=CosmeticAssignments.class.getDeclaredField("grants");field.setAccessible(true);var previous=field.get(null);
        try {
            field.set(null,Map.of(uuid,Set.of("pink_slime","ribcage","black_sneakers")));
            var props=new Properties();props.setProperty(uuid+".accessories","ribcage,black_sneakers");
            var changed=WardrobePreferences.accessoryToggle(props,uuid,WardrobeAccessory.PINK_SLIME);
            assertEquals(3,WardrobePreferences.accessorySelections(changed,uuid).size());
            assertEquals(Set.of(CosmeticSlot.PET),WardrobeAccessory.PINK_SLIME.slots);
            assertEquals(List.of(WardrobeItem.of(WardrobeAccessory.PINK_SLIME)),WardrobeItem.owned(uuid,WardrobeCategory.PETS));
            assertEquals(2,WardrobeItem.owned(uuid,WardrobeCategory.OUTFITS).size());
            field.set(null,Map.of(uuid,Set.of("ribcage","black_sneakers")));
            assertEquals(2,WardrobePreferences.accessorySelections(changed,uuid).size());
        }finally{field.set(null,previous);}
    }
}
