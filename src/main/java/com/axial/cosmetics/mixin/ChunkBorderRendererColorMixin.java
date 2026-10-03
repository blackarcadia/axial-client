package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ChunkBordersConfig;
import net.minecraft.client.render.debug.ChunkBorderDebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces vanilla's three fixed chunk-border line colours with the selected colour. */
@Mixin(ChunkBorderDebugRenderer.class)
public abstract class ChunkBorderRendererColorMixin {
    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/debug/ChunkBorderDebugRenderer;DARK_CYAN:I"))
    private int axial_cosmetics$colorCellBorders() {
        return ChunkBordersConfig.color();
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/debug/ChunkBorderDebugRenderer;YELLOW:I"))
    private int axial_cosmetics$colorThinLines() {
        return ChunkBordersConfig.color();
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/debug/ChunkBorderDebugRenderer;LIGHT_RED:I"))
    private int axial_cosmetics$colorMajorLines() {
        return ChunkBordersConfig.color();
    }
}
