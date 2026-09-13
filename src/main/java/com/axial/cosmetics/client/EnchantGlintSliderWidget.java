package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.Locale;

public final class EnchantGlintSliderWidget extends SliderWidget {
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    public EnchantGlintSliderWidget() {
        super(0, 0, 196, 20, Text.empty(),
                (EnchantGlintConfig.strength() - 1.0) / (EnchantGlintConfig.MAX_STRENGTH - 1.0));
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Text.literal("STRENGTH: " + (value == 0.0 ? "DEFAULT"
                : String.format(Locale.ROOT, "%.2fx", 1.0 + value * (EnchantGlintConfig.MAX_STRENGTH - 1.0))))
                .styled(style -> style.withFont(UI_FONT)));
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        int handleX = x + Math.round((float) value * (width - 8));
        context.fill(x, y, x + width, y + height, 0xF00E1018);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, 0x66FFFFFF);
        context.fill(x + 2, y + height / 2 - 2, x + width - 2, y + height / 2 + 2, 0xCC2A2F3C);
        context.fill(x + 2, y + height / 2 - 2, handleX + 4, y + height / 2 + 2, 0xFF8AF0C2);
        context.fill(handleX, y + 2, handleX + 8, y + height - 2, 0xFFE9D9FF);
        context.drawStrokedRectangle(handleX, y + 2, 8, height - 4, 0xFF8F5DFF);
        context.drawStrokedRectangle(x, y, width, height, 0xFF8F5DFF);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(),
                x + width / 2, y + 6, 0xFFFFFFFF);
    }

    @Override
    protected void applyValue() {
        EnchantGlintConfig.setStrength((float) (1.0 + value * (EnchantGlintConfig.MAX_STRENGTH - 1.0)));
    }

}
