package com.axial.cosmetics.client;

import java.util.EnumSet;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;

/** Render authored face UVs directly. Sodium's base-cuboid fast path snapshots box UVs
 * during construction and cannot represent our subsequently imported face mapping. */
final class FaceMappedCosmeticCuboid extends ModelPart.Cuboid {
    FaceMappedCosmeticCuboid(ModelPart.Cuboid source) {
        super(0,0,source.minX,source.minY,source.minZ,
                source.maxX-source.minX,source.maxY-source.minY,source.maxZ-source.minZ,
                0,0,0,false,1,1,EnumSet.allOf(Direction.class));
        System.arraycopy(source.sides,0,sides,0,source.sides.length);
    }
    @Override public void renderCuboid(MatrixStack.Entry entry,VertexConsumer consumer,int light,int overlay,int color) {
        var point=new Vector3f();var normal=new Vector3f();
        for(var face:sides) {
            entry.transformNormal(face.direction(),normal);
            for(var vertex:face.vertices()) {
                entry.getPositionMatrix().transformPosition(vertex.worldX(),vertex.worldY(),vertex.worldZ(),point);
                consumer.vertex(point.x,point.y,point.z,color,vertex.u(),vertex.v(),overlay,light,normal.x,normal.y,normal.z);
            }
        }
    }
}
