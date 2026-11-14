package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.CapeObstruction;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Covers vanilla and client capes, without changing the player's saved cape selection. */
@Mixin(CapeFeatureRenderer.class)
public abstract class CapeFeatureObstructionMixin {
    @Inject(method="render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V",at=@At("HEAD"),cancellable=true)
    private void axial$hideObstructedCape(MatrixStack matrices,OrderedRenderCommandQueue commands,int light,PlayerEntityRenderState state,float yaw,float pitch,CallbackInfo ci){
        if(state.capeVisible&&CapeObstruction.hidesCape(state))ci.cancel();
    }
}
