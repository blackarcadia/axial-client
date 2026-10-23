package com.axial.cosmetics.client;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.axial.axialutils.client.AxialUiTheme;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.*;

import static com.axial.cosmetics.client.ModMenuUi.*;
import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** One native, keyboard-accessible configure surface for all mod features. */
public final class FeatureSettingsScreen extends Screen {
    interface Preview { void draw(DrawContext context, Rect area); }
    sealed interface Setting permits Section, Toggle, Range, Choice, Color, Input, Action, Key { String label(); }
    record Section(String label) implements Setting {}
    record Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) implements Setting {}
    record Range(String label, DoubleSupplier get, DoubleConsumer set, SettingRange range, String suffix) implements Setting {}
    record Choice(String label, List<String> values, IntSupplier get, IntConsumer set, boolean crosshairs) implements Setting {}
    record Color(String label, IntSupplier get, IntConsumer set) implements Setting {}
    record Input(String label, Supplier<String> get, Consumer<String> set) implements Setting {}
    record Action(String label, Runnable run) implements Setting {}
    record Key(String label, Supplier<KeyBinding> binding) implements Setting {}
    record Model(String title, BooleanSupplier enabled, Runnable toggle, List<Setting> settings, Runnable reset, Preview preview) {}
    private record Placement(Setting setting, Rect area) {}
    private record Positioned(ClickableWidget widget, int contentY) {}

    private final Screen parent;
    private final Model model;
    private final List<Placement> placements = new ArrayList<>();
    private final List<Positioned> controls = new ArrayList<>();
    private FeatureSettingsLayout layout;
    private MenuAnimation animation = new MenuAnimation();
    private int scroll, contentHeight;
    private boolean draggingScrollbar, reopen;
    private Key capturing;
    private String error = "";

    FeatureSettingsScreen(Screen parent, Model model) {
        super(text(model.title()));
        this.parent=parent; this.model=model;
    }

    @Override protected void init() {
        if(reopen) { animation=new MenuAnimation(); reopen=false; }
        layout=new FeatureSettingsLayout(width,height,model.preview()!=null);
        rebuild();
    }

    private void rebuild() {
        clearChildren(); controls.clear(); placements.clear(); capturing=null;
        button(parent instanceof QuickMenuScreen?"BACK":"MODS",layout.back,this::close,false,null);
        button("CLOSE",layout.close,this::close,false,null);
        button("DONE",layout.done,this::close,false,null);
        if(model.reset()!=null) button("RESET DEFAULTS",layout.reset,()->change(()->{model.reset().run(); rebuild();}),false,null);
        if(model.enabled()!=null) button("ENABLED",layout.enabled,()->change(()->{model.toggle().run();rebuild();}),true,model.enabled());
        int y=0;
        for(int i=0;i<model.settings().size();i++) {
            Setting setting=model.settings().get(i);
            int rowHeight=setting instanceof Section ? 17 : setting instanceof Choice c ? choiceHeight(c) : 24;
            int w=layout.controls.width();
            if(setting instanceof Toggle && w>=300 && i+1<model.settings().size() && model.settings().get(i+1) instanceof Toggle) {
                place(setting,0,y,(w-6)/2,rowHeight);
                place(model.settings().get(++i),(w+6)/2,y,(w-6)/2,rowHeight);
            } else place(setting,0,y,w,rowHeight);
            y+=rowHeight;
        }
        contentHeight=y;
        scroll=Math.clamp(scroll,0,layout.maxScroll(contentHeight));
        for(Placement placement:placements) createControl(placement);
        positionControls();
    }

    private int choiceHeight(Choice c) {
        int columns=Math.max(1,Math.min(c.values().size(),layout.controls.width()/(c.crosshairs()?34:55)));
        return 15+((c.values().size()+columns-1)/columns)*23;
    }
    private void place(Setting s,int x,int y,int w,int h) { placements.add(new Placement(s,new Rect(layout.controls.x()+x,y,w,h))); }
    private Rect screenArea(Rect r) { return new Rect(r.x(),layout.controls.y()+r.y()-scroll,r.width(),r.height()); }
    private void track(ClickableWidget widget,int y) { controls.add(new Positioned(widget,y)); addDrawableChild(widget); }

    private void createControl(Placement p) {
        Setting s=p.setting(); Rect r=p.area();
        if(s instanceof Section)return;
        if(s instanceof Range range) { track(new SettingSlider(r.x(),0,r.width(),range),r.y()+1);return; }
        if(s instanceof Input input) {
            int labelWidth=Math.min(105,r.width()/3);
            var field=new TextFieldWidget(textRenderer,r.x()+labelWidth+5,0,r.width()-labelWidth-12,18,text(input.label()));
            field.setMaxLength(48); field.setDrawsBackground(false);field.setTextShadow(false);field.setEditableColor(INK);
            field.addFormatter((value,index)->text(value).asOrderedText());
            field.setText(input.get().get()==null?"":input.get().get());
            field.setChangedListener(value->change(()->input.set().accept(value)));
            track(field,r.y()+6);return;
        }
        if(s instanceof Choice choice) {
            int columns=Math.max(1,Math.min(choice.values().size(),r.width()/(choice.crosshairs()?34:55)));
            int cellWidth=(r.width()-(columns-1)*4)/columns;
            for(int i=0;i<choice.values().size();i++) {
                int value=i;
                var b=new SettingButton(choice.values().get(i),new Rect(r.x()+(i%columns)*(cellWidth+4),0,cellWidth,20),
                        ()->change(()->{choice.set().accept(value);rebuild();}),false,()->choice.get().getAsInt()==value);
                if(choice.crosshairs())b.crosshairStyle=value;
                b.setTooltip(Tooltip.of(text(choice.values().get(i))));
                track(b,r.y()+14+(i/columns)*23);
            }
            return;
        }
        if(s instanceof Toggle toggle) {
            track(new SettingButton(toggle.label(),new Rect(r.x(),0,r.width(),22),
                    ()->change(()->{toggle.set().accept(!toggle.get().getAsBoolean());rebuild();}),true,toggle.get()),r.y()+1);return;
        }
        track(new SettingButton(s.label(),new Rect(r.x(),0,r.width(),22),()->{
            if(s instanceof Color color) client.setScreen(new CrosshairColorPickerScreen(this,color.label(),color.get().getAsInt(),value->change(()->color.set().accept(value)),()->{}));
            if(s instanceof Action action)change(action.run());
            if(s instanceof Key key)capturing=key;
        },false,null,s),r.y()+1);
    }

    private SettingButton button(String label,Rect r,Runnable action,boolean toggle,BooleanSupplier state) {
        return addDrawableChild(new SettingButton(label,r,action,toggle,state));
    }
    private void positionControls() {
        for(Positioned p:controls) {
            p.widget().setY(layout.controls.y()+p.contentY()-scroll);
            p.widget().visible=layout.fullyVisible(new Rect(p.widget().getX(),p.widget().getY(),p.widget().getWidth(),p.widget().getHeight()));
            if(!p.widget().visible&&p.widget().isFocused())setFocused(null);
        }
    }
    private void change(Runnable action) {
        try { action.run(); error=""; }
        catch(RuntimeException ex) { error="COULD NOT SAVE. TRY AGAIN.";org.slf4j.LoggerFactory.getLogger(FeatureSettingsScreen.class).warn("Could not update {} settings",model.title(),ex); }
    }

    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta) {
        context.getMatrices().pushMatrix();context.getMatrices().translate(0,animation.offset());
        try { drawContent(context,mouseX,(int)(mouseY-animation.offset()),delta); }
        finally {context.getMatrices().popMatrix();}
    }
    private void drawContent(DrawContext c,int mx,int my,float delta) {
        frame(c,layout.panel,PANEL_BORDER,PANEL);
        int titleWidth=layout.panel.width()-(model.enabled()!=null?(layout.panel.width()<400?170:184):100);
        int titleCenter=layout.panel.x()+65+titleWidth/2;
        centered(c,"AXIAL CLIENT",titleCenter,layout.panel.y()+8,MUTED,.75f,titleWidth,false);
        centered(c,model.title(),titleCenter,layout.panel.y()+21,INK,1f,titleWidth,true);
        c.fill(layout.panel.x()+12,layout.controls.y()-5,layout.panel.right()-12,layout.controls.y()-4,DIVIDER);
        c.enableScissor(layout.controls.x(),layout.controls.y(),layout.controls.right(),layout.controls.bottom());
        for(Placement p:placements) {
            Rect r=screenArea(p.area());
            if(r.bottom()<=layout.controls.y()||r.y()>=layout.controls.bottom())continue;
            Setting s=p.setting();
            if(s instanceof Section) {
                label(c,textRenderer,s.label(),r.x()+1,r.y()+4,MUTED,.78f,r.width(),true);
            } else if(s instanceof Choice)label(c,textRenderer,s.label(),r.x()+1,r.y()+1,MUTED,.78f,r.width(),false);
            else if(s instanceof Input) {
                frame(c,new Rect(r.x(),r.y()+1,r.width(),22),LINE,FIELD);
                label(c,textRenderer,s.label(),r.x()+7,r.y()+8,MUTED,.78f,Math.min(100,r.width()/3)-5,false);
            }
        }
        // Only fully visible native widgets are interactive; clipping never leaves invisible hit targets.
        for(Positioned p:controls)if(p.widget().visible)p.widget().render(c,mx,my,delta);
        c.disableScissor();
        if(layout.preview!=null) {
            frame(c,layout.preview,LINE,SURFACE);
            label(c,textRenderer,"LIVE PREVIEW",layout.preview.x()+10,layout.preview.y()+9,MUTED,.8f,layout.preview.width()-20,false);
            Rect area=new Rect(layout.preview.x()+9,layout.preview.y()+26,layout.preview.width()-18,layout.preview.height()-35);
            c.enableScissor(area.x(),area.y(),area.right(),area.bottom());
            model.preview().draw(c,area);c.disableScissor();
        }
        for(var child:children())if(child instanceof SettingButton b&&!controls.stream().anyMatch(p->p.widget()==b))b.render(c,mx,my,delta);
        c.fill(layout.panel.x()+12,layout.panel.bottom()-32,layout.panel.right()-12,layout.panel.bottom()-31,DIVIDER);
        if(layout.panel.width()>=410) label(c,textRenderer,error.isEmpty()?"SAVES AUTOMATICALLY":error,layout.reset.right()+14,layout.done.y()+6,error.isEmpty()?MUTED:RED,.75f,layout.done.x()-layout.reset.right()-24,false);
        if(layout.maxScroll(contentHeight)>0) {
            Rect bar=layout.scrollbar;int thumb=Math.max(12,bar.height()*bar.height()/contentHeight);
            rounded(c,bar.x(),bar.y(),bar.width(),bar.height(),DIVIDER);
            rounded(c,bar.x(),bar.y()+(bar.height()-thumb)*scroll/layout.maxScroll(contentHeight),bar.width(),thumb,MUTED);
        }
    }

    @Override public void close() { if(capturing!=null){capturing=null;return;}animation.close(); }
    @Override public void removed() {reopen=true;}
    @Override public void tick() {if(animation.finished())client.setScreen(parent);}
    private Click transformed(Click click) {return new Click(click.x(),click.y()-animation.offset(),click.buttonInfo());}
    @Override public boolean mouseClicked(Click click,boolean doubled) {
        if(animation.closing())return true;
        click=transformed(click);
        if(capturing!=null){capturing=null;return true;}
        Rect bar=layout.scrollbar;
        if(click.button()==0&&layout.maxScroll(contentHeight)>0&&click.x()>=bar.x()-2&&click.x()<bar.right()+3&&click.y()>=bar.y()&&click.y()<bar.bottom()) {
            draggingScrollbar=true;dragScroll(click.y());return true;
        }
        return super.mouseClicked(click,doubled);
    }
    @Override public boolean mouseDragged(Click click,double dx,double dy) {
        if(draggingScrollbar){dragScroll(click.y()-animation.offset());return true;}
        return super.mouseDragged(transformed(click),dx,dy);
    }
    @Override public boolean mouseReleased(Click click) {draggingScrollbar=false;return super.mouseReleased(transformed(click));}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(animation.closing())return true;
        if(layout.controls.contains(x,y-animation.offset())) {scrollTo(scroll-(int)Math.round(vertical*26));return true;}
        return false;
    }
    private void scrollTo(int value) {scroll=Math.clamp(value,0,layout.maxScroll(contentHeight));positionControls();}
    private void centered(DrawContext c,String value,int center,int y,int color,float scale,int maxWidth,boolean bold) {
        String trimmed=textRenderer.trimToWidth(UiTypography.text(value,scale,bold),Math.max(0,maxWidth)).getString();
        label(c,textRenderer,trimmed,center-textRenderer.getWidth(UiTypography.text(trimmed,scale,bold))/2,y,color,scale,maxWidth,bold);
    }
    private void dragScroll(double y) {scrollTo((int)((y-layout.scrollbar.y())/layout.scrollbar.height()*layout.maxScroll(contentHeight)));}
    @Override public boolean keyPressed(KeyInput input) {
        if(animation.closing())return true;
        if(capturing!=null) {
            Key key=capturing;capturing=null;
            if(input.key()!=GLFW.GLFW_KEY_ESCAPE)change(()->{
                KeyBinding binding=key.binding().get();
                if(binding==null)throw new IllegalStateException("Key binding unavailable");
                binding.setBoundKey(InputUtil.fromKeyCode(input));KeyBinding.updateKeysByCode();client.options.write();
            });
            return true;
        }
        if(!(getFocused() instanceof TextFieldWidget)) {
            if(input.key()==GLFW.GLFW_KEY_PAGE_DOWN){scrollTo(scroll+layout.controls.height());return true;}
            if(input.key()==GLFW.GLFW_KEY_PAGE_UP){scrollTo(scroll-layout.controls.height());return true;}
            if(input.key()==GLFW.GLFW_KEY_HOME){scrollTo(0);return true;}
            if(input.key()==GLFW.GLFW_KEY_END){scrollTo(layout.maxScroll(contentHeight));return true;}
            if(QuickMenuScreen.isMenuKey(input)){close();return true;}
        }
        return super.keyPressed(input);
    }

    private final class SettingButton extends ButtonWidget {
        final boolean toggle;final BooleanSupplier state;final Setting setting;final String label;int crosshairStyle=-1;
        SettingButton(String label,Rect r,Runnable action,boolean toggle,BooleanSupplier state){this(label,r,action,toggle,state,null);}
        SettingButton(String label,Rect r,Runnable action,boolean toggle,BooleanSupplier state,Setting setting) {
            super(r.x(),r.y(),r.width(),r.height(),text(label),ignored->action.run(),DEFAULT_NARRATION_SUPPLIER);
            this.toggle=toggle;this.state=state;this.setting=setting;this.label=label;
        }
        @Override public void playDownSound(SoundManager soundManager){AxialUiTheme.playButtonClickSound();}
        @Override protected void drawIcon(DrawContext c,int mx,int my,float delta) {
            boolean hover=isHovered()||isFocused(),selected=state!=null&&state.getAsBoolean();
            Rect r=new Rect(getX(),getY(),width,height);
            boolean header=r.y()<layout.controls.y();
            if(getMessage().getString().equals("CLOSE")){icon(c,"close",getX()+4,getY()+5,11,11,hover?INK:MUTED);return;}
            frame(c,r,hover?HOVER_LINE:selected&&!toggle?SELECTED_LINE:LINE,selected&&!toggle?SELECTED:hover?HOVER:FIELD);
            if(toggle) {
                int color=selected?GREEN:RED,tx=r.right()-34;
                rounded(c,tx,r.y()+5,27,12,color);circle(c,tx+(selected?15:1),r.y()+6,10,INK);
                String shown=header?(selected?"ON":"OFF"):label;
                label(c,textRenderer,shown,r.x()+7,r.y()+7,INK,header?.78f:.8f,r.width()-45,false);
                setMessage(text((header?"ENABLED":label)+": "+(selected?"ON":"OFF")));return;
            }
            if(crosshairStyle>=0){ModFeaturePreviews.crosshairStyle(c,r,crosshairStyle);return;}
            if(setting instanceof Color color) {
                int swatch=color.get().getAsInt();
                rounded(c,r.right()-21,r.y()+4,14,14,swatch);
                label(c,textRenderer,String.format(Locale.ROOT,"#%06X",swatch&0xFFFFFF),r.right()-83,r.y()+7,MUTED,.8f,59,false);
                label(c,textRenderer,setting.label(),r.x()+7,r.y()+7,INK,.8f,r.width()-94,false);return;
            }
            if(setting instanceof Key key) {
                String value=capturing==key?"PRESS KEY...":key.binding().get()==null?"UNBOUND":key.binding().get().getBoundKeyLocalizedText().getString();
                label(c,textRenderer,key.label(),r.x()+7,r.y()+7,INK,.8f,r.width()/2,false);
                label(c,textRenderer,value,r.x()+r.width()/2,r.y()+7,capturing==key?GREEN:MUTED,.8f,r.width()/2-7,false);return;
            }
            if(setting==null)centered(c,getMessage().getString(),r.x()+r.width()/2,r.y()+6,INK,.8f,r.width()-14,false);
            else label(c,textRenderer,getMessage().getString(),r.x()+7,r.y()+6,label.startsWith("DELETE")?RED:INK,.8f,r.width()-14,false);
        }
    }
    private final class SettingSlider extends SliderWidget {
        final Range setting;
        SettingSlider(int x,int y,int w,Range setting) {
            super(x,y,w,22,text(setting.label()),setting.range().progress(setting.get().getAsDouble()));this.setting=setting;updateMessage();
        }
        @Override protected void updateMessage() {if(setting!=null)setMessage(text(setting.label()+": "+format()));}
        private String format(){return String.format(Locale.ROOT,setting.range().step()>=1?"%.0f%s":"%.2f%s",setting.get().getAsDouble(),setting.suffix());}
        @Override protected void applyValue(){change(()->setting.set().accept(setting.range().value(value)));updateMessage();}
        @Override public void playDownSound(SoundManager soundManager){AxialUiTheme.playButtonClickSound();}
        @Override public void onRelease(Click click) {} // SliderWidget's default release sound is the vanilla click.
        @Override public void renderWidget(DrawContext c,int mx,int my,float delta) {
            Rect r=new Rect(getX(),getY(),width,height);frame(c,r,isHovered()||isFocused()?HOVER_LINE:LINE,FIELD);
            int labelW=Math.min(80,width/4),valueW=48,trackX=r.x()+labelW+7,trackW=Math.max(8,width-labelW-valueW-18);
            int handle=trackX+(int)Math.round(setting.range().progress(setting.get().getAsDouble())*(trackW-8));
            label(c,textRenderer,setting.label(),r.x()+7,r.y()+7,INK,.8f,labelW-5,false);
            rounded(c,trackX,r.y()+10,trackW,3,DIVIDER);rounded(c,trackX,r.y()+10,Math.max(3,handle-trackX+4),3,MUTED);
            circle(c,handle,r.y()+7,8,INK);
            frame(c,new Rect(r.right()-valueW,r.y()+3,valueW-5,16),LINE,SURFACE);
            centered(c,format(),r.right()-valueW+(valueW-5)/2,r.y()+7,INK,.8f,valueW-9,false);
        }
        @Override public void onClick(Click click,boolean doubled){setFromPosition(click.x());}
        @Override protected void onDrag(Click click,double dx,double dy){setFromPosition(click.x());}
        private void setFromPosition(double x) {
            int labelW=Math.min(80,width/4),trackX=getX()+labelW+7,trackW=Math.max(8,width-labelW-48-18);
            setValue((x-trackX-4)/Math.max(1,trackW-8));
        }
    }
}
