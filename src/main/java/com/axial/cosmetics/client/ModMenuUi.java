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
    private ModMenuUi() {}
    static Text text(String value) { return Text.literal(value).styled(s -> s.withFont(FONT)); }
    static void icon(DrawContext context, String name, int x, int y, int w, int h, int color) {
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, Identifier.of("axial_cosmetics", "mods/"+name), x, y, w, h, color);
    }
    static void rounded(DrawContext context, int x, int y, int w, int h, int color) {
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, Identifier.of("axial_cosmetics", "accounts/rounded"), x, y, w, h, color);
    }
    static void circle(DrawContext context, int x, int y, int size, int color) {
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, Identifier.of("axial_cosmetics", "accounts/circle"), x, y, size, size, color);
    }
    static void frame(DrawContext context, ModMenuLayout.Rect r, int border, int fill) {
        rounded(context,r.x(),r.y(),r.width(),r.height(),border);
        rounded(context,r.x()+1,r.y()+1,r.width()-2,r.height()-2,fill);
    }
    static void label(DrawContext context, TextRenderer renderer, String value, int x, int y, int color, float scale, int maxWidth, boolean bold) {
        if (maxWidth <= 0) return;
        var font = bold ? BOLD : FONT;
        Text text = Text.literal(value).styled(s -> s.withFont(font));
        String trimmed = renderer.trimToWidth(text, (int)(maxWidth/scale)).getString();
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x,y);
        context.getMatrices().scale(scale,scale);
        context.drawText(renderer,Text.literal(trimmed).styled(s -> s.withFont(font)),0,0,color,false);
        context.getMatrices().popMatrix();
    }
}
