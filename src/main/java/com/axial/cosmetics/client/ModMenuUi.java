package com.axial.cosmetics.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

final class ModMenuUi {
    static final int INK = 0xFFF0F2F3, MUTED = 0xFFA0A7AE, LINE = 0xFF454C53;
    static final int GREEN = 0xFF46D57D, RED = 0xFFF0646C;
    private static final StyleSpriteSource.Font FONT = new StyleSpriteSource.Font(Identifier.of("axial_cosmetics", "accounts"));
    private static final StyleSpriteSource.Font BOLD = new StyleSpriteSource.Font(Identifier.of("axial_cosmetics", "accounts_bold"));
    private static float opacity=1;
    private ModMenuUi() {}
    static float opacity(float value) { float previous=opacity;opacity=value;return previous; }
    static int tint(int color) { return (color&0xFFFFFF)|Math.round((color>>>24)*opacity)<<24; }
    static int mixColor(int a,int b,float t) {
        int red=Math.round(((a>>16)&255)*(1-t)+((b>>16)&255)*t);
        int green=Math.round(((a>>8)&255)*(1-t)+((b>>8)&255)*t);
        int blue=Math.round((a&255)*(1-t)+(b&255)*t);
        return 0xFF000000|(red<<16)|(green<<8)|blue;
    }
    static Text text(String value) { return Text.literal(value).styled(s -> s.withFont(FONT)); }
    static void icon(DrawContext context, String name, int x, int y, int w, int h, int color) {
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, Identifier.of("axial_cosmetics", "mods/"+name), x, y, w, h, tint(color));
    }
    static void rounded(DrawContext context, int x, int y, int w, int h, int color) {
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, Identifier.of("axial_cosmetics", "accounts/rounded"), x, y, w, h, tint(color));
    }
    static void circle(DrawContext context, int x, int y, int size, int color) {
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, Identifier.of("axial_cosmetics", "accounts/circle"), x, y, size, size, tint(color));
    }
    static void frame(DrawContext context, ModMenuLayout.Rect r, int border, int fill) {
        rounded(context,r.x(),r.y(),r.width(),r.height(),border);
        rounded(context,r.x()+1,r.y()+1,r.width()-2,r.height()-2,fill);
    }
    static void label(DrawContext context, TextRenderer renderer, String value, int x, int y, int color, float scale, int maxWidth, boolean bold) {
        if (maxWidth <= 0 || opacity < .01f) return;
        var font = bold ? BOLD : FONT;
        Text text = Text.literal(value).styled(s -> s.withFont(font));
        String trimmed = renderer.trimToWidth(text, (int)(maxWidth/scale)).getString();
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x,y);
        context.getMatrices().scale(scale,scale);
        context.drawText(renderer,Text.literal(trimmed).styled(s -> s.withFont(font)),0,0,tint(color),false);
        context.getMatrices().popMatrix();
    }
}
