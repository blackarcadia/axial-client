package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ShadowArmorGlint;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.axial.axialutils.client.cosmetics.ArmorCosmeticItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.base.RenderPassInfo;

@Mixin(
        targets = "org.axial.axialutils.client.cosmetics.ArmorCosmeticGeoRenderer",
        remap = false
)
public abstract class ArmorCosmeticGeoRendererMixin {

    // This renderer replaces captureDefaultRenderState without calling its superclass.
    @Inject(method = "captureDefaultRenderState(Lorg/axial/axialutils/client/cosmetics/ArmorCosmeticItem;Lsoftware/bernie/geckolib/renderer/GeoArmorRenderer$RenderData;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("RETURN"), remap = true)
    private void axial$captureShadow(ArmorCosmeticItem item, GeoArmorRenderer.RenderData data,
            PlayerEntityRenderState state, float partialTick, CallbackInfo ci) {
        ((GeoRenderState) state).addGeckolibData(ShadowArmorGlint.GEO_SHADOW, ShadowArmorGlint.matches(data.itemStack()));
    }

    /**
     * Full-model cosmetics replace GeoArmorRenderer#submitRenderTasks, so the
     * redirect in GeoArmorGlintMixin is not reached for them.
     */
    @Redirect(
            method = "submitRenderTasks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitCustom(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue$Custom;)V",
                    remap = true
            ),
            remap = false
    )
    private void axial$submitFullModelWithGlint(
            RenderCommandQueue queue,
            MatrixStack matrices,
            RenderLayer baseLayer,
            OrderedRenderCommandQueue.Custom renderer,
            RenderPassInfo<?> renderPassInfo,
            RenderCommandQueue renderTasks,
            RenderLayer renderType
    ) {
        queue.submitCustom(matrices, baseLayer, renderer);

        boolean shadow = renderPassInfo.getOrDefaultGeckolibData(ShadowArmorGlint.GEO_SHADOW, false);
        if (!shadow && !renderPassInfo.getOrDefaultGeckolibData(DataTickets.HAS_GLINT, false)) {
            return;
        }

        // Use the same view offset and texture transform as vanilla worn armor.
        queue.submitCustom(matrices, shadow ? ShadowArmorGlint.armorLayer() : RenderLayers.armorEntityGlint(), renderer);
    }
}
