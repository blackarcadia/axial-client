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

@Mixin(
        targets = "org.axial.axialutils.client.cosmetics.ArmorCosmeticGeoRenderer",
        remap = false
)
public abstract class ArmorCosmeticGeoRendererMixin {

    /**
     * Full-model cosmetics replace GeoArmorRenderer#submitRenderTasks, so the
     * redirect in GeoArmorGlintMixin is not reached for them.
     */
    @Redirect(
            method = "submitRenderTasks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitCustom(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue$Custom;)V",
                    remap = false
            ),
            remap = false
    )
    private void axial$submitFullModelWithGlint(
            RenderCommandQueue queue,
            MatrixStack matrices,
            RenderLayer baseLayer,
            OrderedRenderCommandQueue.Custom renderer,
            RenderPassInfo<?> renderPassInfo
    ) {
        queue.submitCustom(matrices, baseLayer, renderer);

        if (!renderPassInfo.getOrDefaultGeckolibData(DataTickets.HAS_GLINT, false)) {
            return;
        }

        for (RenderLayer glintLayer : ItemRenderer.getGlintRenderLayers(baseLayer, false, true)) {
            if (glintLayer != baseLayer) {
                queue.submitCustom(matrices, glintLayer, renderer);
            }
        }
    }
}
