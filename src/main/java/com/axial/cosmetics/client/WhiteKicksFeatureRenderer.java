package com.axial.cosmetics.client;

import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.*;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Imported sneaker bones retain their authored rotations under each animated player leg. */
public final class WhiteKicksFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState,PlayerEntityModel> {
    private static final Identifier TEXTURE=Identifier.of("axial_cosmetics","textures/entity/accessories/white_kicks.png");
    private final KicksModel kicks=new KicksModel();
    public WhiteKicksFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState,PlayerEntityModel> context){super(context);}
    @Override public void render(MatrixStack matrices,OrderedRenderCommandQueue commands,int light,PlayerEntityRenderState state,float yaw,float pitch){
        var world=MinecraftClient.getInstance().world;
        if(world==null||state.invisible||state.spectator||ItemSkinConflict.hidesCosmetics(state))return;
        var player=world.getEntityById(state.id);
        if(player==null||WardrobePreferences.accessoryFor(player.getUuid())!=WardrobeAccessory.WHITE_KICKS)return;
        kicks.left.setTransform(getContextModel().leftLeg.getTransform());kicks.right.setTransform(getContextModel().rightLeg.getTransform());
        FeatureRenderer.render(kicks,TEXTURE,matrices,commands,light,state,0xFFFFFFFF,0);
    }
    private static final class KicksModel extends Model<PlayerEntityRenderState> {
        final ModelPart left,right;
        KicksModel(){super(model(),RenderLayers::entityCutoutNoCull);left=root.getChild("left");right=root.getChild("right");}
        @Override public void setAngles(PlayerEntityRenderState state){}
    }
    static ModelPart model(){
        var data=new ModelData();var bones=WhiteKicksGeometry.load();
        var parts=new HashMap<String,ModelPartData>();var pivots=new HashMap<String,WhiteKicksGeometry.Vec>();
        for(String side:List.of("left","right")){
            float x=side.equals("left")?2:-2;
            parts.put(side,data.getRoot().addChild(side,ModelPartBuilder.create(),ModelTransform.origin(x,12,0)));
            pivots.put(side,new WhiteKicksGeometry.Vec(x,12,0));
        }
        for(var bone:bones){
            String parent=bone.parent()==null?(bone.name().equals("bipedLeftLeg")?"left":"right"):bone.parent();
            var pivot=WhiteKicksGeometry.position(bone.pivot());var offset=pivot.subtract(pivots.get(parent));
            var rotation=WhiteKicksGeometry.rotation(bone.rotation());
            var part=parts.get(parent).addChild(bone.name(),ModelPartBuilder.create(),ModelTransform.of(offset.x(),offset.y(),offset.z(),rotation.x(),rotation.y(),rotation.z()));
            parts.put(bone.name(),part);pivots.put(bone.name(),pivot);
            int index=0;
            for(var cube:bone.cubes()){
                var local=cube.localOrigin();var size=cube.size();
                var origin=WhiteKicksGeometry.position(cube.pivot()).subtract(pivot);var angles=WhiteKicksGeometry.rotation(cube.rotation());
                part.addChild("cube"+index++,ModelPartBuilder.create().uv(cube.u(),cube.v()).cuboid(local.x(),local.y(),local.z(),size.x(),size.y(),size.z()),
                        ModelTransform.of(origin.x(),origin.y(),origin.z(),angles.x(),angles.y(),angles.z()));
            }
        }
        return TexturedModelData.of(data,32,32).createModel();
    }
}
