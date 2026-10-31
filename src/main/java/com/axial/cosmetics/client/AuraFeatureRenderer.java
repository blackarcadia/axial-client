package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Smooth, depth-tested wind geometry. Rendering only; no particles/entities or server mutations. */
public final class AuraFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState,PlayerEntityModel> {
    private static final Identifier WHITE=Identifier.of("axial_cosmetics","textures/entity/auras/white.png");
    public AuraFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState,PlayerEntityModel> context){super(context);}
    @Override public void render(MatrixStack matrices,OrderedRenderCommandQueue commands,int light,PlayerEntityRenderState state,float yaw,float pitch) {
        var client=MinecraftClient.getInstance();
        if(client.world==null||state.invisible||state.spectator)return;
        var entity=client.world.getEntityById(state.id);
        if(entity==null||!HalloweenSkins.isActualPlayer(entity.getUuid()))return;
        if(WardrobePreferences.auraFor(entity.getUuid())!=WardrobeAura.HEXWIND)return;
        double distance=client.player==null?0:entity.squaredDistanceTo(client.player);
        if(distance>64*64)return;
        float time=state.age+(entity.getUuid().hashCode()&127);
        var mesh=HexwindGeometry.frame(time,distance<24*24);
        commands.submitCustom(matrices,RenderLayers.entityTranslucentEmissiveNoOutline(WHITE),(entry,vertices)->{
            for(var quad:mesh) {
                vertex(vertices,entry,quad.a(),quad.color());vertex(vertices,entry,quad.b(),quad.color());
                vertex(vertices,entry,quad.c(),quad.color());vertex(vertices,entry,quad.d(),quad.color());
            }
        });
    }
    private static void vertex(VertexConsumer vertices,MatrixStack.Entry entry,HexwindGeometry.Point p,int color) {
        // LivingEntityRenderer's model origin is 1.501 blocks above the feet, with inverted Y.
        vertices.vertex(entry,p.x(),1.501f-p.y(),p.z()).color(color).texture(.5f,.5f)
                .overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(entry,0,1,0);
    }
}
