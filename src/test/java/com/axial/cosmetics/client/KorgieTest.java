package com.axial.cosmetics.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class KorgieTest {
 @Test void preservesBothSlippersAndTheirFaceUvs()throws Exception{
  var bones=BedrockCosmeticGeometry.load("korgie");
  assertEquals(16,bones.stream().mapToInt(b->b.cubes().size()).sum());
  assertTrue(bones.stream().flatMap(b->b.cubes().stream()).allMatch(c->c.faces().size()==6));
  assertEquals(java.util.Set.of(com.axial.cosmetics.data.CosmeticSlot.BOOTS),WardrobeAccessory.KORGIE.slots);
  var root=WhiteKicksFeatureRenderer.model("korgie");
  for(String side:java.util.List.of("left","right")){
   int[] count={0};root.getChild(side).forEachCuboid(new net.minecraft.client.util.math.MatrixStack(),(m,p,i,c)->count[0]++);
   assertEquals(8,count[0]);
  }
  try(var in=getClass().getResourceAsStream("/assets/axial_cosmetics/textures/entity/accessories/korgie.png")){
   var image=javax.imageio.ImageIO.read(in);assertEquals(32,image.getWidth());assertEquals(32,image.getHeight());
  }
 }
}
