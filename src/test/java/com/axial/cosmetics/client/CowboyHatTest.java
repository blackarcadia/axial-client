package com.axial.cosmetics.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CowboyHatTest {
 @Test void preservesAuthoredCubesAndHeadAttachment(){
  var bones=BedrockCosmeticGeometry.load("cowboy_hat");
  assertEquals(20,bones.stream().mapToInt(b->b.cubes().size()).sum());
  assertEquals(java.util.Set.of(com.axial.cosmetics.data.CosmeticSlot.HEAD),WardrobeAccessory.COWBOY_HAT.slots);
  var head=CowboyHatFeatureRenderer.model().getChild("head");
  int[] count={0};head.forEachCuboid(new net.minecraft.client.util.math.MatrixStack(),(m,p,i,c)->count[0]++);
  assertEquals(20,count[0]);
 }
}
