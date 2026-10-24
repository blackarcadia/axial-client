package com.axial.cosmetics.client;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.SoundManager;
import org.axial.axialutils.client.AxialUiTheme;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.IntConsumer;
import static com.axial.cosmetics.client.ModMenuUi.*;
import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** Visual preset library and precise editor, both using the gameplay renderer. */
public final class CrosshairSettingsScreen extends NavigationScreen {
    private record Positioned(ClickableWidget widget,int y){}
    private record Caption(String label,int y){}
    private final List<Positioned> widgets=new ArrayList<>();
    private final List<Caption> captions=new ArrayList<>();
    private final List<DimensionSlider> sliders=new ArrayList<>();
    private final CrosshairHistory history=new CrosshairHistory();
    private final int[] scrolls=new int[2];
    private CrosshairEditorLayout layout;
    private int tab,contentHeight,background,zoom=1;
    private boolean draggingScrollbar,syncing;
    private long pulseStart=System.nanoTime();
    private NavButton undoButton,enabledButton;

    public CrosshairSettingsScreen(net.minecraft.client.gui.screen.Screen parent){super("Crosshair",parent);}
    private CrosshairConfig config(){return CrosshairConfigManager.get();}
    @Override protected void init(){layout=new CrosshairEditorLayout(width,height);rebuild();}
    private void rebuild(){
        clearChildren();widgets.clear();captions.clear();sliders.clear();
        button("Back",layout.back,this::close);
        button("Close",layout.close,this::close);
        button("Done",layout.done,this::close).primary=true;
        button("Reset",layout.reset,()->edit(()->config().copyFrom(new CrosshairConfig())));
        undoButton=button("Undo",layout.undo,()->{history.undo(config());save();rebuild();});
        enabledButton=button("Enabled",layout.enabled,()->edit(()->config().enabled=!config().enabled));enabledButton.toggle=true;
        button("Templates",layout.presets,()->switchTab(0)).selected=tab==0;
        button("Customize",layout.customize,()->switchTab(1)).selected=tab==1;
        if(tab==0)buildGallery();else buildCustomization();
        buildPreviewButtons();position();refreshHeader();
    }
    private void switchTab(int value){tab=value;rebuild();}
    private void save(){CrosshairConfigManager.save();}
    private void edit(Runnable action){history.checkpoint(config());action.run();save();rebuild();}
    private void track(ClickableWidget widget,int y){widgets.add(new Positioned(widget,y));addDrawableChild(widget);}
    private int caption(String label,int y){captions.add(new Caption(label,y));return y+18;}
    private void buildGallery(){
        int columns=layout.columns(),cell=(layout.controls.width()-(columns-1)*6)/columns;
        int cardHeight=Math.min(56,layout.controls.height()),stride=cardHeight+6;
        var templates=CrosshairPresets.templates();
        for(int i=0;i<templates.size();i++){
            var preset=templates.get(i);int y=i/columns*stride;
            var b=new PresetButton(preset,new Rect(layout.controls.x()+i%columns*(cell+6),0,cell,cardHeight));
            b.setTooltip(Tooltip.of(text(preset.description())));track(b,y);
        }
        contentHeight=((templates.size()+columns-1)/columns)*stride-6;
    }
    private void buildCustomization(){
        int y=caption("BASE SHAPE",0),columns=Math.max(1,layout.controls.width()/55);
        int cell=(layout.controls.width()-(columns-1)*4)/columns;
        var styles=CrosshairConfig.CrosshairStyle.values();
        for(int i=0;i<styles.length;i++){
            var style=styles[i];int index=i;
            var b=new NavButton(styleName(style),new Rect(layout.controls.x()+i%columns*(cell+4),0,cell,30),()->edit(()->config().style=style)){
                @Override protected void drawIcon(DrawContext c,int mx,int my,float delta){
                    frame(c,new Rect(getX(),getY(),width,height),config().style==style?BRAND:LINE,isHovered()?HOVER:FIELD);
                    var sample=new CrosshairConfig();CrosshairPresets.apply(sample,style);sample.color=config().color;sample.outlineEnabled=false;sample.width=1;
                    CrosshairRenderer.render(c,getX()+width/2,getY()+height/2,sample);
                }
            };
            b.setTooltip(Tooltip.of(text(styleName(style))));track(b,y+index/columns*34);
        }
        y+=((styles.length+columns-1)/columns)*34+8;
        y=caption("GEOMETRY • TYPE A VALUE OR DRAG",y);
        for(Dimension dimension:List.of(Dimension.SCALE,Dimension.LENGTH,Dimension.WIDTH,Dimension.GAP)){addDimension(dimension,y);y+=38;}
        y=caption("APPEARANCE",y+6);
        int half=(layout.controls.width()-6)/2;
        addColor("Crosshair",config().color,new Rect(layout.controls.x(),0,half,24),y,v->config().color=v);
        addColor("Outline",config().outlineColor,new Rect(layout.controls.x()+half+6,0,layout.controls.width()-half-6,24),y,v->config().outlineColor=v);
        y+=30;
        int[] colors={0xFFFFFFFF,0xFF46E891,0xFF55DFFF,0xFFC78DF5,0xFFFFD65A,0xFFFF647B};
        int swatch=Math.min(30,(layout.controls.width()-25)/6);
        for(int i=0;i<colors.length;i++){
            int color=colors[i];var b=new NavButton(String.format(Locale.ROOT,"#%06X",color&0xFFFFFF),new Rect(layout.controls.x()+i*(swatch+5),0,swatch,20),()->edit(()->config().color=color)){
                @Override protected void drawIcon(DrawContext c,int mx,int my,float delta){frame(c,new Rect(getX(),getY(),width,height),config().color==color?BRAND:LINE,color);}
            };b.setTooltip(Tooltip.of(b.getMessage()));track(b,y);
        }
        y+=26;addDimension(Dimension.OPACITY,y);y+=44;
        y=caption("DETAILS",y);
        addToggle("Outline",()->config().outlineEnabled,()->config().outlineEnabled=!config().outlineEnabled,y);y+=28;
        addToggle("Center Dot",()->config().centerDot,()->config().centerDot=!config().centerDot,y);y+=28;
        addToggle("Dynamic Gap",()->config().dynamicEnabled,()->{config().dynamicEnabled=!config().dynamicEnabled;pulseStart=System.nanoTime();},y);y+=32;
        addDimension(Dimension.OUTLINE,y);y+=38;addDimension(Dimension.DOT,y);y+=38;
        contentHeight=y;
    }
    private void addColor(String label,int color,Rect r,int y,IntConsumer setter){
        var button=new NavButton(label+" Color",r,()->{
            history.checkpoint(config());
            client.setScreen(new CrosshairColorPickerScreen(this,label,color,value->{setter.accept(value);save();},()->{}));
        }){
            @Override protected void drawIcon(DrawContext c,int mx,int my,float delta){
                super.drawIcon(c,mx,my,delta);rounded(c,getRight()-17,getY()+8,9,9,label.equals("Crosshair")?config().color:config().outlineColor);
            }
        };track(button,y);
    }
    private void addToggle(String label,java.util.function.BooleanSupplier state,Runnable action,int y){
        var b=new NavButton(label,new Rect(layout.controls.x(),0,layout.controls.width(),24),()->edit(action)){
            private final ToggleAnimation animation=new ToggleAnimation(state.getAsBoolean());
            @Override protected void drawIcon(DrawContext c,int mx,int my,float delta){
                frame(c,new Rect(getX(),getY(),width,height),LINE,FIELD);
                line(c,label,getX()+9,getY()+8,INK,.85f,width-56,false);
                ModMenuUi.toggle(c,getRight()-39,getY()+5,30,15,13,animation.value(state.getAsBoolean()),INK);
                setMessage(text(label+": "+(state.getAsBoolean()?"On":"Off")));
            }
        };b.toggle=true;track(b,y);
    }
    private void addDimension(Dimension dimension,int y){
        var slider=new DimensionSlider(dimension);sliders.add(slider);track(slider,y);
        var field=new TextFieldWidget(textRenderer,layout.controls.right()-53,0,44,14,text(dimension.label));
        field.setMaxLength(6);field.setDrawsBackground(false);field.setTextShadow(false);field.setEditableColor(INK);
        field.setText(dimension.shown(config()));field.setTooltip(Tooltip.of(text(dimension.label+": "+dimension.range.min()+" – "+dimension.range.max())));
        field.setChangedListener(value->{
            if(syncing)return;
            slider.numberDirty=true;
            try{double number=Double.parseDouble(value);if(!Double.isFinite(number)||number<dimension.range.min()||number>dimension.range.max()){field.setEditableColor(RED);return;}
                field.setEditableColor(INK);
                if(dimension.get(config())==dimension.range.clamp(number))return;
                if(!slider.editingNumber){history.checkpoint(config());slider.editingNumber=true;}
                dimension.set(config(),dimension.range.clamp(number));slider.sync();refreshHeader();
            }catch(NumberFormatException ignored){field.setEditableColor(RED);}
        });
        slider.field=field;track(field,y+9);
    }
    private void buildPreviewButtons(){
        Rect p=layout.preview;if(p==null||p.height()<100)return;
        button(zoom==1?"1×":"4×",new Rect(p.right()-43,p.y()+7,35,20),()->{zoom=zoom==1?4:1;rebuild();});
        var pulse=button(config().dynamicEnabled?"Test Pulse":"Dynamic: Off",new Rect(p.x()+10,p.bottom()-56,p.width()-20,21),()->pulseStart=System.nanoTime());
        pulse.active=config().dynamicEnabled;
        pulse.setTooltip(Tooltip.of(text("Enable Dynamic Gap in Customize to preview the pulse")));
        int w=(p.width()-28)/3;
        for(int i=0;i<3;i++){int value=i;button(List.of("Dark","Light","Terrain").get(i),new Rect(p.x()+10+i*(w+4),p.bottom()-28,w,20),()->{background=value;rebuild();}).selected=background==i;}
    }
    private void position(){
        int scroll=scrolls[tab]=Math.clamp(scrolls[tab],0,layout.maxScroll(contentHeight));
        for(var p:widgets){var w=p.widget();w.setY(layout.controls.y()+p.y()-scroll);w.visible=layout.fullyVisible(new Rect(w.getX(),w.getY(),w.getWidth(),w.getHeight()));if(!w.visible&&w.isFocused())setFocused(null);}
    }
    private void refreshHeader(){
        if(undoButton!=null)undoButton.active=history.available();
        if(enabledButton!=null){enabledButton.label(config().enabled?"Enabled":"Disabled");enabledButton.selected=config().enabled;}
    }
    @Override public void render(DrawContext c,int mx,int my,float delta){
        refreshHeader();frame(c,layout.panel,PANEL_BORDER,PANEL);
        int titleX=layout.back.right()+8,titleWidth=layout.enabled.x()-titleX-6;
        line(c,"AXIAL CLIENT",titleX,layout.panel.y()+7,BRAND,.75f,titleWidth,false);
        line(c,"CROSSHAIR",titleX,layout.panel.y()+21,INK,1f,titleWidth,true);
        c.fill(layout.panel.x()+12,layout.panel.y()+37,layout.panel.right()-12,layout.panel.y()+38,DIVIDER);
        c.enableScissor(layout.controls.x(),layout.controls.y(),layout.controls.right(),layout.controls.bottom());
        for(var caption:captions)line(c,caption.label(),layout.controls.x()+1,layout.controls.y()+caption.y()-scrolls[tab]+3,MUTED,.75f,layout.controls.width(),true);
        for(var p:widgets)if(p.widget().visible)p.widget().render(c,mx,my,delta);
        c.disableScissor();
        if(layout.preview!=null)drawPreview(c,layout.preview);
        else if(layout.done.x()-layout.undo.right()>=24){var sample=config().copy();sample.enabled=true;CrosshairRenderer.render(c,(layout.undo.right()+layout.done.x())/2,layout.done.y()+9,sample);}
        for(var child:children())if(child instanceof NavButton b&&widgets.stream().noneMatch(p->p.widget()==b))b.render(c,mx,my,delta);
        c.fill(layout.panel.x()+12,layout.panel.bottom()-32,layout.panel.right()-12,layout.panel.bottom()-31,DIVIDER);
        if(layout.panel.width()>520)line(c,error.isEmpty()?"Saves automatically":error,layout.undo.right()+15,layout.done.y()+6,error.isEmpty()?MUTED:RED,.75f,layout.done.x()-layout.undo.right()-24,false);
        int max=layout.maxScroll(contentHeight);
        if(max>0){Rect r=layout.scrollbar;int thumb=Math.max(12,r.height()*r.height()/contentHeight);rounded(c,r.x(),r.y(),r.width(),r.height(),DIVIDER);rounded(c,r.x(),r.y()+(r.height()-thumb)*scrolls[tab]/max,r.width(),thumb,MUTED);}
    }
    private void drawPreview(DrawContext c,Rect p){
        frame(c,p,LINE,SURFACE);boolean compact=p.height()<100;
        Rect scene=new Rect(p.x()+8,p.y()+(compact?4:35),p.width()-16,p.height()-(compact?8:100));
        if(background==2){c.fill(scene.x(),scene.y(),scene.right(),scene.bottom(),0xFF7FACC2);int ground=scene.y()+scene.height()/2;c.fill(scene.x(),ground,scene.right(),scene.bottom(),0xFF59794D);c.fill(scene.x()+scene.width()/3,ground+scene.height()/6,scene.right(),scene.bottom(),0xFF7F7770);}
        else c.fill(scene.x(),scene.y(),scene.right(),scene.bottom(),background==1?0xFFE5E2D9:0xFF14171D);
        if(!compact){line(c,"LIVE PREVIEW",p.x()+10,p.y()+13,MUTED,.75f,p.width()-58,true);line(c,config().enabled?"Actual size • "+zoom+"× view":"Disabled in game",p.x()+10,p.bottom()-73,MUTED,.75f,p.width()-20,false);}
        var sample=config().copy();sample.enabled=true;
        float pulse=config().dynamicEnabled?CrosshairDynamicState.atElapsedMillis((System.nanoTime()-pulseStart)/1_000_000L%1500):1;
        c.enableScissor(scene.x(),scene.y(),scene.right(),scene.bottom());c.getMatrices().pushMatrix();
        try{c.getMatrices().translate(scene.x()+scene.width()/2,scene.y()+scene.height()/2);c.getMatrices().scale(zoom,zoom);CrosshairRenderer.render(c,0,0,sample,pulse);}
        finally{c.getMatrices().popMatrix();c.disableScissor();}
    }
    @Override public boolean mouseClicked(Click click,boolean doubled){
        for(var slider:sliders)slider.editingNumber=false;
        Rect bar=layout.scrollbar;
        if(click.button()==0&&layout.maxScroll(contentHeight)>0&&click.x()>=bar.x()-2&&click.x()<bar.right()+3&&click.y()>=bar.y()&&click.y()<bar.bottom()){draggingScrollbar=true;dragScroll(click.y());return true;}
        return super.mouseClicked(click,doubled);
    }
    @Override public boolean mouseDragged(Click click,double dx,double dy){if(draggingScrollbar){dragScroll(click.y());return true;}return super.mouseDragged(click,dx,dy);}
    @Override public boolean mouseReleased(Click click){draggingScrollbar=false;return super.mouseReleased(click);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(layout.controls.contains(x,y)){scrollTo(scrolls[tab]-(int)Math.round(vertical*32));return true;}return false;}
    private void scrollTo(int value){scrolls[tab]=Math.clamp(value,0,layout.maxScroll(contentHeight));position();}
    private void dragScroll(double y){Rect r=layout.scrollbar;int thumb=Math.max(12,r.height()*r.height()/contentHeight);scrollTo((int)Math.round((y-r.y()-thumb/2.0)/Math.max(1,r.height()-thumb)*layout.maxScroll(contentHeight)));}
    @Override public boolean keyPressed(KeyInput input){
        if(getFocused() instanceof TextFieldWidget)return super.keyPressed(input);
        switch(input.key()){
            case GLFW.GLFW_KEY_PAGE_DOWN -> {scrollTo(scrolls[tab]+layout.controls.height());return true;}
            case GLFW.GLFW_KEY_PAGE_UP -> {scrollTo(scrolls[tab]-layout.controls.height());return true;}
            case GLFW.GLFW_KEY_HOME -> {scrollTo(0);return true;}
            case GLFW.GLFW_KEY_END -> {scrollTo(layout.maxScroll(contentHeight));return true;}
        }
        if(QuickMenuScreen.isMenuKey(input)){close();return true;}return super.keyPressed(input);
    }
    @Override public void removed(){save();}
    @Override public void close(){save();super.close();}
    private static String styleName(CrosshairConfig.CrosshairStyle style){return switch(style){case LUNAR->"Cross";case SMALL_DOT->"Pinpoint";case SMALL_CROSS->"Small Cross";default->style.name().charAt(0)+style.name().substring(1).toLowerCase(Locale.ROOT);};}
    private final class PresetButton extends NavButton {
        private final CrosshairPresets.Template preset;private final CrosshairConfig sample;
        PresetButton(CrosshairPresets.Template preset,Rect r){super(preset.name(),r,()->edit(()->CrosshairPresets.applyTemplate(config(),preset)));this.preset=preset;sample=preset.preview(config());}
        @Override protected void drawIcon(DrawContext c,int mx,int my,float delta){
            boolean chosen=CrosshairPresets.matches(config(),preset);Rect r=new Rect(getX(),getY(),width,height);
            frame(c,r,chosen?BRAND:LINE,isHovered()||isFocused()?HOVER:FIELD);
            c.enableScissor(Math.max(layout.controls.x(),r.x()+2),Math.max(layout.controls.y(),r.y()+2),Math.min(layout.controls.right(),r.right()-2),Math.min(layout.controls.bottom(),r.bottom()-2));
            CrosshairRenderer.render(c,height<45?getX()+17:getX()+width/2,height<45?getY()+height/2:getY()+22,sample);c.disableScissor();
            line(c,preset.name(),getX()+(height<45?32:6),height<45?getY()+(height-8)/2:getBottom()-13,chosen?BRAND:INK,.75f,width-(height<45?38:12),chosen);
        }
    }
    private enum Dimension {
        SCALE("Scale",.1,4,.05),LENGTH("Arm Length / Radius",0,32,1),WIDTH("Thickness",1,12,1),GAP("Gap",0,20,1),
        OPACITY("Opacity %",10,100,1),OUTLINE("Outline Thickness",1,4,1),DOT("Center Dot Size",1,8,1);
        final String label;final SettingRange range;
        Dimension(String label,double min,double max,double step){this.label=label;range=new SettingRange(min,max,step);}
        double get(CrosshairConfig c){return switch(this){case SCALE->c.size;case LENGTH->c.length;case WIDTH->c.width;case GAP->c.gap;case OPACITY->c.opacity*100;case OUTLINE->c.outlineThickness;case DOT->c.dotSize;};}
        void set(CrosshairConfig c,double v){switch(this){case SCALE->c.size=(float)v;case LENGTH->c.length=(float)v;case WIDTH->c.width=(float)v;case GAP->c.gap=(float)v;case OPACITY->c.opacity=(float)v/100;case OUTLINE->c.outlineThickness=(int)v;case DOT->c.dotSize=(int)v;}}
        String shown(CrosshairConfig c){return this==SCALE?String.format(Locale.ROOT,"%.2f",get(c)):String.format(Locale.ROOT,"%.0f",get(c));}
    }
    private final class DimensionSlider extends SliderWidget {
        final Dimension dimension;TextFieldWidget field;boolean dragging,editingNumber,numberDirty;
        DimensionSlider(Dimension dimension){super(layout.controls.x(),0,layout.controls.width()-64,34,text(dimension.label),dimension.range.progress(dimension.get(config())));this.dimension=dimension;updateMessage();}
        void sync(){value=dimension.range.progress(dimension.get(config()));updateMessage();}
        @Override protected void updateMessage(){if(dimension!=null)setMessage(text(dimension.label+": "+dimension.shown(config())));}
        @Override protected void applyValue(){
            if(!dragging)history.checkpoint(config());dimension.set(config(),dimension.range.value(value));if(!dragging)save();updateMessage();
            if(field!=null){syncing=true;try{field.setText(dimension.shown(config()));}finally{syncing=false;}}refreshHeader();
        }
        @Override public void onClick(Click click,boolean doubled){history.checkpoint(config());dragging=true;fromX(click.x());}
        @Override protected void onDrag(Click click,double dx,double dy){fromX(click.x());}
        @Override public void onRelease(Click click){dragging=false;save();}
        private void fromX(double x){setValue(dimension.range.progress(dimension.range.value((x-getX()-5)/Math.max(1,width-10))));}
        @Override public void playDownSound(SoundManager sound){AxialUiTheme.playButtonClickSound();}
        @Override public void renderWidget(DrawContext c,int mx,int my,float delta){
            if(field!=null&&!field.isFocused()&&numberDirty){syncing=true;try{field.setText(dimension.shown(config()));field.setEditableColor(INK);}finally{syncing=false;}numberDirty=false;editingNumber=false;save();}
            line(c,dimension.label,getX()+1,getY()+3,MUTED,.8f,width,false);
            int y=getY()+24,filled=(int)Math.round(value*(width-10));rounded(c,getX()+4,y,width-8,3,DIVIDER);rounded(c,getX()+4,y,Math.max(2,filled),3,BRAND);circle(c,getX()+filled,y-3,9,isHovered()||isFocused()?INK:BRAND);
            frame(c,new Rect(layout.controls.right()-58,getY()+3,58,27),field!=null&&field.isFocused()?BRAND:LINE,FIELD);
        }
    }
}
