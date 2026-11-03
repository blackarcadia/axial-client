package com.axial.cosmetics.client;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.SoundManager;
import org.axial.axialutils.client.AxialUiTheme;
import java.awt.Color;
import java.util.Locale;
import java.util.function.IntConsumer;
import static com.axial.cosmetics.client.ModMenuUi.*;
import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** Shared responsive shade/hue editor with native keyboard controls and hex input. */
public final class CrosshairColorPickerScreen extends Screen {
    private final Screen parent;
    private final IntConsumer onChange;
    private final ColorPickerSaveAction saveAction;
    private final int resetColor;
    private Rect panel,shade,hueBar;
    private TextFieldWidget hex;
    private float hue,saturation,brightness;
    private int dragging=-1;
    private boolean syncing,chroma;
    private PickerButton chromaButton;
    private ColorPickerLayout layout;

    public CrosshairColorPickerScreen(Screen parent,String label,int initialColor,IntConsumer onChange) {
        this(parent,label,initialColor,onChange,CrosshairConfigManager::save);
    }
    public CrosshairColorPickerScreen(Screen parent,String label,int initialColor,IntConsumer onChange,ColorPickerSaveAction saveAction) {
        super(text(label.toUpperCase(Locale.ROOT).endsWith("COLOR")?label:label+" COLOR"));this.parent=parent;this.onChange=onChange;this.saveAction=saveAction;
        resetColor=ChromaColor.solid(HudColorDefaults.forPickerLabel(label,initialColor));setColor(initialColor);
    }
    @Override protected void init() {
        layout=new ColorPickerLayout(width,height);panel=layout.panel;shade=layout.shade;hueBar=layout.hue;
        int w=panel.width();
        addButton("BACK",layout.back,this::close);
        addButton("RESET",layout.reset,()->{setColor(resetColor);push();});
        addButton("DONE",layout.done,this::close);
        chromaButton=addButton("CHROMA",layout.chroma,()->{chroma=!chroma;push();});
        hex=addDrawableChild(new TextFieldWidget(textRenderer,panel.x()+62,shade.bottom()+14,w-79,14,text("HEX COLOR")));
        hex.setMaxLength(7);hex.setDrawsBackground(false);hex.setTextShadow(false);hex.setEditableColor(INK);
        hex.addFormatter((value,index)->text(value).asOrderedText());syncHex();
        hex.setChangedListener(value->{
            if(syncing)return;
            String digits=value.startsWith("#")?value.substring(1):value;
            if(!digits.matches("[0-9a-fA-F]{6}"))return;
            setColor(0xFF000000|Integer.parseInt(digits,16));onChange.accept(storedColor());saveAction.save();
        });
    }
    @Override public void close(){saveAction.save();client.setScreen(parent);}
    @Override public void render(DrawContext c,int mx,int my,float delta) {
        frame(c,panel,PANEL_BORDER,PANEL);
        MenuHeader.draw(c,textRenderer,new Rect(panel.x()+62,panel.y()+3,panel.width()-76,30),title.getString());
        for(int col=0;col<shade.width();col++) {
            int color=Color.HSBtoRGB(hue,col/(float)Math.max(1,shade.width()-1),1);
            c.fillGradient(shade.x()+col,shade.y(),shade.x()+col+1,shade.bottom(),color,0xFF000000);
        }
        c.drawStrokedRectangle(shade.x()-1,shade.y()-1,shade.width()+2,shade.height()+2,LINE);
        for(int row=0;row<hueBar.height();row++)c.fill(hueBar.x(),hueBar.y()+row,hueBar.right(),hueBar.y()+row+1,Color.HSBtoRGB(row/(float)Math.max(1,hueBar.height()-1),1,1));
        int sx=shade.x()+Math.round(saturation*(shade.width()-1)),sy=shade.y()+Math.round((1-brightness)*(shade.height()-1));
        circle(c,sx-4,sy-4,8,0xFF000000);circle(c,sx-3,sy-3,6,INK);circle(c,sx-2,sy-2,4,currentColor());
        int hy=hueBar.y()+Math.round(hue*(hueBar.height()-1));
        c.drawStrokedRectangle(hueBar.x()-2,hy-2,hueBar.width()+4,5,0xFF000000);
        c.drawStrokedRectangle(hueBar.x()-1,hy-1,hueBar.width()+2,3,INK);
        frame(c,new Rect(panel.x()+14,shade.bottom()+9,panel.width()-28,22),hex.isFocused()?FOCUS:LINE,FIELD);
        rounded(c,panel.x()+19,shade.bottom()+13,28,14,currentColor());
        chromaButton.setMessage(text("CHROMA: "+(chroma?"ON":"OFF")));
        super.render(c,mx,my,delta);
        if(chroma)rounded(c,layout.chroma.right()-24,layout.chroma.y()+5,12,12,currentColor());
    }
    @Override public boolean mouseClicked(Click click,boolean doubled) {
        if(super.mouseClicked(click,doubled))return true;
        if(click.button()!=0)return false;
        if(shade.contains(click.x(),click.y()))dragging=0;
        else if(hueBar.contains(click.x(),click.y()))dragging=1;
        else return false;
        AxialUiTheme.playButtonClickSound();update(click.x(),click.y());return true;
    }
    @Override public boolean mouseDragged(Click click,double dx,double dy) {
        if(dragging<0)return super.mouseDragged(click,dx,dy);
        update(click.x(),click.y());return true;
    }
    @Override public boolean mouseReleased(Click click){dragging=-1;return super.mouseReleased(click);}
    private void update(double x,double y) {
        chroma=false;
        float vertical=(float)Math.clamp((y-shade.y())/Math.max(1,shade.height()-1),0,1);
        if(dragging==0){saturation=(float)Math.clamp((x-shade.x())/Math.max(1,shade.width()-1),0,1);brightness=1-vertical;}
        else hue=vertical;
        push();
    }
    private void setColor(int color){chroma=ChromaColor.enabled(color);float[] hsb=Color.RGBtoHSB((color>>16)&255,(color>>8)&255,color&255,null);hue=hsb[0];saturation=hsb[1];brightness=hsb[2];}
    private int storedColor(){return ChromaColor.withChroma(Color.HSBtoRGB(hue,saturation,brightness),chroma);}
    private int currentColor(){return ChromaColor.resolve(storedColor());}
    private void push(){onChange.accept(storedColor());saveAction.save();syncHex();}
    private void syncHex(){if(hex==null)return;syncing=true;try{hex.setText(String.format(Locale.ROOT,"#%06X",storedColor()&0xFFFFFF));}finally{syncing=false;}}
    private PickerButton addButton(String label,Rect r,Runnable action){return addDrawableChild(new PickerButton(label,r,action));}
    private final class PickerButton extends AnimatedMenuButton {
        PickerButton(String label,Rect r,Runnable action){super(r.x(),r.y(),r.width(),r.height(),text(label),ignored->action.run(),DEFAULT_NARRATION_SUPPLIER);}
        @Override public void playDownSound(SoundManager soundManager){AxialUiTheme.playButtonClickSound();}
        @Override protected void drawContents(DrawContext c,int mx,int my,float delta){
            boolean hover=isHovered()||isFocused();frame(c,new Rect(getX(),getY(),width,height),hover?HOVER_LINE:LINE,hover?HOVER:FIELD);
            label(c,textRenderer,getMessage().getString(),getX()+7,getY()+6,INK,.8f,width-14,false);
        }
    }
}
