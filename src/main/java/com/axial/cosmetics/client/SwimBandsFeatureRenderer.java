package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.*;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** The supplied Bedrock cubes, attached to the native animated player arms. */
public final class SwimBandsFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState,PlayerEntityModel> {
    private static final Identifier TEXTURE=Identifier.of("axial_cosmetics","textures/entity/accessories/swim_bands.png");
    private final BandsModel bands=new BandsModel();
    public SwimBandsFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState,PlayerEntityModel> context){super(context);}
    @Override public void render(MatrixStack matrices,OrderedRenderCommandQueue commands,int light,PlayerEntityRenderState state,float yaw,float pitch){
        var world=MinecraftClient.getInstance().world;
        if(world==null||state.invisible||state.spectator||ItemSkinConflict.hidesCosmetics(state))return;
        var player=world.getEntityById(state.id);
        if(player==null||WardrobePreferences.accessoryFor(player.getUuid())!=WardrobeAccessory.SWIM_BANDS)return;
        bands.left.setTransform(getContextModel().leftArm.getTransform());bands.right.setTransform(getContextModel().rightArm.getTransform());
        // This argument is the ARGB tint; white preserves the supplied texture colours.
        FeatureRenderer.render(bands,TEXTURE,matrices,commands,light,state,0xFFFFFFFF,0);
    }
    private static final class BandsModel extends Model<PlayerEntityRenderState> {
        final ModelPart left,right;
        BandsModel(){super(model(),RenderLayers::entityCutoutNoCull);left=root.getChild("left");right=root.getChild("right");}
        @Override public void setAngles(PlayerEntityRenderState state){}
        private static ModelPart model(){
            var data=new ModelData();
            // Original geometry: left [3,18,-3], right [-9,18,-3], each 6x3x6.
            data.getRoot().addChild("left",ModelPartBuilder.create().uv(0,0).cuboid(-2,1,-3,6,3,6),ModelTransform.origin(5,2,0));
            data.getRoot().addChild("right",ModelPartBuilder.create().uv(0,0).cuboid(-4,1,-3,6,3,6),ModelTransform.origin(-5,2,0));
            return TexturedModelData.of(data,32,32).createModel();
        }
    }
}
