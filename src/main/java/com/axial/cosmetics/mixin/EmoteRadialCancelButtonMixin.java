package com.axial.cosmetics.mixin;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Element;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Removes the redundant Cancel button from the hold-to-open quick wheel. */
@Mixin(targets = "io.github.kosmx.emotes.arch.screen.ingame.FastMenuScreen", remap = false)
public abstract class EmoteRadialCancelButtonMixin extends Screen {
    protected EmoteRadialCancelButtonMixin() {
        super(Text.empty());
    }

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void axial_cosmetics$removeCancelButton(CallbackInfo ci) {
        for (Element element : List.copyOf(children())) {
            if (element instanceof ButtonWidget button
                    && "Cancel".equalsIgnoreCase(button.getMessage().getString())) {
                remove(element);
            }
        }
    }
}
