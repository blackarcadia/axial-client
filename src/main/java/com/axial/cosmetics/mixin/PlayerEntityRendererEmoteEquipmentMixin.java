package com.axial.cosmetics.mixin;

import io.github.kosmx.emotes.main.mixinFunctions.IPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps an emote's silhouette clear without changing the player's actual
 * equipment. The render state is rebuilt every frame, so equipment and capes return
 * immediately when Emotecraft stops the emote.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererEmoteEquipmentMixin {
    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void axial$hideEquipmentDuringEmote(
            PlayerLikeEntity player,
            PlayerEntityRenderState state,
            float tickProgress,
            CallbackInfo ci
    ) {
        if (!(player instanceof IPlayerEntity emotePlayer) || !emotePlayer.isPlayingEmote()) {
            return;
        }

        state.equippedHeadStack = ItemStack.EMPTY;
        state.equippedChestStack = ItemStack.EMPTY;
        state.equippedLegsStack = ItemStack.EMPTY;
        state.equippedFeetStack = ItemStack.EMPTY;
        state.rightHandItem = ItemStack.EMPTY;
        state.leftHandItem = ItemStack.EMPTY;
        state.capeVisible = false;
    }
}
