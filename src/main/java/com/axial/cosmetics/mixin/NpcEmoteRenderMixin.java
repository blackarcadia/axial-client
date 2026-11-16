package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.NpcEmoteDirector;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Re-evaluate from replay time for every rendered frame, including offline video export. */
@Mixin(value=PlayerEntityRenderer.class,priority=1100)
public abstract class NpcEmoteRenderMixin {
    @Inject(method="updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",at=@At("HEAD"))
    private void axial$updateActorPose(PlayerLikeEntity player,PlayerEntityRenderState state,float partial,CallbackInfo ci){NpcEmoteDirector.beforeRender(player);}
}
