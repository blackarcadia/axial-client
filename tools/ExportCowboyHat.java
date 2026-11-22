package com.axial.cosmetics.client;
import java.nio.file.*;import java.util.*;import net.minecraft.client.util.math.MatrixStack;import org.joml.Vector3f;
public class ExportCowboyHat {
 public static void main(String[] args)throws Exception {
  var vertices=new ArrayList<Float>();
  CowboyHatFeatureRenderer.model().forEachCuboid(new MatrixStack(),(entry,path,index,cube)->{
   for(var face:cube.sides){var n=entry.getNormalMatrix().transform(new Vector3f(face.direction())).normalize();
    for(int i:new int[]{0,1,2,0,2,3}){var v=face.vertices()[i];var p=entry.getPositionMatrix().transformPosition(v.worldX(),v.worldY(),v.worldZ(),new Vector3f());
     Collections.addAll(vertices,p.x,1.5f-p.y,p.z,v.u(),v.v(),n.x,-n.y,n.z,1f,1f,1f,1f);
    }
   }
  });
  Files.writeString(Path.of(args[0]),new com.google.gson.Gson().toJson(Map.of("vertices",vertices)));
 }
}
