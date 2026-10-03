package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.axial.axialutils.client.AxialUiTheme;

import java.util.Locale;

public final class CompassSettingsScreen extends Screen {
    private static final int WIDTH = 452, HEIGHT = 168;
    private static final StyleSpriteSource.Font FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));
    private final Screen parent;
    private ScaleSlider slider;
    private int x, y;
    public CompassSettingsScreen(Screen parent) { super(text("COMPASS")); this.parent = parent; }
    @Override protected void init() { layout(); slider = addDrawableChild(new ScaleSlider(x + 18, y + 84, 416)); }
    @Override public void render(DrawContext c, int mx, int my, float d) {
        layout(); slider.setPosition(x + 18, y + 84); c.fill(x,y,x+WIDTH,y+HEIGHT,0xE8101018); c.fill(x+1,y+1,x+WIDTH-1,y+2,0x44FFFFFF); c.drawStrokedRectangle(x,y,WIDTH,HEIGHT,0xD08F5DFF);
        c.drawCenteredTextWithShadow(textRenderer,title,x+WIDTH/2,y+10,0xFFF7F7FF); ModMenuBackButton.draw(c,x+18,y+6,mx,my);
        boolean enabled=CompassConfig.isEnabled(); AxialUiTheme.drawButton(c,textRenderer,x+18,y+30,416,20,"","",inside(mx,my,x+18,y+30,416,20),false,enabled?AxialUiTheme.TOGGLE_ON:AxialUiTheme.TOGGLE_OFF);
        c.drawCenteredTextWithShadow(textRenderer,text(enabled?"ENABLED":"DISABLED"),x+226,y+35,0xFFF7F7FF);
        Text label=text("SCALE"); c.drawTextWithShadow(textRenderer,label,x+20,y+66,0xFFC6D0F3); c.fill(x+18+textRenderer.getWidth(label)+12,y+71,x+WIDTH-28,y+72,0x998F5DFF);
        drawScaleSlider(c); slider.setAlpha(0.0f); super.render(c,mx,my,d);
    }
    @Override public boolean mouseClicked(Click click, boolean doubled) { if(click.button()==0&&inside(click.x(),click.y(),x+18,y+6,24,18)){close();return true;} if(click.button()==0&&inside(click.x(),click.y(),x+18,y+30,416,20)){CompassConfig.toggle();return true;} return super.mouseClicked(click,doubled); }
    @Override public void close(){MinecraftClient.getInstance().setScreen(parent);} private void layout(){x=(width-WIDTH)/2;y=Math.max(16,(height-HEIGHT)/2);} private static boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;} private static Text text(String s){return Text.literal(s).styled(st->st.withFont(FONT));}
    private void drawScaleSlider(DrawContext c) {
        int sx=x+18, sy=y+84, w=416, h=20; float progress=(CompassConfig.scale()-.5f)/2.5f; int handle=sx+Math.round(progress*(w-8));
        c.fill(sx,sy,sx+w,sy+h,0xF00E1018); c.fill(sx+1,sy+1,sx+w-1,sy+2,0x66FFFFFF);
        c.fill(sx+2,sy+h/2-2,sx+w-2,sy+h/2+2,0xCC2A2F3C); c.fill(sx+2,sy+h/2-2,handle+4,sy+h/2+2,0xFF8AF0C2);
        c.fill(handle,sy+2,handle+8,sy+h-2,0xFFE9D9FF); c.drawStrokedRectangle(handle,sy+2,8,h-4,0xFF8F5DFF); c.drawStrokedRectangle(sx,sy,w,h,0xFF8F5DFF);
        c.drawTextWithShadow(textRenderer,text("SCALE"),sx+2,sy-12,0xFFC6D0F3);
        c.drawTextWithShadow(textRenderer,text(String.format(Locale.ROOT,"SCALE: %.2fx",CompassConfig.scale())),sx+w-78,sy-12,0xFFFFFFFF);
    }
    private final class ScaleSlider extends SliderWidget { ScaleSlider(int x,int y,int w){super(x,y,w,20,Text.empty(),(CompassConfig.scale()-.5f)/2.5f);} @Override protected void updateMessage(){setMessage(Text.empty());} @Override protected void applyValue(){CompassConfig.setScale(.5f+(float)value*2.5f);} }
}
