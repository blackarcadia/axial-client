package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ItemScalerConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class ItemInHandRendererItemScalerMixin {
    private float axial_cosmetics$itemScalerScale = 1.0f;

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void axial_cosmetics$captureHeldItemScale(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light, CallbackInfo ci) {
        axial_cosmetics$itemScalerScale = hand == Hand.MAIN_HAND ? ItemScalerConfig.mainHandScale() : ItemScalerConfig.offHandScale();
    }

    @Redirect(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"
            )
    )
    private void axial_cosmetics$renderScaledHeldItem(HeldItemRenderer renderer, LivingEntity entity, ItemStack stack, ItemDisplayContext renderMode, MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light) {
        if (axial_cosmetics$itemScalerScale != 1.0f) {
            matrices.push();
            matrices.scale(axial_cosmetics$itemScalerScale, axial_cosmetics$itemScalerScale, axial_cosmetics$itemScalerScale);
            axial_cosmetics$invokeRenderItem(entity, stack, renderMode, matrices, commandQueue, light);
            matrices.pop();
            return;
        }

        axial_cosmetics$invokeRenderItem(entity, stack, renderMode, matrices, commandQueue, light);
    }

    @Invoker("renderItem")
    protected abstract void axial_cosmetics$invokeRenderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext renderMode, MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light);
}
