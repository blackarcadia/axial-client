package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.AxialEmoteLoadoutScreen;
import com.axial.cosmetics.client.EmoteMenuTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Routes the quick wheel's All Emotes button to Emotecraft's emote configuration screen. */
@Mixin(targets = "io.github.kosmx.emotes.arch.screen.ingame.FastMenuScreen", remap = false)
public abstract class EmoteRadialConfigureButtonMixin extends Screen {
    protected EmoteRadialConfigureButtonMixin() {
        super(Text.empty());
    }

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void axial_cosmetics$openEmoteConfiguration(CallbackInfo ci) {
        for (Element element : List.copyOf(children())) {
            if (!(element instanceof ButtonWidget button)
                    || !"All Emotes".equalsIgnoreCase(button.getMessage().getString())) {
                continue;
            }

            int x = button.getX();
            int y = button.getY();
            int width = button.getWidth();
            int height = button.getHeight();
            Text label = button.getMessage();
            remove(button);
            addDrawableChild(new ConfigureEmotesButton(x, y, width, height, label,
                    () -> client.setScreen(new AxialEmoteLoadoutScreen(this))));
            return;
        }
    }

    /** Matches the compact Chunk Borders key-input style instead of the shared in-game texture. */
    private static final class ConfigureEmotesButton extends ButtonWidget {
        private ConfigureEmotesButton(int x, int y, int width, int height, net.minecraft.text.Text label, Runnable action) {
            super(x, y, width, height, label, ignored -> action.run(), DEFAULT_NARRATION_SUPPLIER);
        }

        @Override
        protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
            EmoteMenuTheme.button(context,this,MinecraftClient.getInstance().textRenderer);
        }
    }
}
