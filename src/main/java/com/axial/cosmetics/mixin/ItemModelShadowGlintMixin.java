package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ShadowArmorGlint;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemModelManager.class)
public abstract class ItemModelShadowGlintMixin {
    @WrapOperation(method = "update", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/render/item/model/ItemModel;update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/item/ItemModelManager;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/util/HeldItemContext;I)V"))
    private void axial$markShadowLayers(ItemModel model, ItemRenderState state, ItemStack stack,
            ItemModelManager manager, ItemDisplayContext context, ClientWorld world,
            HeldItemContext holder, int seed, Operation<Void> original) {
        ItemRenderStateAccessor access = (ItemRenderStateAccessor) state;
        int firstLayer = access.axial$getLayerCount();
        original.call(model, state, stack, manager, context, world, holder, seed);
        if (!ShadowArmorGlint.matches(stack)) return;
        // Include the override in the GUI item-atlas cache key.
        state.addModelKey("axial:shadow_glint");
        for (int i = firstLayer; i < access.axial$getLayerCount(); i++) {
            ItemRenderState.LayerRenderState layer = access.axial$getLayers()[i];
            RenderLayer renderLayer = ((ItemLayerRenderStateAccessor) layer).axial$getRenderLayer();
            if (renderLayer != null) {
                layer.setRenderLayer(ShadowArmorGlint.variant(renderLayer));
                layer.setGlint(ItemRenderState.Glint.STANDARD);
            }
        }
    }
}
