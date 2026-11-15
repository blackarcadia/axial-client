package com.axial.cosmetics.client;

import java.lang.reflect.Proxy;
import java.util.*;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CosmeticFaceRenderingTest {
    @Test void actualGhostRenderingUsesAuthoredFrontTextureInsteadOfBoxUvCache() throws Exception {
        var root=ShoulderPetFeatureRenderer.model("ghostly_ghost");
        var face=root.getChild("torso").getChild("body").getChild("ghost").getChild("cube0");
        var front=new ArrayList<float[]>();
        var consumer=(VertexConsumer)Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(),new Class<?>[]{VertexConsumer.class},(proxy,method,args)->{
            if(method.getName().equals("vertex")&&args.length==11&&((Float)args[10])<-.99f)
                front.add(new float[]{(Float)args[4],(Float)args[5]});
            return method.getReturnType()==void.class?null:proxy;
        });
        face.render(new MatrixStack(),consumer,0xF000F0,0,0xFFFFFFFF);
        assertEquals(4,front.size());
        assertEquals(Set.of(0f,8f/64),new HashSet<>(front.stream().map(v->v[0]).toList()));
        assertEquals(Set.of(0f,5.8824f/8),new HashSet<>(front.stream().map(v->v[1]).toList()));
        // This override runs independently of Sodium's injected base render method.
        assertEquals(FaceMappedCosmeticCuboid.class,FaceMappedCosmeticCuboid.class.getDeclaredMethod("renderCuboid",
                MatrixStack.Entry.class,VertexConsumer.class,int.class,int.class,int.class).getDeclaringClass());
    }
    @Test void onlyImportedPerFaceModelsUseTheCompatibleRenderer() {
        for(String id:List.of("ghostly_ghost","pink_slime","pookles","black_sneakers")) {
            var root=id.equals("black_sneakers")?WhiteKicksFeatureRenderer.model(id):ShoulderPetFeatureRenderer.model(id);
            var mapped=new HashSet<String>();
            for(var bone:BedrockCosmeticGeometry.load(id))for(int i=0;i<bone.cubes().size();i++)
                if(!bone.cubes().get(i).faces().isEmpty())mapped.add("/"+bone.name()+"/cube"+i);
            var faces=new int[1];root.forEachCuboid(new MatrixStack(),(matrix,path,index,cube)->{
                if(mapped.stream().anyMatch(path::endsWith)){assertInstanceOf(FaceMappedCosmeticCuboid.class,cube,id+path);faces[0]++;}
                else assertEquals(ModelPart.Cuboid.class,cube.getClass());
            });assertEquals(mapped.size(),faces[0]);
        }
        RibcageFeatureRenderer.model().forEachCuboid(new MatrixStack(),(matrix,path,index,cube)->assertEquals(ModelPart.Cuboid.class,cube.getClass()));
    }
}
