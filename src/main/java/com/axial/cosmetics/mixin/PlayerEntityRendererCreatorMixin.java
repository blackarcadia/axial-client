package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.CreatorCosmetics;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererCreatorMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void axial$creatorCape(PlayerLikeEntity player, PlayerEntityRenderState state,
                                   float tickProgress, CallbackInfo ci) {
        // Vanilla still controls cape visibility, movement, armor offsets and elytra suppression.
        state.skinTextures = CreatorCosmetics.applyCape(player.getUuid(), state.skinTextures);
    }
}
