package com.axial.cosmetics.client;

import net.minecraft.util.Formatting;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/** Local sidebar visibility, including servers that explicitly send blank number formats. */
public final class ScoreboardNumbers {
    private ScoreboardNumbers() { }

    public static MutableText resolve(MutableText formatted, int score, boolean show) {
        if (!show) return Text.empty();
        return formatted.getString().isBlank() ? Text.literal(Integer.toString(score)).formatted(Formatting.RED) : formatted;
    }
}
