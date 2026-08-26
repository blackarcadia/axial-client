package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

public final class MenuMusicSoundInstance extends MovingSoundInstance {
    public MenuMusicSoundInstance() {
        super(SoundEvent.of(AxialCosmetics.id("menu_music")), SoundCategory.MASTER, SoundInstance.createRandom());
        this.repeat = true;
        this.repeatDelay = 0;
        this.relative = true;
        this.attenuationType = AttenuationType.NONE;
        this.volume = MenuMusicConfig.volume();
        this.pitch = 1.0f;
    }

    @Override
    public void tick() {
        this.volume = MenuMusicConfig.volume();
    }
}
