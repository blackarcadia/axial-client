package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.MenuMusicConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MusicTracker;
import net.minecraft.sound.MusicType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicTracker.class)
public abstract class MusicTrackerMixin {
    @Shadow @Final private MinecraftClient client;

    @Shadow private int timeUntilNextSong;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$skipVanillaMenuMusic(CallbackInfo ci) {
        if (this.client.world == null) {
            this.timeUntilNextSong = Integer.MAX_VALUE;
            ((MusicTracker) (Object) this).stop(MusicType.MENU);
            ci.cancel();
        }
    }
}
