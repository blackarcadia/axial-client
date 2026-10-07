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
                axial_cosmetics$select(client, element, cir);
                return;
            }
        }

        // The legacy card hitboxes are animated and can be gone by the time the
        // release event arrives. Resolve the same card from its fixed wheel slot.
        int elementIndex = axial_cosmetics$elementAt(mouseX, mouseY);
        if (elementIndex >= 0 && elementIndex < fastMenu.getChooseElements().size()) {
            IChooseElement element = fastMenu.getChooseElements().get(elementIndex);
            if (element.hasEmote()) axial_cosmetics$select(client, element, cir);
        }
    }

    private void axial_cosmetics$select(MinecraftClient client, IChooseElement element,
                                        CallbackInfoReturnable<Boolean> cir) {
        ((FastMenuScreen) (Object) this).doHoverPart(element);
        // doHoverPart only closes after a successful play; releasing the wheel
        // must still dismiss it when the emote is unavailable.
        client.setScreen(null);
        cir.setReturnValue(true);
    }

    private int axial_cosmetics$elementAt(double mouseX, double mouseY) {
        int centerX = fastMenu.getX() + fastMenu.getWidth() / 2;
        int centerY = fastMenu.getY() + fastMenu.getHeight() / 2;
        int size = Math.min(fastMenu.getWidth(), fastMenu.getHeight());
        double x = mouseX - centerX;
        double y = mouseY - centerY;
        double distance = Math.hypot(x, y);
        if (distance < size * 0.20 || distance > size * 0.52) return -1;

        double segmentSize = Math.PI * 2 / 8;
        // PlayerChooseCircleElement places index zero at the bottom and then
        // advances clockwise around the wheel.
        return Math.floorMod((int) Math.floor((Math.PI / 2 - Math.atan2(y, x) + segmentSize / 2) / segmentSize), 8);
    }
}
