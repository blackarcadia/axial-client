package com.axial.cosmetics.mixin;

import io.github.kosmx.emotes.PlatformTools;
import io.github.kosmx.emotes.arch.EmotecraftClientMod;
import io.github.kosmx.emotes.arch.screen.ingame.FastMenuScreen;
import io.github.kosmx.emotes.arch.screen.widget.AbstractFastChooseWidget;
import io.github.kosmx.emotes.arch.screen.widget.IChooseElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Plays the card actually under the cursor when the legacy wheel key is released. */
@Mixin(targets = "io.github.kosmx.emotes.arch.screen.ingame.FastMenuScreen", remap = false)
public abstract class EmoteRadialReleaseSelectMixin {
    @Shadow protected AbstractFastChooseWidget fastMenu;

    @Inject(method = "keyReleased", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$playHoveredEmoteOnRelease(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(PlatformTools.getConfig().oldChooseWheel.get())
                || !EmotecraftClientMod.OPEN_MENU_KEY.matchesKey(input)
                || fastMenu == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
        for (IChooseElement element : fastMenu.getChooseElements()) {
            if (element.hasEmote() && element.isMouseOver(mouseX, mouseY)) {
                ((FastMenuScreen) (Object) this).doHoverPart(element);
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
