package com.axial.cosmetics.client;

import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.*;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Supplied rib geometry follows the native animated torso independently of shoes. */
public final class RibcageFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState,PlayerEntityModel> {
    private static final Identifier TEXTURE=Identifier.of("axial_cosmetics","textures/entity/accessories/ribcage.png");
    private final RibModel ribs=new RibModel();
    public RibcageFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState,PlayerEntityModel> context){super(context);}
    @Override public void render(MatrixStack matrices,OrderedRenderCommandQueue commands,int light,PlayerEntityRenderState state,float yaw,float pitch) {
        if(state instanceof WardrobePreviewState preview) {
            if(!preview.accessories.contains(WardrobeAccessory.RIBCAGE))return;
        } else {
            var world=MinecraftClient.getInstance().world;
            if(world==null||state.invisible||state.spectator||ItemSkinConflict.hidesCosmetics(state,WardrobeAccessory.RIBCAGE.slots))return;
            var player=world.getEntityById(state.id);
            if(player==null||!WardrobePreferences.accessoriesFor(player.getUuid()).contains(WardrobeAccessory.RIBCAGE))return;
        }
        ribs.body.setTransform(getContextModel().body.getTransform());
        FeatureRenderer.render(ribs,TEXTURE,matrices,commands,light,state,0xFFFFFFFF,0);
    }
    static ModelPart model(){return BedrockCosmeticModel.bake("ribcage",Map.of("bipedBody",
            new BedrockCosmeticModel.Anchor("body",new BedrockCosmeticGeometry.Vec(0,0,0))));}
    private static final class RibModel extends Model<PlayerEntityRenderState> {
        final ModelPart body;
        RibModel(){super(model(),RenderLayers::entityCutoutNoCull);body=root.getChild("body");}
        @Override public void setAngles(PlayerEntityRenderState state){}
    }
}
