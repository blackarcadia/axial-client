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

/** A torso anchored companion that floats above the shoulder rather than swinging with the arm. */
public final class ShoulderPetFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState,PlayerEntityModel> {
    private final Identifier texture;
    private final WardrobeAccessory accessory;
    private final PetModel pet;
    public ShoulderPetFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState,PlayerEntityModel> context){this(context,WardrobeAccessory.PINK_SLIME);}
    private ShoulderPetFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState,PlayerEntityModel> context,WardrobeAccessory accessory){
        super(context);this.accessory=accessory;pet=new PetModel(accessory.id);
        texture=Identifier.of("axial_cosmetics","textures/entity/accessories/"+accessory.id+".png");
    }
    public static ShoulderPetFeatureRenderer ghostlyGhost(FeatureRendererContext<PlayerEntityRenderState,PlayerEntityModel> context){return new ShoulderPetFeatureRenderer(context,WardrobeAccessory.GHOSTLY_GHOST);}
    public static ShoulderPetFeatureRenderer pookles(FeatureRendererContext<PlayerEntityRenderState,PlayerEntityModel> context){return new ShoulderPetFeatureRenderer(context,WardrobeAccessory.POOKLES);}
    public static ShoulderPetFeatureRenderer ufo(FeatureRendererContext<PlayerEntityRenderState,PlayerEntityModel> context){return new ShoulderPetFeatureRenderer(context,WardrobeAccessory.UFO);}
    @Override public void render(MatrixStack matrices,OrderedRenderCommandQueue commands,int light,PlayerEntityRenderState state,float yaw,float pitch) {
        if(state instanceof WardrobePreviewState preview) {
            if(!preview.accessories.contains(accessory))return;
        } else {
            var world=MinecraftClient.getInstance().world;
            if(world==null||state.invisible||state.spectator)return;
            var player=world.getEntityById(state.id);
            if(player==null||!WardrobePreferences.accessoriesFor(player.getUuid()).contains(accessory))return;
        }
        pet.body.setTransform(getContextModel().body.getTransform());
        FeatureRenderer.render(pet,texture,matrices,commands,light,state,0xFFFFFFFF,0);
    }
    static ModelPart model(){return model("pink_slime");}
    static ModelPart model(String id){return BedrockCosmeticModel.bake(id,Map.of(id.equals("ufo")?"spinnybottom2":"body",
            new BedrockCosmeticModel.Anchor("torso",new BedrockCosmeticGeometry.Vec(0,0,0))),switch(id){case "ghostly_ghost"->64;case "pookles"->96;default->32;},id.equals("ufo")?32:8);}
    static Map<String,ModelPart> bones(ModelPart root,String id) {
        var result=new HashMap<String,ModelPart>();result.put("torso",root.getChild("torso"));
        for(var bone:BedrockCosmeticGeometry.load(id))result.put(bone.name(),result.get(bone.parent()==null?"torso":bone.parent()).getChild(bone.name()));
        return result;
    }
    static void animate(Map<String,ModelPart> bones,ShoulderPetAnimation animation,float seconds) {
        for(var name:animation.boneNames()) {
            var part=bones.get(name);part.resetTransform();var pose=animation.sample(name,seconds);
            part.originX+=pose.position().x();part.originY-=pose.position().y();part.originZ+=pose.position().z();
            var r=BedrockCosmeticGeometry.rotation(pose.rotation());part.pitch+=r.x();part.yaw+=r.y();part.roll+=r.z();
            part.xScale=pose.scale().x();part.yScale=pose.scale().y();part.zScale=pose.scale().z();
        }
    }
    private static final class PetModel extends Model<PlayerEntityRenderState> {
        private final ShoulderPetAnimation animation;
        private final Map<String,ModelPart> bones;
        final ModelPart body;
        PetModel(String id){super(model(id),RenderLayers::entityCutoutNoCull);body=root.getChild("torso");bones=bones(root,id);animation=new ShoulderPetAnimation(id);}
        @Override public void setAngles(PlayerEntityRenderState state){animate(bones,animation,state.age/20f);}
    }
}
