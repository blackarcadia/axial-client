package com.axial.cosmetics.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import net.minecraft.text.StyleSpriteSource;
import org.axial.axialutils.client.AxialUiTheme;
import static com.axial.cosmetics.client.ModMenuUi.*;
import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** Shared charcoal controls and purple branding for navigation screens. */
abstract class NavigationScreen extends Screen {
    protected final Screen parent;
    protected String error="";
    NavigationScreen(String title,Screen parent){super(copy(title,1,true));this.parent=parent;}
    static Text copy(String value,float size,boolean bold){return Text.literal(value).styled(s->s.withFont(new StyleSpriteSource.Font(UiTypography.font(size,bold))));}
    static void line(DrawContext c,TextRenderer font,String value,int x,int y,int color,float size,int width,boolean bold) {
        if(width<=0)return;
        String trimmed=font.trimToWidth(copy(value,size,bold),width).getString();c.drawText(font,copy(trimmed,size,bold),x,y,color,false);
    }
    protected void line(DrawContext c,String value,int x,int y,int color,float size,int width,boolean bold){line(c,textRenderer,value,x,y,color,size,width,bold);}
    protected NavButton button(String label,Rect r,Runnable action){return addDrawableChild(new NavButton(label,r,action));}
    protected TextFieldWidget field(String title,String value,Rect r,int length) {
        var field=new TextFieldWidget(textRenderer,r.x()+7,r.y()+7,r.width()-14,12,copy(title,1,false));
        field.setMaxLength(length);field.setText(value);field.setDrawsBackground(false);field.setTextShadow(false);
        field.setEditableColor(INK);field.setPlaceholder(copy(title,.85f,false));field.addFormatter((v,i)->copy(v,.85f,false).asOrderedText());
        return addDrawableChild(field);
    }
    protected void inputFrame(DrawContext c,TextFieldWidget field,Rect r){frame(c,r,field.isFocused()?BRAND:LINE,FIELD);}
    protected void attempt(Runnable action){try{action.run();error="";}catch(RuntimeException ex){error=ex.getMessage()==null?"Could not save. Try again.":ex.getMessage();org.slf4j.LoggerFactory.getLogger(getClass()).warn("Navigation action failed",ex);}}
    protected void heading(DrawContext c,Rect panel,String title,String subtitle) {
        frame(c,panel,PANEL_BORDER,PANEL);
        line(c,"AXIAL CLIENT",panel.x()+16,panel.y()+10,BRAND,.8f,panel.width()-55,true);
        line(c,title,panel.x()+16,panel.y()+24,INK,1.35f,panel.width()-55,true);
        if(!subtitle.isEmpty())line(c,subtitle,panel.x()+16,panel.y()+43,MUTED,.8f,panel.width()-32,false);
    }
    @Override public void close(){client.setScreen(parent);}
    protected class NavButton extends ButtonWidget {
        String label;
        boolean selected,primary,danger;
        NavButton(String label,Rect r,Runnable action){super(r.x(),r.y(),r.width(),r.height(),copy(label,.85f,false),b->attempt(action),DEFAULT_NARRATION_SUPPLIER);this.label=label;}
        void label(String value){label=value;setMessage(copy(value,.85f,false));}
        @Override public void playDownSound(SoundManager sounds){AxialUiTheme.playButtonClickSound();}
        @Override protected void drawIcon(DrawContext c,int mx,int my,float dt) {
            boolean hover=active&&(isHovered()||isFocused());
            frame(c,new Rect(getX(),getY(),width,height),selected?BRAND:hover?HOVER_LINE:LINE,
                    !active?SURFACE:hover?HOVER:primary?SELECTED:FIELD);
            int color=!active?MUTED:danger?RED:selected?BRAND:INK;
            int w=textRenderer.getWidth(copy(label,.85f,false));
            line(c,label,getX()+Math.max(5,(width-w)/2),getY()+(height-8)/2,color,.85f,width-10,false);
        }
    }
}
