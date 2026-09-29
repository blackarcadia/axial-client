package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.EnchantGlintConfig;
import com.axial.cosmetics.client.ShadowArmorGlint;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(RenderLayer.class)
public abstract class RenderLayerEnchantGlintMixin {
    @ModifyArg(method = "draw", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gl/DynamicUniforms;write(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"), index = 1)
    private Vector4fc axial_cosmetics$glintModulator(Vector4fc original) {
        if (((RenderLayer) (Object) this).getRenderPipeline() != RenderPipelines.GLINT) return original;
        ShadowArmorGlint.GlintType specialType = ShadowArmorGlint.type((RenderLayer) (Object) this);
        return EnchantGlintConfig.resolveModulator(original, specialType, EnchantGlintConfig.strength(),
                EnchantGlintConfig.customColor(), EnchantGlintConfig.color());
    }
}
