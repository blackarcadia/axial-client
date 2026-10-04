package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.EmoteSlotPickerScreen;
import io.github.kosmx.emotes.arch.screen.widget.IChooseElement;
import io.github.kosmx.emotes.server.config.Serializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.AbstractInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Opens an emote picker when an empty wheel slot is clicked. */
@Mixin(targets = "io.github.kosmx.emotes.arch.screen.ingame.FastMenuScreen", remap = false)
public abstract class EmoteFastWheelAssignmentMixin {
    @Inject(method = "onClick", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$assignEmptyWheelSlot(IChooseElement slot, AbstractInput input, boolean doubled,
                                                      CallbackInfoReturnable<Boolean> cir) {
        // Left-click assigns an empty slot. Right-click replaces a filled slot without playing it.
        if (slot.hasEmote() && input.getKeycode() != 1) return;

        Screen wheel = (Screen) (Object) this;
        MinecraftClient.getInstance().setScreen(new EmoteSlotPickerScreen(wheel, emote -> {
            slot.setEmote(emote);
            if (Serializer.INSTANCE != null) Serializer.INSTANCE.saveConfig();
        }));
        cir.setReturnValue(true);
    }
}
