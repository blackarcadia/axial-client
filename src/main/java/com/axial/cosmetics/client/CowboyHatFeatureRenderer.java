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

/** Supplied headwear geometry follows the native animated head. */
public final class CowboyHatFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState,PlayerEntityModel> {
    private final Identifier texture;
    private final WardrobeAccessory accessory;
    private final HatModel hat;
    public CowboyHatFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState,PlayerEntityModel> context){this(context,WardrobeAccessory.COWBOY_HAT);}
    public static CowboyHatFeatureRenderer glasses3d(FeatureRendererContext<PlayerEntityRenderState,PlayerEntityModel> context){return new CowboyHatFeatureRenderer(context,WardrobeAccessory.GLASSES_3D);}
    private CowboyHatFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState,PlayerEntityModel> context,WardrobeAccessory accessory){
        super(context);this.accessory=accessory;
        texture=Identifier.of("axial_cosmetics","textures/entity/accessories/"+accessory.id+".png");
        hat=new HatModel(accessory.id);
    }
    @Override public void render(MatrixStack matrices,OrderedRenderCommandQueue commands,int light,PlayerEntityRenderState state,float yaw,float pitch) {
        if(state instanceof WardrobePreviewState preview) {
            if(!preview.accessories.contains(accessory))return;
        } else {
            var world=MinecraftClient.getInstance().world;
            if(world==null||state.invisible||state.spectator||ItemSkinConflict.hidesCosmetics(state,accessory.slots))return;
            var player=world.getEntityById(state.id);
            if(player==null||!WardrobePreferences.accessoriesFor(player.getUuid()).contains(accessory))return;
        }
        hat.head.setTransform(getContextModel().head.getTransform());
        FeatureRenderer.render(hat,texture,matrices,commands,light,state,0xFFFFFFFF,0);
    }
    static ModelPart model(){return model("cowboy_hat");}
    static ModelPart model(String id){return BedrockCosmeticModel.bake(id,Map.of("bipedHead",
            new BedrockCosmeticModel.Anchor("head",new BedrockCosmeticGeometry.Vec(0,0,0))),id.equals("cowboy_hat")?64:32,id.equals("cowboy_hat")?64:32);}
    private static final class HatModel extends Model<PlayerEntityRenderState> {
        final ModelPart head;
        HatModel(String id){super(model(id),RenderLayers::entityCutoutNoCull);head=root.getChild("head");}
        @Override public void setAngles(PlayerEntityRenderState state){}
    }
}
