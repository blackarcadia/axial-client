package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.HitIndicators;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerHitIndicatorsMixin {
    // TAIL runs after the vanilla packet handler has enforced the client thread.
    @Inject(method = "onEntityDamage", at = @At("TAIL"))
    private void axial$hitIndicator(EntityDamageS2CPacket packet, CallbackInfo ci) {
        HitIndicators.onDamage(packet);
    }
}
