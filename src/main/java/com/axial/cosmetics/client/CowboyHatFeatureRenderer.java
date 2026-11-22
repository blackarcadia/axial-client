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

/** Supplied Cowboy Hat geometry follows the native animated head. */
public final class CowboyHatFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState,PlayerEntityModel> {
    private static final Identifier TEXTURE=Identifier.of("axial_cosmetics","textures/entity/accessories/cowboy_hat.png");
    private final HatModel hat=new HatModel();
    public CowboyHatFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState,PlayerEntityModel> context){super(context);}
    @Override public void render(MatrixStack matrices,OrderedRenderCommandQueue commands,int light,PlayerEntityRenderState state,float yaw,float pitch) {
        if(state instanceof WardrobePreviewState preview) {
            if(!preview.accessories.contains(WardrobeAccessory.COWBOY_HAT))return;
        } else {
            var world=MinecraftClient.getInstance().world;
            if(world==null||state.invisible||state.spectator||ItemSkinConflict.hidesCosmetics(state,WardrobeAccessory.COWBOY_HAT.slots))return;
            var player=world.getEntityById(state.id);
            if(player==null||!WardrobePreferences.accessoriesFor(player.getUuid()).contains(WardrobeAccessory.COWBOY_HAT))return;
        }
        hat.head.setTransform(getContextModel().head.getTransform());
        FeatureRenderer.render(hat,TEXTURE,matrices,commands,light,state,0xFFFFFFFF,0);
    }
    static ModelPart model(){return BedrockCosmeticModel.bake("cowboy_hat",Map.of("bipedHead",
            new BedrockCosmeticModel.Anchor("head",new BedrockCosmeticGeometry.Vec(0,0,0))),64,64);}
    private static final class HatModel extends Model<PlayerEntityRenderState> {
        final ModelPart head;
        HatModel(){super(model(),RenderLayers::entityCutoutNoCull);head=root.getChild("head");}
        @Override public void setAngles(PlayerEntityRenderState state){}
    }
}
