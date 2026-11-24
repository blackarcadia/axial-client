package com.axial.cosmetics.client;
import java.util.*;
import javax.imageio.ImageIO;
import net.minecraft.client.util.math.MatrixStack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class UfoTest {
 @Test void outerHullUsesGreyExteriorRatherThanGreenInterior() {
  var hull=ShoulderPetFeatureRenderer.model("ufo").getChild("torso").getChild("spinnybottom2").getChild("base").getChild("cube2");
  hull.forEachCuboid(new MatrixStack(),(m,p,i,c)->{
   var outside=Arrays.stream(c.sides).filter(f->f.direction().x()<-.9f).findFirst().orElseThrow();
   assertEquals(Set.of(6f/32,14f/32),new HashSet<>(Arrays.stream(outside.vertices()).map(v->v.u()).toList()));
   assertEquals(Set.of(2f/32,4f/32),new HashSet<>(Arrays.stream(outside.vertices()).map(v->v.v()).toList()));
   var inside=Arrays.stream(c.sides).filter(f->f.direction().x()>.9f).findFirst().orElseThrow();
   assertEquals(Set.of(0f,2f/32),new HashSet<>(Arrays.stream(inside.vertices()).map(v->v.v()).toList()));
  });
 }
 @Test void preservesAuthoredCubesTextureAndAnimatedHierarchy()throws Exception {
  var root=ShoulderPetFeatureRenderer.model("ufo");var count=new int[1];
  root.forEachCuboid(new MatrixStack(),(m,p,i,c)->{count[0]++;assertInstanceOf(FaceMappedCosmeticCuboid.class,c);});
  assertEquals(53,count[0]);
  try(var in=getClass().getResourceAsStream("/assets/axial_cosmetics/textures/entity/accessories/ufo.png")) {
   var image=ImageIO.read(in);assertEquals(32,image.getWidth());assertEquals(32,image.getHeight());
  }
  var bones=ShoulderPetFeatureRenderer.bones(root,"ufo");var animation=new ShoulderPetAnimation("ufo");
  assertEquals(20,animation.boneNames().size());
  for(int i=0;i<98;i++) {
   float time=i/24f;ShoulderPetFeatureRenderer.animate(bones,animation,time);
   root.collectVertices(new MatrixStack(),p->{assertTrue(Float.isFinite(p.x())&&Float.isFinite(p.y())&&Float.isFinite(p.z()));});
  }
  var pose=animation.sample("spinnybottom2",0);
  assertEquals(pose,animation.sample("spinnybottom2",4.04167f));
  ShoulderPetFeatureRenderer.animate(bones,animation,0);float y=bones.get("spinnybottom2").originY;
  ShoulderPetFeatureRenderer.animate(bones,animation,0);assertEquals(y,bones.get("spinnybottom2").originY);
 }
    @Test void privateGrantEquipsUfoReplacingOnlyAnotherPet()throws Exception {
        var uuid=UUID.randomUUID();var field=CosmeticAssignments.class.getDeclaredField("grants");field.setAccessible(true);var previous=field.get(null);
        try {
            field.set(null,Map.of(uuid,Set.of("pink_slime","ribcage","black_sneakers")));assertFalse(WardrobeAccessory.UFO.ownedBy(uuid));
            field.set(null,Map.of(uuid,Set.of("ufo","pink_slime","ribcage","black_sneakers")));
            var props=new Properties();props.setProperty(uuid+".accessories","ribcage,black_sneakers,pink_slime");
            var updated=WardrobePreferences.accessoryToggle(props,uuid,WardrobeAccessory.UFO);
            assertEquals(List.of(WardrobeAccessory.RIBCAGE,WardrobeAccessory.BLACK_SNEAKERS,WardrobeAccessory.UFO),WardrobePreferences.accessorySelections(updated,uuid));
            assertTrue(WardrobeItem.owned(uuid,WardrobeCategory.PETS).contains(WardrobeItem.of(WardrobeAccessory.UFO)));
            field.set(null,Map.of(uuid,Set.of("ribcage","black_sneakers")));
            assertEquals(List.of(WardrobeAccessory.RIBCAGE,WardrobeAccessory.BLACK_SNEAKERS),WardrobePreferences.accessorySelections(updated,uuid));
        }finally{field.set(null,previous);}
    }
}
