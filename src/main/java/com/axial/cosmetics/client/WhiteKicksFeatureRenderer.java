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
    private final Identifier texture;
    private final WardrobeAccessory accessory;
    private final KicksModel kicks;
    public WhiteKicksFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState,PlayerEntityModel> context){this(context,WardrobeAccessory.WHITE_KICKS);}
    public static WhiteKicksFeatureRenderer whiteSneakers(FeatureRendererContext<PlayerEntityRenderState,PlayerEntityModel> context){
        return new WhiteKicksFeatureRenderer(context,WardrobeAccessory.WHITE_SNEAKERS);
    }
    private WhiteKicksFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState,PlayerEntityModel> context,WardrobeAccessory accessory){
        super(context);this.accessory=accessory;
        texture=Identifier.of("axial_cosmetics","textures/entity/accessories/"+accessory.id+".png");
        kicks=new KicksModel(accessory.id);
    }
    @Override public void render(MatrixStack matrices,OrderedRenderCommandQueue commands,int light,PlayerEntityRenderState state,float yaw,float pitch){
        if(state instanceof WardrobePreviewState preview) {
            if(!preview.accessories.contains(accessory))return;
        } else {
            var world=MinecraftClient.getInstance().world;
            if(world==null||state.invisible||state.spectator||ItemSkinConflict.hidesCosmetics(state,accessory.slots))return;
            var player=world.getEntityById(state.id);
            if(player==null||!WardrobePreferences.accessoriesFor(player.getUuid()).contains(accessory))return;
        }
        kicks.left.setTransform(getContextModel().leftLeg.getTransform());kicks.right.setTransform(getContextModel().rightLeg.getTransform());
        FeatureRenderer.render(kicks,texture,matrices,commands,light,state,0xFFFFFFFF,0);
    }
    private static final class KicksModel extends Model<PlayerEntityRenderState> {
        final ModelPart left,right;
        KicksModel(String id){super(model(id),RenderLayers::entityCutoutNoCull);left=root.getChild("left");right=root.getChild("right");}
        @Override public void setAngles(PlayerEntityRenderState state){}
    }
    static ModelPart model(){return model("white_kicks");}
    static ModelPart model(String id){
        var data=new ModelData();var bones=WhiteKicksGeometry.load(id);
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
                var local=cube.meshOrigin();var size=cube.uvSize();var scale=cube.meshScale();
                var origin=WhiteKicksGeometry.position(cube.pivot()).subtract(pivot);var angles=WhiteKicksGeometry.rotation(cube.rotation());
                part.addChild("cube"+index++,ModelPartBuilder.create().uv(cube.u(),cube.v()).cuboid(local.x(),local.y(),local.z(),size.x(),size.y(),size.z()),
                        new ModelTransform(origin.x(),origin.y(),origin.z(),angles.x(),angles.y(),angles.z(),scale.x(),scale.y(),scale.z()));
            }
        }
        return TexturedModelData.of(data,32,32).createModel();
    }
}
