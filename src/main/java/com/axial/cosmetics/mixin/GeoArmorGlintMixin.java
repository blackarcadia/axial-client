package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ShadowArmorGlint;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.item.Item;
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
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.GeoArmorRenderer;


@Mixin(
        targets = "software.bernie.geckolib.renderer.GeoArmorRenderer",
        remap = false
)
public abstract class GeoArmorGlintMixin {

    @Inject(method = "captureDefaultRenderState(Lnet/minecraft/item/Item;Lsoftware/bernie/geckolib/renderer/GeoArmorRenderer$RenderData;Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;F)V",
            at = @At("RETURN"), remap = true)
    private void axial$captureShadow(Item item, GeoArmorRenderer.RenderData data,
            BipedEntityRenderState state, float partialTick, CallbackInfo ci) {
        ((GeoRenderState) state).addGeckolibData(ShadowArmorGlint.GEO_SHADOW, ShadowArmorGlint.matches(data.itemStack()));
    }

    @Redirect(
            method = "submitRenderTasks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitCustom(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue$Custom;)V",
                    remap = true
            ),
            remap = false
    )
    private void axial$submitWithGlint(
            RenderCommandQueue queue,
            MatrixStack matrices,
            RenderLayer baseLayer,
            OrderedRenderCommandQueue.Custom renderer,
            RenderPassInfo<?> renderPassInfo,
            RenderCommandQueue renderTasks,
            RenderLayer renderType
    ) {
        // Normal GEO render
        queue.submitCustom(matrices, baseLayer, renderer);

        // Shadow armor always gets its fixed glint; other items use their normal glint flag.
        boolean hasGlint = renderPassInfo.getOrDefaultGeckolibData(
                DataTickets.HAS_GLINT,
                false
        );

        boolean shadow = renderPassInfo.getOrDefaultGeckolibData(ShadowArmorGlint.GEO_SHADOW, false);
        if (!hasGlint && !shadow) {
            return;
        }

        // Match the armor base layer's view offset. The held-item entity glint
        // lacks this offset and fails the equal-depth test on worn armor.
        queue.submitCustom(matrices, shadow ? ShadowArmorGlint.armorLayer() : RenderLayers.armorEntityGlint(), renderer);
    }
}
