package com.axial.cosmetics.mixin;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(PressableWidget.class)
public abstract class InGameButtonSkinMixin {
    private static final Identifier BUTTON_TEXTURE = AxialCosmetics.id("textures/gui/buttons/packbutton2.png");
    private static final int BUTTON_TEXTURE_WIDTH = 200;
    private static final int BUTTON_TEXTURE_HEIGHT = 20;
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$renderInGameButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen currentScreen = client.currentScreen;
        if (currentScreen == null || currentScreen instanceof TitleScreen) {
            return;
        }

        Object self = this;
        if (!(self instanceof ClickableWidget button)) {
            return;
        }

        String lower = button.getMessage().getString().toLowerCase(Locale.ROOT);
        String buttonClass = button.getClass().getName().toLowerCase(Locale.ROOT);
        if (lower.contains("difficulty lock") || lower.contains("knowledge book") || lower.contains("recipe book") || buttonClass.contains("recipebook")) {
            return;
        }

        int renderedWidth = Math.max(button.getWidth(), 1);
        int renderedHeight = Math.max(button.getHeight(), 1);
        int x = button.getX();
        int y = button.getY();
        boolean hovered = button.isHovered() || button.isFocused();

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                BUTTON_TEXTURE,
                x,
                y,
                0.0f,
                0.0f,
                renderedWidth,
                renderedHeight,
                BUTTON_TEXTURE_WIDTH,
                BUTTON_TEXTURE_HEIGHT,
                BUTTON_TEXTURE_WIDTH,
                BUTTON_TEXTURE_HEIGHT
        );
        if (hovered) {
            context.fill(x, y, x + renderedWidth, y + renderedHeight, 0x22000000);
        }

        TextRenderer textRenderer = client.textRenderer;
        int textColor = ColorHelper.getWhite(button.getAlpha());
        int textY = y + (renderedHeight - 8) / 2;
        Text buttonText = Text.literal(button.getMessage().getString()).styled(style -> style.withFont(UI_FONT));
        context.drawCenteredTextWithShadow(textRenderer, buttonText, x + renderedWidth / 2, textY, textColor);
        ci.cancel();
    }
}
