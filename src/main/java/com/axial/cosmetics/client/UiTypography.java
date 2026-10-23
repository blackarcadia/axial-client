package com.axial.cosmetics.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.Locale;

/** Brand casing and native font sizes; glyphs are never shrunk with a GUI matrix. */
final class UiTypography {
    private UiTypography() {}

    static String uppercase(String value) { return value.toUpperCase(Locale.ROOT); }

    static int size(float scale) {
        int requested=Math.round(9*scale);
        if(requested<=7)return 7;
        if(requested<=8)return 8;
        if(requested<=9)return 9;
        if(requested<=11)return 11;
        if(requested<=12)return 12;
        return 16;
    }

    static Identifier font(float scale,boolean bold) {
        int size=size(scale);
        String path=size==9?(bold?"accounts_bold":"accounts"):"ui_"+size+(bold?"_bold":"");
        return Identifier.of("axial_cosmetics",path);
    }

    static Text text(String value,float scale,boolean bold) {
        var face=new StyleSpriteSource.Font(font(scale,bold));
        return Text.literal(uppercase(value)).styled(style->style.withFont(face));
    }

    static int width(TextRenderer renderer,String value,float scale,boolean bold) {
        return renderer.getWidth(text(value,scale,bold));
    }
}
