package com.axial.cosmetics.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(
        targets = "org.axial.axialutils.client.cosmetics.ArmorCosmeticGeoRenderer",
        remap = false
)
public abstract class ArmorCosmeticGeoRendererMixin {

    private static List<RenderLayer> axial$getGlintLayers(
            RenderLayer baseLayer,
            boolean solid,
            boolean glint
    ) {
        return ItemRenderer.getGlintRenderLayers(baseLayer, solid, glint);
    }

    private static void axial$verifyQueue(RenderCommandQueue queue) {
        // Compile test only.
    }
}