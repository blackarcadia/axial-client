package com.axial.cosmetics.client;

import java.util.*;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BedrockCosmeticModelTest {
    @Test void bakedShoesMatchBlockbenchCoordinatesForEveryCubeCorner() {
        for(String id:List.of("white_sneakers","white_kicks","ribcage")) {
            var actual=new ArrayList<Vector3f>();
            (id.equals("ribcage")?RibcageFeatureRenderer.model():WhiteKicksFeatureRenderer.model(id)).collectVertices(new MatrixStack(),p->actual.add(new Vector3f(p)));
            var expected=blockbenchCorners(BedrockCosmeticGeometry.load(id));
            for(var point:expected)assertTrue(actual.stream().anyMatch(p->p.distance(point)<.00001f),id+" missing corner "+point);
            for(var point:actual)assertTrue(expected.stream().anyMatch(p->p.distance(point)<.00001f),id+" unexpected corner "+point);
        }
    }

    @Test void fractionalSoleKeepsItsSizeButUsesWholeTexelBoxUvSpans() {
        var found=new ArrayList<ModelPart.Cuboid>();
        WhiteKicksFeatureRenderer.model("white_sneakers").forEachCuboid(new MatrixStack(),(entry,path,index,cube)->{
            if(path.endsWith("/armorLeftBoot/cube2"))found.add(cube);
        });
        assertEquals(1,found.size());
        var us=new TreeSet<Float>();var vs=new TreeSet<Float>();
        for(var side:found.getFirst().sides)for(var vertex:side.vertices()) {
            us.add(vertex.u()*32);vs.add(vertex.v()*32);
        }
        assertEquals(new TreeSet<>(List.of(0f,7f,12f,17f,19f,24f)),us);
        assertEquals(new TreeSet<>(List.of(0f,7f,8f)),vs);
    }

    /** Independent editor-space reference: Bedrock loader mirrors X and negates
     * X/Y Euler angles; a half-turn around Z converts editor space to Java. */
    private static List<Vector3f> blockbenchCorners(List<BedrockCosmeticGeometry.Bone> bones) {
        var matrices=new HashMap<String,Matrix4f>();
        var pivots=new HashMap<String,Vector3f>();
        var result=new ArrayList<Vector3f>();
        for(var bone:bones) {
            var pivot=editor(bone.pivot());
            var parent=bone.parent();
            var matrix=parent==null?new Matrix4f():new Matrix4f(matrices.get(parent));
            matrix.translate(new Vector3f(pivot).sub(parent==null?new Vector3f():pivots.get(parent)));
            rotateEditor(matrix,bone.rotation());
            matrices.put(bone.name(),matrix);pivots.put(bone.name(),pivot);
            for(var cube:bone.cubes()) {
                var cp=editor(cube.pivot());
                var transform=new Matrix4f(matrix).translate(new Vector3f(cp).sub(pivot));
                rotateEditor(transform,cube.rotation());
                for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++) {
                    float inflate=cube.inflate();
                    var point=new Vector3f(-cube.origin().x()+inflate-x*(cube.size().x()+2*inflate),
                            cube.origin().y()-inflate+y*(cube.size().y()+2*inflate),
                            cube.origin().z()-inflate+z*(cube.size().z()+2*inflate)).sub(cp);
                    transform.transformPosition(point);
                    result.add(new Vector3f(-point.x,24-point.y,point.z).div(16));
                }
            }
        }
        return result;
    }
    private static Vector3f editor(BedrockCosmeticGeometry.Vec p){return new Vector3f(-p.x(),p.y(),p.z());}
    private static void rotateEditor(Matrix4f matrix,BedrockCosmeticGeometry.Vec r) {
        matrix.rotateZ((float)Math.toRadians(r.z())).rotateY((float)Math.toRadians(-r.y())).rotateX((float)Math.toRadians(-r.x()));
    }
}
