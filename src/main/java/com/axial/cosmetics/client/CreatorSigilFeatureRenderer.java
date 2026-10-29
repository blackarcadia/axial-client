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

/** A block-built square halo with a cube travelling along its perimeter. */
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
            matrices.translate(0, -0.82 + Math.sin(state.age * 0.035) * 0.018, 0);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45));
            commands.submitModel(ring, state, matrices, RenderLayers.entityCutoutNoCull(TEXTURE),
                    LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0xFFAD70E0, null, 0, null);
            float[] position = perimeterPosition(state.age);
            matrices.translate(position[0] / 16f, -0.035f, position[1] / 16f);
            commands.submitModel(point, state, matrices, RenderLayers.entityCutoutNoCull(TEXTURE),
                    LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0xFFEED5FF, null, 0, null);
        } finally {
            matrices.pop();
        }
    }

    /** One eight-second lap, with straight edges and exact square corners. Model units. */
    public static float[] perimeterPosition(float age) {
        float progress = ((age % 160f) + 160f) % 160f / 40f;
        int edge = (int) progress;
        float offset = (progress - edge) * 11f;
        return switch (edge) {
            case 0 -> new float[]{-5.5f + offset, -5.5f};
            case 1 -> new float[]{5.5f, -5.5f + offset};
            case 2 -> new float[]{5.5f - offset, 5.5f};
            default -> new float[]{-5.5f, 5.5f - offset};
        };
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
                        .cuboid(-0.7f, -0.7f, -0.7f, 1.4f, 1.4f, 1.4f), ModelTransform.NONE);
            } else {
                data.getRoot().addChild("frame", ModelPartBuilder.create().uv(0, 0)
                        .cuboid(-6, -0.4f, -6, 12, 0.8f, 1)
                        .cuboid(-6, -0.4f, 5, 12, 0.8f, 1)
                        .cuboid(-6, -0.4f, -5, 1, 0.8f, 10)
                        .cuboid(5, -0.4f, -5, 1, 0.8f, 10), ModelTransform.NONE);
                for (int x : new int[]{-1, 1}) for (int z : new int[]{-1, 1}) {
                    data.getRoot().addChild("corner_" + x + "_" + z, ModelPartBuilder.create().uv(0, 0)
                            .cuboid(x * 5.5f - 0.8f, -0.65f, z * 5.5f - 0.8f, 1.6f, 1.3f, 1.6f), ModelTransform.NONE);
                }
            }
            return TexturedModelData.of(data, 16, 16);
        }
    }
}
