package com.axial.cosmetics.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;

/** Shared menu colours only: wheel textures, hit regions and emote actions stay intact. */
public final class EmoteMenuTheme {
    public static final int PANEL=ModMenuUi.PANEL,FIELD=ModMenuUi.FIELD,LINE=ModMenuUi.LINE;
    public static final int BORDER=ModMenuUi.PANEL_BORDER,BRAND=ModMenuUi.BRAND,INK=ModMenuUi.INK,MUTED=ModMenuUi.MUTED;
    public static final int HOVER=ModMenuUi.HOVER,HOVER_LINE=ModMenuUi.HOVER_LINE,SELECTED=ModMenuUi.SELECTED;
    public static final RenderPipeline WHEEL=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
            .withLocation(Identifier.of("axial_cosmetics","pipeline/emote_palette"))
            .withFragmentShader(Identifier.of("axial_cosmetics","core/emote_palette")).build());
    private static final StyleSpriteSource.Font UI_FONT=new StyleSpriteSource.Font(Identifier.of("axialutils","ui_clean"));
    private EmoteMenuTheme(){}
    public static void initialize(){} // Register the pipeline before the shader resource reload.
    public static void button(DrawContext context,ClickableWidget button,TextRenderer renderer){
        boolean hover=button.active&&(button.isHovered()||button.isFocused());
        ModMenuUi.frame(context,new ModMenuLayout.Rect(button.getX(),button.getY(),button.getWidth(),button.getHeight()),hover?HOVER_LINE:LINE,hover?HOVER:FIELD);
        Text text=Text.literal(button.getMessage().getString()).styled(style->style.withFont(UI_FONT));
        context.drawCenteredTextWithShadow(renderer,text,button.getX()+button.getWidth()/2,button.getY()+(button.getHeight()-renderer.fontHeight)/2,button.active?INK:MUTED);
    }
}
