package com.axial.cosmetics.client;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import java.util.Locale;

public final class EnchantGlintSliderWidget extends SliderWidget {
    public EnchantGlintSliderWidget() {
        super(0, 0, 392, 20, Text.empty(),
                (EnchantGlintConfig.strength() - 1.0) / (EnchantGlintConfig.MAX_STRENGTH - 1.0));
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Text.literal("ENCHANT GLINT MODIFIER: " + (value == 0.0 ? "DEFAULT"
                : String.format(Locale.ROOT, "%.2f×", 1.0 + value * (EnchantGlintConfig.MAX_STRENGTH - 1.0)))));
    }

    @Override
    protected void applyValue() {
        EnchantGlintConfig.setStrength((float) (1.0 + value * (EnchantGlintConfig.MAX_STRENGTH - 1.0)));
    }

}
