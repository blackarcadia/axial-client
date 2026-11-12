package com.axial.cosmetics.client;

import java.util.*;
import net.minecraft.client.model.*;

/** Bake imported bones beneath native player-part anchors, preserving box UVs. */
final class BedrockCosmeticModel {
    record Anchor(String name,BedrockCosmeticGeometry.Vec pivot) {}
    private BedrockCosmeticModel() {}

    static ModelPart bake(String id,Map<String,Anchor> anchors) {
        var data=new ModelData();
        var parts=new HashMap<String,ModelPartData>();
        var pivots=new HashMap<String,BedrockCosmeticGeometry.Vec>();
        for(var anchor:anchors.values()) {
            var p=anchor.pivot();
            parts.put(anchor.name(),data.getRoot().addChild(anchor.name(),ModelPartBuilder.create(),ModelTransform.origin(p.x(),p.y(),p.z())));
            pivots.put(anchor.name(),p);
        }
        for(var bone:BedrockCosmeticGeometry.load(id)) {
            String parent=bone.parent()==null?anchors.get(bone.name()).name():bone.parent();
            var pivot=BedrockCosmeticGeometry.position(bone.pivot());
            var offset=pivot.subtract(pivots.get(parent));
            var angles=BedrockCosmeticGeometry.rotation(bone.rotation());
            var part=parts.get(parent).addChild(bone.name(),ModelPartBuilder.create(),
                    ModelTransform.of(offset.x(),offset.y(),offset.z(),angles.x(),angles.y(),angles.z()));
            parts.put(bone.name(),part);pivots.put(bone.name(),pivot);
            int index=0;
            for(var cube:bone.cubes()) {
                var local=cube.meshOrigin();var size=cube.uvSize();var scale=cube.meshScale();
                var origin=BedrockCosmeticGeometry.position(cube.pivot()).subtract(pivot);
                var rotation=BedrockCosmeticGeometry.rotation(cube.rotation());
                var dilation=new Dilation(cube.inflate()/scale.x(),cube.inflate()/scale.y(),cube.inflate()/scale.z());
                part.addChild("cube"+index++,ModelPartBuilder.create().uv(cube.u(),cube.v()).mirrored(cube.mirror())
                                .cuboid(local.x(),local.y(),local.z(),size.x(),size.y(),size.z(),dilation),
                        new ModelTransform(origin.x(),origin.y(),origin.z(),rotation.x(),rotation.y(),rotation.z(),scale.x(),scale.y(),scale.z()));
            }
        }
        return TexturedModelData.of(data,32,32).createModel();
    }
}
