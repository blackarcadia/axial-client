package com.axial.cosmetics.mixin;

import io.github.kosmx.emotes.arch.screen.EmoteMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;
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
                    () -> client.setScreen(new EmoteMenu(this))));
            return;
        }
    }

    /** Matches the compact Chunk Borders key-input style instead of the shared in-game texture. */
    private static final class ConfigureEmotesButton extends ButtonWidget {
        private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

        private ConfigureEmotesButton(int x, int y, int width, int height, net.minecraft.text.Text label, Runnable action) {
            super(x, y, width, height, label, ignored -> action.run(), DEFAULT_NARRATION_SUPPLIER);
        }

        @Override
        protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
            int x = getX();
            int y = getY();
            boolean hovered = active && (isHovered() || isFocused());
            context.fill(x, y, x + width, y + height, hovered ? 0xFF20202D : 0xFF11111B);
            context.fill(x + 1, y + 1, x + width - 1, y + 2, hovered ? 0x44FFFFFF : 0x20FFFFFF);
            context.drawStrokedRectangle(x, y, width, height, hovered ? 0xFFFFFFFF : 0xFFE7D9FF);

            var renderer = MinecraftClient.getInstance().textRenderer;
            net.minecraft.text.Text label = net.minecraft.text.Text.literal(getMessage().getString())
                    .styled(style -> style.withFont(UI_FONT));
            context.drawCenteredTextWithShadow(renderer, label, x + width / 2,
                    y + (height - renderer.fontHeight) / 2, active ? 0xFFF7F7FF : 0xFF807B89);
        }
    }
}
