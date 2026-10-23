package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.SmartMiningVisuals;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.axial.axialutils.client.AxialVisualEffects;
import org.axial.axialutils.client.LivingTransparencyState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Restores the omitted Smart Mining renderer using the library's existing eligibility rules. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererSmartMiningMixin {
    @Shadow public abstract Identifier getTexture(LivingEntityRenderState state);

    @Inject(method="updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",at=@At("TAIL"))
    private void axial$updateMiningFade(LivingEntity entity,LivingEntityRenderState state,float tickDelta,CallbackInfo ci) {
        // Set both true and false on every update: render states are reused.
        ((LivingTransparencyState)state).axialutils$setShouldFade(AxialVisualEffects.shouldFadeLivingEntity(entity));
    }

    @ModifyReturnValue(method="getMixColor",at=@At("RETURN"))
    private int axial$fadeMiningColor(int original,LivingEntityRenderState state) {
        return SmartMiningVisuals.color(original,((LivingTransparencyState)state).axialutils$shouldFade());
    }

    @Inject(method="getRenderLayer",at=@At("HEAD"),cancellable=true)
    private void axial$useMiningTransparency(LivingEntityRenderState state,boolean showBody,boolean translucent,boolean outline,CallbackInfoReturnable<RenderLayer> cir) {
        // Preserve vanilla invisibility and outline-only passes.
        if(showBody&&((LivingTransparencyState)state).axialutils$shouldFade())cir.setReturnValue(RenderLayers.entityTranslucent(getTexture(state)));
    }
}
