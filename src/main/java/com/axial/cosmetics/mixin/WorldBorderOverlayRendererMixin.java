package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.WorldBorderOverlayGeometry;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Retains AxialUtils' feature toggle while correcting its unbounded wall geometry. */
@Mixin(targets = "org.axial.axialutils.client.WorldBorderOverlayRenderer", remap = false)
public abstract class WorldBorderOverlayRendererMixin {
    @Inject(method = "drawWorldBorder", at = @At("HEAD"), cancellable = true)
    private static void axial_cosmetics$drawFiniteWalls(MinecraftClient client, CallbackInfo ci) {
        WorldBorderOverlayGeometry.render(client);
        ci.cancel();
    }
}
