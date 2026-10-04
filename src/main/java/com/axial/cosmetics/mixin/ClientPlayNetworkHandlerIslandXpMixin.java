package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.IslandXpPopup;
import com.axial.cosmetics.client.EmotePermissionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Converts AxialSky's private Island XP system message into a HUD popup. */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerIslandXpMixin {
    private static final String MARKER = "[AXIAL_ISLAND_XP]";
    private static final String EMOTE_PERMISSIONS_MARKER = "[AXIAL_EMOTE_PERMISSIONS]";

    @Inject(method = "onGameMessage", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$showIslandXp(GameMessageS2CPacket packet, CallbackInfo ci) {
        String message = packet.content().getString();
        int emotePermissionsMarker = message.indexOf(EMOTE_PERMISSIONS_MARKER);
        if (emotePermissionsMarker >= 0) {
            EmotePermissionState.update(message.substring(emotePermissionsMarker + EMOTE_PERMISSIONS_MARKER.length()).trim());
            ci.cancel();
            return;
        }
        int marker = message.indexOf(MARKER);
        if (marker < 0) return;
        String amount = message.substring(marker + MARKER.length()).trim();
        if (!amount.isEmpty()) IslandXpPopup.show(amount);
        ci.cancel();
    }
}
