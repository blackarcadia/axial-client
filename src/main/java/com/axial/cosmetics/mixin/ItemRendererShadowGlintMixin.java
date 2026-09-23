package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ShadowArmorGlint;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererShadowGlintMixin {
    @WrapMethod(method = "renderItem")
    private static void axial$renderShadow(ItemDisplayContext context, MatrixStack matrices,
            VertexConsumerProvider consumers, int light, int overlay, int[] tints,
            List<BakedQuad> quads, RenderLayer layer, ItemRenderState.Glint glint, Operation<Void> original) {
        VertexConsumerProvider selected = consumers;
        if (ShadowArmorGlint.isShadow(layer)) {
            selected = requested -> consumers.getBuffer(requested.getRenderPipeline() == RenderPipelines.GLINT
                    ? ShadowArmorGlint.variant(requested) : requested);
            // Restore identity before vanilla's translucent-layer comparisons.
            layer = ShadowArmorGlint.original(layer);
        }
        original.call(context, matrices, selected, light, overlay, tints, quads, layer, glint);
    }
}
