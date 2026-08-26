package com.axial.cosmetics.client;

import com.axial.cosmetics.data.CosmeticManager;
import com.axial.cosmetics.data.CosmeticSlot;
import com.axial.cosmetics.data.CosmeticTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

import java.util.EnumSet;
import java.util.List;

public class CosmeticFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private final CosmeticManager manager;
    private final BackpackModel backpackModel;
    private final MaskModel maskModel;
    public CosmeticFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context,
                                   CosmeticManager manager) {
        super(context);
        this.manager = manager;
        this.backpackModel = new BackpackModel();
        this.maskModel = new MaskModel();
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue commands, int light, PlayerEntityRenderState state,
                       float limbAngle, float limbDistance) {
        Entity entity = MinecraftClient.getInstance().world.getEntityById(state.id);
        if (entity == null) return;
        List<CosmeticTexture> cosmetics = manager.get(entity.getUuid(), entity.getName().getString());
        if (cosmetics.isEmpty()) return;

        PlayerEntityModel model = this.getContextModel();
        boolean head = model.head.visible;
        boolean hat = model.hat.visible;
        boolean body = model.body.visible;
        boolean la = model.leftArm.visible;
        boolean ra = model.rightArm.visible;
        boolean ll = model.leftLeg.visible;
        boolean rl = model.rightLeg.visible;

        for (CosmeticTexture cosmetic : cosmetics) {
            if (cosmetic.definition().modelId() != null && cosmetic.definition().slots().contains(CosmeticSlot.HEAD)) {
                FeatureRenderer.render(maskModel, cosmetic.texture(), matrices, commands, light, state,
                        OverlayTexture.DEFAULT_UV, 0);
            } else {
                setupVisibility(model, cosmetic.definition().slots());
                FeatureRenderer.render(model, cosmetic.texture(), matrices, commands, light, state,
                        OverlayTexture.DEFAULT_UV, 0);
                if (cosmetic.definition().slots().contains(CosmeticSlot.BACKPACK)) {
                    FeatureRenderer.render(backpackModel, cosmetic.texture(), matrices, commands, light, state,
                            OverlayTexture.DEFAULT_UV, 0);
                }
            }
        }

        model.head.visible = head;
        model.hat.visible = hat;
        model.body.visible = body;
        model.leftArm.visible = la;
        model.rightArm.visible = ra;
        model.leftLeg.visible = ll;
        model.rightLeg.visible = rl;
    }

    private void setupVisibility(PlayerEntityModel model, Iterable<CosmeticSlot> slots) {
        model.setVisible(false);
        EnumSet<CosmeticSlot> set = EnumSet.noneOf(CosmeticSlot.class);
        for (CosmeticSlot s : slots) set.add(s);

        model.head.visible = set.contains(CosmeticSlot.HEAD);
        model.hat.visible = set.contains(CosmeticSlot.HEAD);
        // Treat backpack as chest overlay for now
        model.body.visible = set.contains(CosmeticSlot.CHEST) || set.contains(CosmeticSlot.BACKPACK);
        model.leftArm.visible = set.contains(CosmeticSlot.LEFT_ARM);
        model.rightArm.visible = set.contains(CosmeticSlot.RIGHT_ARM);
        model.leftLeg.visible = set.contains(CosmeticSlot.LEGS) || set.contains(CosmeticSlot.BOOTS);
        model.rightLeg.visible = set.contains(CosmeticSlot.LEGS) || set.contains(CosmeticSlot.BOOTS);
    }

    private static class MaskModel extends Model<PlayerEntityRenderState> {
        MaskModel() {
            super(buildModel().createModel(), RenderLayers::entityCutoutNoCull);
        }

        @Override
        public void setAngles(PlayerEntityRenderState state) {
        }

        private static TexturedModelData buildModel() {
            ModelData data = new ModelData();
            record Cube(float fx, float fy, float fz, float tx, float ty, float tz, float ox, float oy, float oz, float rx, float ry, float rz) {}
            Cube[] cubes = new Cube[]{
                    new Cube(2,3,3,3,12,11,2,3,3,0,0,0),
                    new Cube(3,4,2.25f,4,11,3.25f,3,3,2.25f,0,(float)Math.toRadians(-45),0),
                    new Cube(9.875f,4,2.25f,10.875f,11,3.25f,9.875f,3,2.25f,0,(float)Math.toRadians(-45),0),
                    new Cube(2.875f,10,2.25f,9.875f,11,3.25f,6,11,2.25f,(float)Math.toRadians(-22.5),0,0),
                    new Cube(2.875f,3.625f,2.25f,9.875f,4.625f,3.25f,6,4.625f,2.25f,(float)Math.toRadians(-22.5),0,0),
                    new Cube(4.875f,4.375f,2.375f,7.875f,5.375f,3.375f,6.5f,5,2.875f,0,0,0),
                    new Cube(5.375f,3,2,7.375f,5,3,7,4.625f,2.5f,(float)Math.toRadians(-22.5),0,0),
                    new Cube(5.625f,2.625f,1.625f,7.125f,4.625f,2.25f,7.25f,3,1.75f,(float)Math.toRadians(22.5),0,0),
                    new Cube(5.375f,1.25f,0.625f,7.375f,3.25f,2.625f,7,2.875f,2.125f,(float)Math.toRadians(22.5),0,0),
                    new Cube(3.125f,3.875f,2.375f,5.125f,4.875f,3.375f,3.75f,4.5f,2.875f,0,0,(float)Math.toRadians(22.5)),
                    new Cube(7.625f,3.75f,2.375f,9.625f,4.75f,3.375f,9.25f,4.375f,2.875f,0,0,(float)Math.toRadians(-22.5)),
                    new Cube(10,4,3,11,12,11,10,3,3,0,0,0),
                    new Cube(3,4,10,10,11,11,9,3,10,0,0,0),
                    new Cube(3,3,3,11,4,11,10,3,3,0,0,0),
                    new Cube(3,11,3,10,12,11,10,11,3,0,0,0)
            };

            int idx = 0;
            for (Cube c : cubes) {
                float dx = c.tx - c.fx;
                float dy = c.ty - c.fy;
                float dz = c.tz - c.fz;
                float offX = c.fx - 8.0f - c.ox + 8.0f;
                float offY = c.fy - 8.0f - c.oy + 8.0f;
                float offZ = c.fz - 8.0f - c.oz + 8.0f;
                data.getRoot().addChild("p" + (idx++),
                        ModelPartBuilder.create().uv(0, 0).cuboid(offX, offY, offZ, dx, dy, dz, new Dilation(0.0f)),
                        ModelTransform.of(0, 0, 0, c.rx, c.ry, c.rz));
            }
            return TexturedModelData.of(data, 64, 64);
        }
    }

    private static class BackpackModel extends Model<PlayerEntityRenderState> {
        BackpackModel() {
            super(buildModel().createModel(), RenderLayers::entityCutoutNoCull);
        }

        @Override
        public void setAngles(PlayerEntityRenderState state) {
            // Static model; no per-state transforms needed.
        }

        private static TexturedModelData buildModel() {
            ModelData data = new ModelData();
            data.getRoot().addChild("backpack",
                    ModelPartBuilder.create()
                            .uv(0, 0)
                            .cuboid(-4.0F, 1.0F, 2.5F, 8.0F, 10.0F, 3.0F, new Dilation(0.3F)),
                    ModelTransform.NONE);
            return TexturedModelData.of(data, 64, 64);
        }
    }
}
