package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.TitleScreenAccountsDropdown;
import io.github.kosmx.emotes.arch.screen.EmoteMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes the original quick-wheel button open emote-slot configuration. */
@Mixin(ButtonWidget.class)
public abstract class EmoteAllButtonPressMixin {
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$openEmoteConfiguration(CallbackInfo ci) {
        ButtonWidget button = (ButtonWidget) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        Screen screen = client.currentScreen;
        if (screen instanceof TitleScreen
                && "accounts".equalsIgnoreCase(button.getMessage().getString())) {
            TitleScreenAccountsDropdown.toggle();
            ci.cancel();
            return;
        }
        if (screen != null
                && "io.github.kosmx.emotes.arch.screen.ingame.FastMenuScreen".equals(screen.getClass().getName())
                && "All Emotes".equalsIgnoreCase(button.getMessage().getString())) {
            client.setScreen(new EmoteMenu(screen));
            ci.cancel();
        }
    }
}
