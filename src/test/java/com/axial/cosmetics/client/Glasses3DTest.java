package com.axial.cosmetics.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class Glasses3DTest {
 @Test void preservesGlassesGeometryOnTheHead()throws Exception{
  var bones=BedrockCosmeticGeometry.load("3d_glasses");
  assertEquals(64,bones.stream().mapToInt(b->b.cubes().size()).sum());
  assertEquals(java.util.Set.of(com.axial.cosmetics.data.CosmeticSlot.HEAD),WardrobeAccessory.GLASSES_3D.slots);
  var head=CowboyHatFeatureRenderer.model("3d_glasses").getChild("head");
  int[] count={0};head.forEachCuboid(new net.minecraft.client.util.math.MatrixStack(),(m,p,i,c)->count[0]++);
  assertEquals(64,count[0]);
  var frame=WardrobeModelView.fit(WardrobeItem.of(WardrobeAccessory.GLASSES_3D),new ModMenuLayout.Rect(0,0,140,130),true);
  assertEquals(1.9f,frame.centerY());
 }
}
