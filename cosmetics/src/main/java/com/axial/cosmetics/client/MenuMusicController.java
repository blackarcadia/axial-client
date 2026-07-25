package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.MusicType;

public final class MenuMusicController {
    private static MenuMusicSoundInstance currentSound;

    private MenuMusicController() {
    }

    public static void tick(MinecraftClient client) {
        if (client.world != null) {
            stop(client);
            return;
        }

        if (currentSound == null) {
            currentSound = new MenuMusicSoundInstance();
        }

        if (!client.getSoundManager().isPlaying(currentSound)) {
            client.getSoundManager().play(currentSound);
        }
    }

    private static void stop(MinecraftClient client) {
        client.getMusicTracker().stop(MusicType.MENU);
        if (currentSound != null) {
            client.getSoundManager().stop(currentSound);
            currentSound = null;
        }
    }
}
