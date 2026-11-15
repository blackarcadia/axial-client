package com.axial.cosmetics.client;

import java.util.*;
import net.minecraft.client.util.math.MatrixStack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GhostlyGhostTest {
    @Test void modelPreservesAllCubesAndSixtyFourByEightUvMapping() {
        var model=ShoulderPetFeatureRenderer.model("ghostly_ghost");var count=new int[1];var maxU=new float[1];var maxV=new float[1];
        model.forEachCuboid(new MatrixStack(),(entry,path,index,cube)->{
            count[0]++;
            for(var face:cube.sides)for(var v:face.vertices()) {
                assertTrue(v.u()>=0&&v.u()<=1);assertTrue(v.v()>=0&&v.v()<=1);
                maxU[0]=Math.max(maxU[0],v.u());maxV[0]=Math.max(maxV[0],v.v());
            }
        });assertEquals(29,count[0]);assertEquals(1,maxU[0]);assertEquals(1,maxV[0]);
    }
    @Test void embeddedTracksAnimateGhostEyesChainAndWeightAndLoopCleanly() {
        var animation=new ShoulderPetAnimation("ghostly_ghost");
        assertEquals(Set.of("ghost","eye_left","eye_right","chain","iron_ball"),animation.boneNames());
        assertEquals(1.65f,animation.sample("ghost",1).position().y(),.0001f);
        assertEquals(.935f,animation.sample("ghost",1).scale().x(),.0001f);
        assertEquals(.035f,animation.sample("eye_left",1.65f).scale().y(),.0001f);
        assertEquals(1,animation.sample("eye_left",4f).scale().y(),.0001f);
        assertEquals(animation.sample("chain",0),animation.sample("chain",4));
        var model=ShoulderPetFeatureRenderer.model("ghostly_ghost");var bones=ShoulderPetFeatureRenderer.bones(model,"ghostly_ghost");
        for(int i=0;i<80;i++) {
            ShoulderPetFeatureRenderer.animate(bones,animation,i/20f);
            model.collectVertices(new MatrixStack(),p->{assertTrue(Float.isFinite(p.x())&&Float.isFinite(p.y())&&Float.isFinite(p.z()));assertTrue(p.x()>.35f,"pet stays clear of head and torso");});
        }
        ShoulderPetFeatureRenderer.animate(bones,animation,0);var roll=bones.get("chain").roll;
        ShoulderPetFeatureRenderer.animate(bones,animation,4);assertEquals(roll,bones.get("chain").roll,.00001f);
    }
    @Test void ghostReplacesOtherPetAndPreservesOutfits()throws Exception {
        var uuid=UUID.randomUUID();var field=CosmeticAssignments.class.getDeclaredField("grants");field.setAccessible(true);var previous=field.get(null);
        try {
            field.set(null,Map.of(uuid,Set.of("pink_slime","ghostly_ghost","ribcage","black_sneakers")));
            var props=new Properties();props.setProperty(uuid+".accessories","ribcage,black_sneakers,pink_slime");
            var changed=WardrobePreferences.accessoryToggle(props,uuid,WardrobeAccessory.GHOSTLY_GHOST);
            assertEquals(List.of(WardrobeAccessory.RIBCAGE,WardrobeAccessory.BLACK_SNEAKERS,WardrobeAccessory.GHOSTLY_GHOST),WardrobePreferences.accessorySelections(changed,uuid));
            assertEquals(2,WardrobeItem.owned(uuid,WardrobeCategory.PETS).size());
        }finally{field.set(null,previous);}
    }
}
