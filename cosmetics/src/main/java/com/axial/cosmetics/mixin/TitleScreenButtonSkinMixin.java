package com.axial.cosmetics.mixin;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(PressableWidget.class)
public abstract class TitleScreenButtonSkinMixin {
    private static final Identifier BUTTON_1 = AxialCosmetics.id("textures/gui/title/button1.png");
    private static final Identifier BUTTON_2 = AxialCosmetics.id("textures/gui/title/button2.png");
    private static final Identifier QUIT_ICON = AxialCosmetics.id("textures/gui/title/quit-button-icon.png");
    private static final Identifier QUIT_ICON_HOVER = AxialCosmetics.id("textures/gui/title/quit-button-icon-hover.png");
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));
    private static final int BUTTON_TEXTURE_WIDTH = 208;
    private static final int BUTTON_TEXTURE_HEIGHT = 22;
    private static final int BUTTON_HOVER_EXPAND = 12;
    private static final int RIGHT_MARGIN = 20;
    private static final int QUIT_ICON_SIZE = 18;

    @Unique
    private float axial_cosmetics$hoverProgress;

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$renderTitleButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof TitleScreen titleScreen)) {
            return;
        }

        Object self = this;
        if (!(self instanceof ButtonWidget button)) {
            return;
        }

        String lower = button.getMessage().getString().toLowerCase(Locale.ROOT);
        boolean isTitleButton = lower.contains("single") || lower.contains("multi") || lower.contains("options") || lower.contains("quit");
        if (!isTitleButton) {
            return;
        }

        if (lower.contains("quit")) {
            boolean highlighted = button.isHovered() || button.isFocused();
            int size = button.getWidth();
            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    highlighted ? QUIT_ICON_HOVER : QUIT_ICON,
                    button.getX(),
                    button.getY(),
                    0.0f,
                    0.0f,
                    size,
                    size,
                    1024,
                    1024,
                    1024,
                    1024
            );
            ci.cancel();
            return;
        }

        boolean highlighted = button.isHovered() || button.isFocused();
        axial_cosmetics$hoverProgress += ((highlighted ? 1.0f : 0.0f) - axial_cosmetics$hoverProgress) * 0.34f;

        int renderedWidth = BUTTON_TEXTURE_WIDTH + Math.round(axial_cosmetics$hoverProgress * BUTTON_HOVER_EXPAND);
        button.setWidth(renderedWidth);

        int x = lower.contains("quit")
                ? Math.max(RIGHT_MARGIN, titleScreen.width - RIGHT_MARGIN - renderedWidth)
                : button.getX();
        button.setX(x);

        Identifier texture = highlighted ? BUTTON_2 : BUTTON_1;
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x,
                button.getY(),
                0.0f,
                0.0f,
                renderedWidth,
                BUTTON_TEXTURE_HEIGHT,
                BUTTON_TEXTURE_WIDTH,
                BUTTON_TEXTURE_HEIGHT,
                BUTTON_TEXTURE_WIDTH,
                BUTTON_TEXTURE_HEIGHT
        );

        TextRenderer textRenderer = client.textRenderer;
        int textColor = ColorHelper.getWhite(button.getAlpha());
        int textY = button.getY() + (BUTTON_TEXTURE_HEIGHT - 8) / 2;
        Text buttonText = Text.literal(button.getMessage().getString().toUpperCase(Locale.ROOT))
                .styled(style -> style.withFont(UI_FONT));
        int textX = x + 12;
        context.drawTextWithShadow(textRenderer, buttonText, textX, textY, textColor);
        ci.cancel();
    }
}
