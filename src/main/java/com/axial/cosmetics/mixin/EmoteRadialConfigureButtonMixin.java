package com.axial.cosmetics.mixin;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

/** Emotes are managed in the wardrobe; the quick wheel needs no library button. */
@Mixin(targets="io.github.kosmx.emotes.arch.screen.ingame.FastMenuScreen",remap=false)
public abstract class EmoteRadialConfigureButtonMixin extends Screen {
    protected EmoteRadialConfigureButtonMixin(){super(Text.empty());}

    @Inject(method={"method_25426","init"},at=@At("TAIL"),remap=false)
    private void axial_cosmetics$removeAllEmotesButton(CallbackInfo ci) {
        for(Element element:List.copyOf(children()))if(element instanceof ButtonWidget button) {
            boolean library=button.getMessage().getContent() instanceof TranslatableTextContent text&&text.getKey().equals("emotecraft.emotelist");
            if(library||"All Emotes".equalsIgnoreCase(button.getMessage().getString()))remove(button);
        }
    }
}
