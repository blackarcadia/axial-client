package com.axial.cosmetics.mixin;

import io.github.kosmx.emotes.arch.EmotecraftClientMod;
import io.github.kosmx.emotes.PlatformTools;
import io.github.kosmx.emotes.arch.screen.widget.AbstractFastChooseWidget;
import io.github.kosmx.emotes.arch.screen.widget.IChooseElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Plays the radial slot under the pointer when the Emotecraft menu key is released. */
@Mixin(targets = "io.github.kosmx.emotes.arch.screen.ingame.FastMenuScreen", remap = false)
public abstract class EmoteRadialReleaseSelectMixin {
    @Shadow protected AbstractFastChooseWidget fastMenu;

    @Inject(method = "method_25406", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$playHoveredEmoteOnRelease(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(PlatformTools.getConfig().oldChooseWheel.get())
                || !EmotecraftClientMod.OPEN_MENU_KEY.matchesKey(input) || fastMenu == null) return;

        IChooseElement selected = selectedElement();
        if (selected != null && selected.getEmote() != null) {
            selected.getEmote().playEmote();
        }
        ((Screen) (Object) this).close();
        cir.setReturnValue(true);
    }

    private IChooseElement selectedElement() {
        ClickableWidget wheel = (ClickableWidget) (Object) fastMenu;
        double x = MinecraftClient.getInstance().mouse.getX() * MinecraftClient.getInstance().getWindow().getScaledWidth()
                / MinecraftClient.getInstance().getWindow().getWidth();
        double y = MinecraftClient.getInstance().mouse.getY() * MinecraftClient.getInstance().getWindow().getScaledHeight()
                / MinecraftClient.getInstance().getWindow().getHeight();
        int size = Math.min(wheel.getWidth(), wheel.getHeight());
        int centerX = wheel.getX() + wheel.getWidth() / 2;
        int centerY = wheel.getY() + wheel.getHeight() / 2;
        int innerRadius = Math.max(46, Math.round(size * 0.19f));
        int outerRadius = Math.round(size * 0.49f);
        double relativeX = x - centerX;
        double relativeY = y - centerY;
        double distance = Math.hypot(relativeX, relativeY);
        if (distance < innerRadius || distance > outerRadius) return null;

        double segmentSize = Math.PI * 2 / 8;
        int index = Math.floorMod((int) Math.floor((Math.PI / 2 - Math.atan2(relativeY, relativeX) + segmentSize / 2) / segmentSize), 8);
        var elements = fastMenu.getChooseElements();
        return index < elements.size() ? elements.get(index) : null;
    }
}
