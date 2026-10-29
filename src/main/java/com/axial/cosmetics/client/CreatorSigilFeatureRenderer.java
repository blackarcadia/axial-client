package com.axial.cosmetics.client;

import net.minecraft.client.model.*;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/** A small solid ring; no particles, world effects, or visibility through walls. */
public final class CreatorSigilFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private static final Identifier TEXTURE = Identifier.of("axial_cosmetics", "textures/entity/creator_sigil.png");
    private final SigilModel ring = new SigilModel(false);
    private final SigilModel point = new SigilModel(true);

    public CreatorSigilFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    public static boolean shouldRender(PlayerEntityRenderState state) {
        return CreatorCosmetics.hasCreatorCape(state.skinTextures)
                && !state.invisible && !state.spectator && !state.sneaking
                && state.deathTime == 0 && state.pose == EntityPose.STANDING;
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue commands, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        if (!shouldRender(state)) return;
        matrices.push();
        try {
            // Player models use sixteen units per block, with negative Y above the head.
            matrices.translate(0, -0.78 + Math.sin(state.age * 0.035) * 0.012, 0);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(14));
            commands.submitModel(ring, state, matrices, RenderLayers.entityCutoutNoCull(TEXTURE),
                    LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0xFFB080D0, null, 0, null);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(state.age * 0.025f));
            commands.submitModel(point, state, matrices, RenderLayers.entityCutoutNoCull(TEXTURE),
                    LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0xFFEED5FF, null, 0, null);
        } finally {
            matrices.pop();
        }
    }

    private static final class SigilModel extends Model<PlayerEntityRenderState> {
        SigilModel(boolean point) {
            super(build(point).createModel(), RenderLayers::entityCutoutNoCull);
        }

        @Override public void setAngles(PlayerEntityRenderState state) { }

        private static TexturedModelData build(boolean point) {
            ModelData data = new ModelData();
            if (point) {
                data.getRoot().addChild("point", ModelPartBuilder.create().uv(0, 0)
                        .cuboid(4.4f, -0.4f, -0.4f, 0.8f, 0.8f, 0.8f), ModelTransform.NONE);
            } else {
                int segments = 48;
                for (int i = 0; i < segments; i++) {
                    float angle = (float) (i * Math.PI * 2 / segments);
                    data.getRoot().addChild("arc_" + i, ModelPartBuilder.create().uv(0, 0)
                                    .cuboid(-0.33f, -0.09f, -0.09f, 0.66f, 0.18f, 0.18f),
                            ModelTransform.of((float) Math.sin(angle) * 4.8f, 0,
                                    (float) Math.cos(angle) * 4.8f, 0, angle, 0));
                }
            }
            return TexturedModelData.of(data, 16, 16);
        }
    }
}
