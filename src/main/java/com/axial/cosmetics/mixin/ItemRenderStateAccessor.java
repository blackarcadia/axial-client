package com.axial.cosmetics.mixin;

import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.class)
public interface ItemRenderStateAccessor {
    @Accessor("layerCount")
    int axial$getLayerCount();

    @Accessor("layers")
    ItemRenderState.LayerRenderState[] axial$getLayers();
}
