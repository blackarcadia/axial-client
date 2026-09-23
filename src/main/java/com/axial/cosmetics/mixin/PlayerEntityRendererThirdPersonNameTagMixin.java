package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ThirdPersonNameTagsConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererThirdPersonNameTagMixin {
    @ModifyReturnValue(method = "hasLabel(Lnet/minecraft/entity/PlayerLikeEntity;D)Z", at = @At("RETURN"))
    private boolean axial_cosmetics$showLocalNameTagInThirdPerson(boolean original, PlayerLikeEntity player, double squaredDistance) {
        MinecraftClient client = MinecraftClient.getInstance();
        return original || (ThirdPersonNameTagsConfig.enabled()
                && player == client.player
                && !client.options.getPerspective().isFirstPerson());
    }
}
