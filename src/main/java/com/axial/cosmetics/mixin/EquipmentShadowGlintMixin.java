package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ShadowArmorGlint;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EquipmentRenderer.class)
public abstract class EquipmentShadowGlintMixin {
    @ModifyExpressionValue(method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;hasGlint()Z"))
    private boolean axial$enableShadowGlint(boolean original, @Local(argsOnly = true) ItemStack stack) {
        return original || ShadowArmorGlint.matches(stack);
    }

    @ModifyExpressionValue(method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/RenderLayers;armorEntityGlint()Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer axial$shadowLayer(RenderLayer original, @Local(argsOnly = true) ItemStack stack) {
        ShadowArmorGlint.GlintType specialType = ShadowArmorGlint.type(stack);
        return specialType == null ? original : ShadowArmorGlint.armorLayer(specialType);
    }
}
