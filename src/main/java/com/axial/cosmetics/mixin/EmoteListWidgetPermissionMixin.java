package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.EmotePermissionState;
import com.axial.cosmetics.client.AxialEmoteLoadoutScreen;
import io.github.kosmx.emotes.main.EmoteHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Removes emotes not granted by the connected Axial server before the menu is built. */
@Mixin(targets = "io.github.kosmx.emotes.arch.gui.widgets.EmoteListWidget", remap = false)
public abstract class EmoteListWidgetPermissionMixin {
    /** Makes the library entries span their responsive Axial pane. */
    @Inject(method = "getRowWidth", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$useLoadoutPaneWidth(CallbackInfoReturnable<Integer> cir) {
        if (MinecraftClient.getInstance().currentScreen instanceof AxialEmoteLoadoutScreen) {
            cir.setReturnValue(Math.max(1, ((ClickableWidget) (Object) this).getWidth() - 12));
        }
    }

    @ModifyVariable(method = "setEmotes", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private Iterable<EmoteHolder> axial_cosmetics$filterUnavailableEmotes(Iterable<EmoteHolder> emotes) {
        return EmotePermissionState.filter(emotes);
    }
}
