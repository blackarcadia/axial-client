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

/** A torso anchored companion that floats above the shoulder rather than swinging with the arm. */
public final class ShoulderPetFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState,PlayerEntityModel> {
    private static final Identifier TEXTURE=Identifier.of("axial_cosmetics","textures/entity/accessories/pink_slime.png");
    private final PetModel pet=new PetModel();
    public ShoulderPetFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState,PlayerEntityModel> context){super(context);}
    @Override public void render(MatrixStack matrices,OrderedRenderCommandQueue commands,int light,PlayerEntityRenderState state,float yaw,float pitch) {
        if(state instanceof WardrobePreviewState preview) {
            if(!preview.accessories.contains(WardrobeAccessory.PINK_SLIME))return;
        } else {
            var world=MinecraftClient.getInstance().world;
            if(world==null||state.invisible||state.spectator)return;
            var player=world.getEntityById(state.id);
            if(player==null||!WardrobePreferences.accessoriesFor(player.getUuid()).contains(WardrobeAccessory.PINK_SLIME))return;
        }
        pet.body.setTransform(getContextModel().body.getTransform());
        FeatureRenderer.render(pet,TEXTURE,matrices,commands,light,state,0xFFFFFFFF,0);
    }
    static ModelPart model(){return BedrockCosmeticModel.bake("pink_slime",Map.of("body",
            new BedrockCosmeticModel.Anchor("torso",new BedrockCosmeticGeometry.Vec(0,0,0))),32,8);}
    private static final class PetModel extends Model<PlayerEntityRenderState> {
        private final ShoulderPetAnimation animation=new ShoulderPetAnimation();
        final ModelPart body,slime;
        PetModel(){super(model(),RenderLayers::entityCutoutNoCull);body=root.getChild("torso");slime=body.getChild("body").getChild("slime");}
        @Override public void setAngles(PlayerEntityRenderState state){
            slime.resetTransform();
            var pose=animation.sample(state.age/20f);
            slime.originX+=pose.position().x();slime.originY-=pose.position().y();slime.originZ+=pose.position().z();
            var r=BedrockCosmeticGeometry.rotation(pose.rotation());slime.pitch+=r.x();slime.yaw+=r.y();slime.roll+=r.z();
            slime.xScale=pose.scale().x();slime.yScale=pose.scale().y();slime.zScale=pose.scale().z();
        }
    }
}
