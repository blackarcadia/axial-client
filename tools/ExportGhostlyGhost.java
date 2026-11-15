package com.axial.cosmetics.client;
import java.nio.file.*;import java.util.*;import net.minecraft.client.model.*;import net.minecraft.client.util.math.MatrixStack;import org.joml.Vector3f;
public class ExportGhostlyGhost {
    private static List<Float> vertices(ModelPart model) {
        var result=new ArrayList<Float>();
        model.forEachCuboid(new MatrixStack(),(entry,path,index,cube)->{
            for(var face:cube.sides){
                var normal=entry.getNormalMatrix().transform(new Vector3f(face.direction())).normalize();
                for(int vertex:new int[]{0,1,2,0,2,3}){
                    var v=face.vertices()[vertex];
                    var p=entry.getPositionMatrix().transformPosition(v.worldX(),v.worldY(),v.worldZ(),new Vector3f());
                    Collections.addAll(result,p.x,1.5f-p.y,p.z,v.u(),v.v(),normal.x,-normal.y,normal.z,1f,1f,1f,1f);
                }
            }
        });
        return result;
    }
public static void main(String[]args)throws Exception {
    var root=ShoulderPetFeatureRenderer.model("ghostly_ghost");
    var bones=ShoulderPetFeatureRenderer.bones(root,"ghostly_ghost");
    var animation=new ShoulderPetAnimation("ghostly_ghost");
    var frames=new ArrayList<List<Float>>();
    for(int i=0;i<32;i++){ShoulderPetFeatureRenderer.animate(bones,animation,i/8f);frames.add(vertices(root));}
    var data=new com.google.gson.Gson().toJson(Map.of("frames",frames,"duration",4000,"vertices",frames.getFirst()));
    Files.writeString(Path.of(args[0]),data);
    try(var gzip=new java.util.zip.GZIPOutputStream(Files.newOutputStream(Path.of(args[0]+".gz")))){gzip.write(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
}}
