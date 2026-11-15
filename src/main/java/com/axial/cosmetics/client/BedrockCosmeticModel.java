package com.axial.cosmetics.client;

import java.util.*;
import net.minecraft.client.model.*;

/** Bake imported bones beneath native player-part anchors, preserving box UVs. */
final class BedrockCosmeticModel {
    record Anchor(String name,BedrockCosmeticGeometry.Vec pivot) {}
    private BedrockCosmeticModel() {}

    static ModelPart bake(String id,Map<String,Anchor> anchors) {
        return bake(id,anchors,32,32);
    }
    static ModelPart bake(String id,Map<String,Anchor> anchors,int textureWidth,int textureHeight) {
        var data=new ModelData();
        var parts=new HashMap<String,ModelPartData>();
        var paths=new HashMap<String,List<String>>();
        var cubePaths=new ArrayList<List<String>>();
        var faceMappings=new HashMap<List<String>,Map<String,BedrockCosmeticGeometry.FaceUv>>();
        var pivots=new HashMap<String,BedrockCosmeticGeometry.Vec>();
        for(var anchor:anchors.values()) {
            var p=anchor.pivot();
            parts.put(anchor.name(),data.getRoot().addChild(anchor.name(),ModelPartBuilder.create(),ModelTransform.origin(p.x(),p.y(),p.z())));
            pivots.put(anchor.name(),p);paths.put(anchor.name(),List.of(anchor.name()));
        }
        for(var bone:BedrockCosmeticGeometry.load(id)) {
            String parent=bone.parent()==null?anchors.get(bone.name()).name():bone.parent();
            var pivot=BedrockCosmeticGeometry.position(bone.pivot());
            var offset=pivot.subtract(pivots.get(parent));
            var angles=BedrockCosmeticGeometry.rotation(bone.rotation());
            var part=parts.get(parent).addChild(bone.name(),ModelPartBuilder.create(),
                    ModelTransform.of(offset.x(),offset.y(),offset.z(),angles.x(),angles.y(),angles.z()));
            parts.put(bone.name(),part);pivots.put(bone.name(),pivot);
            var path=new ArrayList<>(paths.get(parent));path.add(bone.name());paths.put(bone.name(),List.copyOf(path));
            int index=0;
            for(var cube:bone.cubes()) {
                var local=cube.meshOrigin();var size=cube.uvSize();var scale=cube.meshScale();
                var origin=BedrockCosmeticGeometry.position(cube.pivot()).subtract(pivot);
                var rotation=BedrockCosmeticGeometry.rotation(cube.rotation());
                var dilation=new Dilation(cube.inflate()/scale.x(),cube.inflate()/scale.y(),cube.inflate()/scale.z());
                String cubeName="cube"+index++;
                var cubePath=new ArrayList<>(path);cubePath.add(cubeName);cubePaths.add(List.copyOf(cubePath));
                if(!cube.faces().isEmpty())faceMappings.put(List.copyOf(cubePath),cube.faces());
                part.addChild(cubeName,ModelPartBuilder.create().uv(cube.u(),cube.v()).mirrored(cube.mirror())
                                .cuboid(local.x(),local.y(),local.z(),size.x(),size.y(),size.z(),dilation),
                        new ModelTransform(origin.x(),origin.y(),origin.z(),rotation.x(),rotation.y(),rotation.z(),scale.x(),scale.y(),scale.z()));
            }
        }
        var root=TexturedModelData.of(data,textureWidth,textureHeight).createModel();
        for(var entry:faceMappings.entrySet()){
            var part=root;for(var name:entry.getKey())part=part.getChild(name);
            part.forEachCuboid(new net.minecraft.client.util.math.MatrixStack(),(matrix,path,index,cube)->{
                for(int i=0;i<cube.sides.length;i++){
                    var quad=cube.sides[i];var n=quad.direction();
                    // Bedrock's east/west names are reflected in the Java player model;
                    // Y-down means the native DOWN normal is the authored top face.
                    String face=n.x()<-.5f?"east":n.x()>.5f?"west":n.y()<-.5f?"up":n.y()>.5f?"down":n.z()<0?"north":"south";
                    var uv=entry.getValue().get(face);if(uv==null)continue;
                    float u0=uv.u()/textureWidth,v0=uv.v()/textureHeight,u1=(uv.u()+uv.width())/textureWidth,v1=(uv.v()+uv.height())/textureHeight;
                    var v=quad.vertices();
                    cube.sides[i]=new ModelPart.Quad(new ModelPart.Vertex[]{v[0].remap(u1,v0),v[1].remap(u0,v0),v[2].remap(u0,v1),v[3].remap(u1,v1)},quad.direction());
                }
            });
        }
        if(faceMappings.isEmpty())return root;
        var children=new HashMap<List<String>,List<String>>();
        var allPaths=new ArrayList<>(paths.values());allPaths.addAll(cubePaths);
        for(var path:allPaths)children.computeIfAbsent(List.copyOf(path.subList(0,path.size()-1)),key->new ArrayList<>()).add(path.getLast());
        return withAuthoredFaces(root,List.of(),children,faceMappings.keySet());
    }

    private static ModelPart withAuthoredFaces(ModelPart original,List<String> path,
            Map<List<String>,List<String>> childNames,Set<List<String>> mapped) {
        var cubes=new ArrayList<ModelPart.Cuboid>();
        original.forEachCuboid(new net.minecraft.client.util.math.MatrixStack(),(matrix,name,index,cube)->{
            if(name.isEmpty())cubes.add(mapped.contains(path)?new FaceMappedCosmeticCuboid(cube):cube);
        });
        var children=new LinkedHashMap<String,ModelPart>();
        for(var name:childNames.getOrDefault(path,List.of())) {
            var childPath=new ArrayList<>(path);childPath.add(name);
            children.put(name,withAuthoredFaces(original.getChild(name),List.copyOf(childPath),childNames,mapped));
        }
        var part=new ModelPart(List.copyOf(cubes),children);
        part.setDefaultTransform(original.getDefaultTransform());part.setTransform(original.getTransform());
        // getTransform() intentionally omits scale; fractional imported cubes need it.
        part.xScale=original.xScale;part.yScale=original.yScale;part.zScale=original.zScale;
        part.visible=original.visible;part.hidden=original.hidden;
        return part;
    }
}
