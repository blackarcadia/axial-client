package com.axial.cosmetics.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.base.RenderPassInfo;

import java.util.List;

@Mixin(
        targets = "software.bernie.geckolib.renderer.GeoArmorRenderer",
        remap = false
)
public abstract class GeoArmorGlintMixin {

    @Redirect(
            method = "submitRenderTasks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitCustom(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue$Custom;)V",
                    remap = false
            ),
            remap = false
    )
    private void axial$submitWithGlint(
            RenderCommandQueue queue,
            MatrixStack matrices,
            RenderLayer baseLayer,
            OrderedRenderCommandQueue.Custom renderer,
            RenderPassInfo<?> renderPassInfo
    ) {
        // Normal GEO render
        queue.submitCustom(matrices, baseLayer, renderer);

        // Only add the extra pass when the equipped item actually has glint.
        boolean hasGlint = renderPassInfo.getOrDefaultGeckolibData(
                DataTickets.HAS_GLINT,
                false
        );

        if (!hasGlint) {
            return;
        }

        List<RenderLayer> layers =
                ItemRenderer.getGlintRenderLayers(baseLayer, false, true);

        for (RenderLayer layer : layers) {
            if (layer == baseLayer) {
                continue;
            }

            queue.submitCustom(matrices, layer, renderer);
        }
    }
}