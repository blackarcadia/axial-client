package com.axial.cosmetics.mixin;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClickableWidget.class)
public abstract class TitleScreenButtonPressSoundMixin {
    private static final Identifier PRESS_SOUND = AxialCosmetics.id("ui.menu_button_press");

    @Inject(method = "playDownSound", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$playPressSound(SoundManager soundManager, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen currentScreen = client.currentScreen;
        if (currentScreen != null && currentScreen.getClass().getName().startsWith("com.axial.")) {
            return;
        }

        client.getSoundManager().play(new PositionedSoundInstance(
                PRESS_SOUND,
                SoundCategory.UI,
                0.5f,
                1.0f,
                SoundInstance.createRandom(),
                false,
                0,
                SoundInstance.AttenuationType.NONE,
                0.0,
                0.0,
                0.0,
                false
        ));
        ci.cancel();
    }
}
