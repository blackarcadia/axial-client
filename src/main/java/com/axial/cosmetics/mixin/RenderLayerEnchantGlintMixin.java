package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.EnchantGlintConfig;
import com.axial.cosmetics.client.ShadowArmorGlint;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import org.joml.Vector4f;
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
        if (specialType != null) {
            float strength = ShadowArmorGlint.STRENGTH;
            return specialType == ShadowArmorGlint.GlintType.SHADOW
                    ? new Vector4f(strength, strength, strength, -1.0f)
                    : new Vector4f(0.5f * strength, strength, 0.0f, -1.0f);
        }
        float strength = EnchantGlintConfig.strength();
        if (!EnchantGlintConfig.customColor()) {
            return strength == 1.0f ? original : new Vector4f(strength, strength, strength, 1.0f);
        }
        int color = EnchantGlintConfig.color();
        // Negative alpha selects recolouring in glint.fsh; it is never used as opacity.
        return new Vector4f(((color >> 16) & 255) / 255.0f * strength,
                ((color >> 8) & 255) / 255.0f * strength,
                (color & 255) / 255.0f * strength, -1.0f);
    }
}
