package com.axial.cosmetics.client;

import java.util.*;
import javax.imageio.ImageIO;
import net.minecraft.client.util.math.MatrixStack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PooklesTest {
    @Test void importsAllPlushCubesAndItsNinetySixByEightTexture()throws Exception {
        var model=ShoulderPetFeatureRenderer.model("pookles");var count=new int[1];var maxU=new float[1];
        model.forEachCuboid(new MatrixStack(),(matrix,path,index,cube)->{
            count[0]++;assertInstanceOf(FaceMappedCosmeticCuboid.class,cube);
            for(var face:cube.sides)for(var v:face.vertices()) {
                assertTrue(v.u()>=0&&v.u()<=1);assertTrue(v.v()>=0&&v.v()<=1);maxU[0]=Math.max(maxU[0],v.u());
            }
        });assertEquals(39,count[0]);assertEquals(88f/96,maxU[0],.00001f);
        try(var in=getClass().getResourceAsStream("/assets/axial_cosmetics/textures/entity/accessories/pookles.png")) {
            assertNotNull(in);var image=ImageIO.read(in);assertEquals(96,image.getWidth());assertEquals(8,image.getHeight());
        }
        var front=model.getChild("torso").getChild("body").getChild("plush").getChild("cube0");
        front.forEachCuboid(new MatrixStack(),(matrix,path,index,cube)->{
            var face=Arrays.stream(cube.sides).filter(f->f.direction().z()<-.9f).findFirst().orElseThrow();
            assertEquals(8f/96,Arrays.stream(face.vertices()).map(v->v.u()).max(Float::compare).orElseThrow(),.00001f);
        });
    }
    @Test void allIdleTracksLoopAndAnimateWithoutAccumulation() {
        var animation=new ShoulderPetAnimation("pookles");
        assertEquals(Set.of("plush","head","ear_right","ear_left","eye_right","eye_left","mouth","hand_left","hand_right"),animation.boneNames());
        var root=ShoulderPetFeatureRenderer.model("pookles");var bones=ShoulderPetFeatureRenderer.bones(root,"pookles");
        var minimumEye=1f;var maximumMouth=1f;
        ShoulderPetFeatureRenderer.animate(bones,animation,0);var baseline=new HashMap<String,float[]>();
        for(var name:animation.boneNames()){var p=bones.get(name);baseline.put(name,new float[]{p.originX,p.originY,p.originZ,p.pitch,p.yaw,p.roll,p.xScale,p.yScale,p.zScale});}
        for(int i=0;i<=80;i++) {
            float time=i/20f;ShoulderPetFeatureRenderer.animate(bones,animation,time);
            minimumEye=Math.min(minimumEye,animation.sample("eye_left",time).scale().y());
            maximumMouth=Math.max(maximumMouth,animation.sample("mouth",time).scale().y());
            root.collectVertices(new MatrixStack(),p->{assertTrue(Float.isFinite(p.x())&&Float.isFinite(p.y())&&Float.isFinite(p.z()));assertTrue(p.x()>.3f);});
        }
        assertTrue(minimumEye<.1f);assertTrue(maximumMouth>1.2f);
        for(var name:animation.boneNames()){var p=bones.get(name);assertArrayEquals(baseline.get(name),new float[]{p.originX,p.originY,p.originZ,p.pitch,p.yaw,p.roll,p.xScale,p.yScale,p.zScale},.00001f);}
    }
    @Test void privateGrantEquipsPooklesReplacingOnlyAnotherPet()throws Exception {
        var uuid=UUID.randomUUID();var field=CosmeticAssignments.class.getDeclaredField("grants");field.setAccessible(true);var previous=field.get(null);
        try {
            field.set(null,Map.of(uuid,Set.of("pink_slime","ribcage","black_sneakers")));assertFalse(WardrobeAccessory.POOKLES.ownedBy(uuid));
            field.set(null,Map.of(uuid,Set.of("pookles","pink_slime","ribcage","black_sneakers")));
            var props=new Properties();props.setProperty(uuid+".accessories","ribcage,black_sneakers,pink_slime");
            var updated=WardrobePreferences.accessoryToggle(props,uuid,WardrobeAccessory.POOKLES);
            assertEquals(List.of(WardrobeAccessory.RIBCAGE,WardrobeAccessory.BLACK_SNEAKERS,WardrobeAccessory.POOKLES),WardrobePreferences.accessorySelections(updated,uuid));
            assertTrue(WardrobeItem.owned(uuid,WardrobeCategory.PETS).contains(WardrobeItem.of(WardrobeAccessory.POOKLES)));
            field.set(null,Map.of(uuid,Set.of("ribcage","black_sneakers")));
            assertEquals(List.of(WardrobeAccessory.RIBCAGE,WardrobeAccessory.BLACK_SNEAKERS),WardrobePreferences.accessorySelections(updated,uuid));
        }finally{field.set(null,previous);}
    }
}
