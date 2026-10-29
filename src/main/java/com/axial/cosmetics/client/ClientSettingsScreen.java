package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Util;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.util.Identifier;
import org.axial.axialutils.client.AxialConfigManager;
import org.axial.axialutils.client.AxialUiTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static com.axial.cosmetics.client.ModMenuUi.*;

/** General client settings, using the existing configuration stores. */
public final class ClientSettingsScreen extends Screen {
    private record Row(ClickableWidget widget,int offset) {}
    private final Screen parent;
    private final List<Row> rows=new ArrayList<>();
    private ClientSettingsLayout layout;
    private MenuAnimation animation=new MenuAnimation();
    private StrengthSlider strength;
    private int scroll;
    private boolean reopen,draggingScrollbar;
    private String error="";

    public ClientSettingsScreen(Screen parent) { super(text("SETTINGS"));this.parent=parent; }

    /** Also replaces legacy OPTIONS entry points after their initialization completes. */
    public static boolean openOptions(Screen backend) {
        try {
            if(!ModMenuCatalog.isOptions(backend))return false;
            MinecraftClient.getInstance().setScreen(new ClientSettingsScreen(ModMenuCatalog.parent(backend)));
            return true;
        } catch(ReflectiveOperationException ex) {
            org.slf4j.LoggerFactory.getLogger(ClientSettingsScreen.class).warn("Could not open client settings",ex);
            return false;
        }
    }

    @Override protected void init() {
        if(reopen){animation=new MenuAnimation();reopen=false;}
        clearChildren();rows.clear();layout=new ClientSettingsLayout(width,height);
        addDrawableChild(new SettingsButton("Mods",layout.back,this::close));
        addDrawableChild(new SettingsButton("Settings",layout.settings,()->{}));
        var wardrobe=new SettingsButton("Wardrobe",layout.wardrobe,()->WardrobeScreen.open(this));
        wardrobe.active=WardrobeAccess.allowed();
        wardrobe.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(text("Wardrobe • Granted accounts only")));
        addDrawableChild(wardrobe);
        var hud=new SettingsButton("Edit HUD",layout.editHud,()->change(()->{
            if(parent instanceof ModMenuScreen menu)menu.openHudEditor();
        }));
        hud.active=parent instanceof ModMenuScreen;
        addDrawableChild(hud);
        addDrawableChild(new SettingsButton("CLOSE",layout.close,this::close));
        addDrawableChild(new SettingsButton("Done",layout.done,this::close));
        addDrawableChild(new SettingsButton("Reset Defaults",layout.reset,()->change(()->{
            AxialConfigManager.get().showPlayerShadows=true;AxialConfigManager.save();
            ThirdPersonNameTagsConfig.setEnabled(false);EnchantGlintConfig.setStrength(1);EnchantGlintConfig.resetColor();
            PerformanceConfig.reset();strength.refresh();
        })));
        row(new ToggleButton("Player Shadows","Show shadows beneath players",()->Boolean.TRUE.equals(AxialConfigManager.get().showPlayerShadows),value->{
            AxialConfigManager.get().showPlayerShadows=value;AxialConfigManager.save();
        }),16);
        row(new ToggleButton("Third Person Name Tags","Show your name in third person",ThirdPersonNameTagsConfig::enabled,ThirdPersonNameTagsConfig::setEnabled),58);
        row(new ToggleButton("Display FPS","Show the frame rate in the top-left corner",PerformanceConfig::displayFps,PerformanceConfig::setDisplayFps),130);
        row(new SettingsButton("FPS COLOR",new Rect(layout.controls.x(),0,layout.controls.width(),36),()->client.setScreen(
                new CrosshairColorPickerScreen(this,"FPS DISPLAY",PerformanceConfig.fpsColor(),value->change(()->PerformanceConfig.setFpsColor(value)),()->{}))),172);
        row(new ToggleButton("Render Dropped Items","Hide dropped items when off; pickups still work",PerformanceConfig::droppedItems,PerformanceConfig::setDroppedItems),214);
        row(new ToggleButton("Render Liquids","Show water and lava surfaces; rebuilds chunks",PerformanceConfig::liquids,PerformanceConfig::setLiquids),256);
        row(new ToggleButton("Disable Spawner Mobs","Hide miniature mobs inside regular and trial spawners",PerformanceConfig::disableSpawnerMobs,PerformanceConfig::setDisableSpawnerMobs),298);
        row(new ToggleButton("Render Particles","Show particles such as smoke, sparks and spell effects",PerformanceConfig::particles,PerformanceConfig::setParticles),340);
        row(new ToggleButton("Render Weather","Show rain, snow and rain splashes",PerformanceConfig::weather,PerformanceConfig::setWeather),382);
        strength=new StrengthSlider();row(strength,456);
        int colorWidth=layout.controls.width()-66;
        row(new SettingsButton("COLOR",new Rect(layout.controls.x(),0,colorWidth,36),()->client.setScreen(
                new CrosshairColorPickerScreen(this,"ENCHANT GLINT",EnchantGlintConfig.color(),value->change(()->{
                    EnchantGlintConfig.setColor(value);EnchantGlintConfig.save();
                }),()->{}))),492);
        row(new SettingsButton("Reset",new Rect(layout.controls.right()-58,0,58,36),()->change(EnchantGlintConfig::resetColor)),492);
        var wiki=QuickMenuLinks.get("wiki");
        var wikiButton=new SettingsButton("Open Wiki",new Rect(layout.controls.x(),0,layout.controls.width(),36),()->Util.getOperatingSystem().open(wiki));
        wikiButton.active=wiki!=null;
        row(wikiButton,562);
        scroll=Math.clamp(scroll,0,layout.maxScroll());positionRows();
    }

    private void row(ClickableWidget widget,int offset){rows.add(new Row(widget,offset));addDrawableChild(widget);}
    private void positionRows() {
        for(Row row:rows) {
            var widget=row.widget();widget.setY(layout.controls.y()+row.offset()-scroll);
            widget.visible=layout.fullyVisible(new Rect(widget.getX(),widget.getY(),widget.getWidth(),widget.getHeight()));
            if(!widget.visible&&widget.isFocused())setFocused(null);
        }
    }
    private void change(Runnable action) {
        try{action.run();error="";}
        catch(RuntimeException ex){error="COULD NOT SAVE. TRY AGAIN.";org.slf4j.LoggerFactory.getLogger(ClientSettingsScreen.class).warn("Could not save client settings",ex);}
    }

    @Override public void render(DrawContext c,int mx,int my,float delta) {
        float offset=animation.offset();c.getMatrices().pushMatrix();c.getMatrices().translate(0,offset);
        try {
            frame(c,layout.panel,PANEL_BORDER,PANEL);
            int brandWidth=layout.back.x()-layout.panel.x()-20;
            if(brandWidth>65) {
                c.drawTexture(RenderPipelines.GUI_TEXTURED,Identifier.of("axialutils","textures/gui/menu_logo.png"),
                        layout.panel.x()+8,layout.panel.y()+5,0,0,64,32,240,117,240,117);
                if(brandWidth>175)label(c,textRenderer,"AXIAL CLIENT",layout.panel.x()+77,layout.panel.y()+17,
                        BRAND,1.2f,brandWidth-70,true);
            }
            c.fill(layout.panel.x()+1,layout.headerBottom,layout.panel.right()-1,layout.headerBottom+1,DIVIDER);
            if(!layout.compact) {
                label(c,textRenderer,"Settings",layout.controls.x(),layout.panel.y()+54,INK,1.7f,layout.controls.width(),true);
                label(c,textRenderer,"Performance and appearance",layout.controls.x(),layout.panel.y()+75,MUTED,.85f,layout.controls.width(),false);
            }
            c.enableScissor(layout.controls.x(),layout.controls.y(),layout.controls.right(),layout.controls.bottom());
            section(c,"GENERAL",0);section(c,"PERFORMANCE",112);section(c,"ENCHANT GLINT",438);section(c,"HELP & LINKS",544);
            frame(c,new Rect(layout.controls.x(),layout.controls.y()+456-scroll,layout.controls.width(),72),LINE,SURFACE);
            c.fill(layout.controls.x()+1,layout.controls.y()+492-scroll,layout.controls.right()-1,layout.controls.y()+493-scroll,DIVIDER);
            for(Row row:rows)if(row.widget().visible)row.widget().render(c,mx,(int)(my-offset),delta);
            c.disableScissor();
            for(var child:children())if(child instanceof SettingsButton button&&rows.stream().noneMatch(row->row.widget()==button))button.render(c,mx,(int)(my-offset),delta);
            c.fill(layout.panel.x()+12,layout.panel.bottom()-33,layout.panel.right()-12,layout.panel.bottom()-32,DIVIDER);
            if(layout.panel.width()>=400)centered(c,error.isEmpty()?"Saves automatically":error,
                    (layout.reset.right()+layout.done.x())/2,layout.done.y()+7,error.isEmpty()?MUTED:RED,.75f,layout.done.x()-layout.reset.right()-12);
            if(layout.maxScroll()>0) {
                Rect bar=layout.scrollbar;int thumb=Math.max(12,bar.height()*bar.height()/ClientSettingsLayout.CONTENT_HEIGHT);
                rounded(c,bar.x(),bar.y(),bar.width(),bar.height(),DIVIDER);
                rounded(c,bar.x(),bar.y()+(bar.height()-thumb)*scroll/layout.maxScroll(),bar.width(),thumb,MUTED);
            }
        } finally {c.getMatrices().popMatrix();}
    }
    private void centered(DrawContext c,String value,int center,int y,int color,float scale,int maxWidth) {
        String trimmed=textRenderer.trimToWidth(styled(value,scale,false),Math.max(0,maxWidth)).getString();
        label(c,textRenderer,trimmed,center-textRenderer.getWidth(styled(trimmed,scale,false))/2,y,color,scale,maxWidth,false);
    }
    private static Text styled(String value,float scale,boolean bold) {
        return Text.literal(value).styled(style->style.withFont(new StyleSpriteSource.Font(UiTypography.font(scale,bold))));
    }
    private static void label(DrawContext c,TextRenderer renderer,String value,int x,int y,int color,float scale,int maxWidth,boolean bold) {
        if(maxWidth<=0)return;
        String trimmed=renderer.trimToWidth(styled(value,scale,bold),maxWidth).getString();
        c.drawText(renderer,styled(trimmed,scale,bold),x,y,tint(color),false);
    }
    private void section(DrawContext c,String title,int offset) {
        int y=layout.controls.y()+offset-scroll;
        label(c,textRenderer,title,layout.controls.x()+1,y+3,MUTED,.8f,layout.controls.width(),true);
        int start=layout.controls.x()+UiTypography.width(textRenderer,title,.8f,true)+12;
        if(start<layout.controls.right())c.fill(start,y+7,layout.controls.right(),y+8,DIVIDER);
    }
    @Override public void close(){animation.close();}
    @Override public void removed(){reopen=true;}
    @Override public void tick(){if(animation.finished())client.setScreen(parent);}
    private Click transformed(Click click){return new Click(click.x(),click.y()-animation.offset(),click.buttonInfo());}
    @Override public boolean mouseClicked(Click click,boolean doubled) {
        if(animation.closing())return true;
        click=transformed(click);Rect bar=layout.scrollbar;
        if(click.button()==0&&layout.maxScroll()>0&&click.x()>=bar.x()-2&&click.x()<bar.right()+3&&click.y()>=bar.y()&&click.y()<bar.bottom()) {
            draggingScrollbar=true;dragScroll(click.y());return true;
        }
        return super.mouseClicked(click,doubled);
    }
    @Override public boolean mouseDragged(Click click,double dx,double dy) {
        if(draggingScrollbar){dragScroll(click.y()-animation.offset());return true;}
        return super.mouseDragged(transformed(click),dx,dy);
    }
    @Override public boolean mouseReleased(Click click){draggingScrollbar=false;return super.mouseReleased(transformed(click));}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(animation.closing())return true;
        if(layout.controls.contains(x,y-animation.offset())){scrollTo(scroll-(int)Math.round(vertical*30));return true;}
        return false;
    }
    private void scrollTo(int value){scroll=Math.clamp(value,0,layout.maxScroll());positionRows();}
    private void dragScroll(double y){scrollTo((int)((y-layout.scrollbar.y())/layout.scrollbar.height()*layout.maxScroll()));}
    @Override public boolean keyPressed(KeyInput input) {
        if(animation.closing())return true;
        switch(input.key()) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN -> {scrollTo(scroll+layout.controls.height());return true;}
            case org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP -> {scrollTo(scroll-layout.controls.height());return true;}
            case org.lwjgl.glfw.GLFW.GLFW_KEY_HOME -> {scrollTo(0);return true;}
            case org.lwjgl.glfw.GLFW.GLFW_KEY_END -> {scrollTo(layout.maxScroll());return true;}
        }
        if(QuickMenuScreen.isMenuKey(input)){close();return true;}
        return super.keyPressed(input);
    }

    private class SettingsButton extends ButtonWidget {
        final String label;
        SettingsButton(String label,Rect r,Runnable action) {
            super(r.x(),r.y(),r.width(),r.height(),text(label),ignored->action.run(),DEFAULT_NARRATION_SUPPLIER);this.label=label;
        }
        @Override public void playDownSound(SoundManager sounds){AxialUiTheme.playButtonClickSound();}
        @Override protected void drawIcon(DrawContext c,int mx,int my,float delta) {
            if(label.equals("Wardrobe")) {
                active=WardrobeAccess.allowed();
                Rect r=new Rect(getX(),getY(),width,height);
                frame(c,r,active&&isHovered()?HOVER_LINE:LINE,SURFACE);
                label(c,textRenderer,active?"Wardrobe":"Locked",r.x()+5,r.y()+8,active?INK:MUTED,.8f,r.width()-10,false);
                return;
            }
            boolean hover=active&&(isHovered()||isFocused());Rect r=new Rect(getX(),getY(),width,height);
            if(label.equals("Reset"))r=new Rect(r.x()+4,r.y()+8,r.width()-8,20);
            if(label.equals("CLOSE")){icon(c,"close",r.x()+4,r.y()+5,11,11,hover?INK:MUTED);return;}
            if(label.equals("Mods")||label.equals("Settings")||label.equals("Edit HUD")) {
                boolean selected=label.equals("Settings");
                frame(c,r,selected?SELECTED_LINE:hover?HOVER_LINE:0x00454C53,selected?SELECTED:hover?HOVER:SURFACE);
                String symbol=label.equals("Mods")?"cube":selected?"gear":"move";
                icon(c,symbol,r.x()+7,r.y()+7,10,10,active?INK:MUTED);
                label(c,textRenderer,label,r.x()+22,r.y()+8,active?INK:MUTED,.8f,r.width()-25,false);
                return;
            }
            boolean colorButton=label.equals("COLOR")||label.equals("FPS COLOR");
            if(!colorButton||label.equals("FPS COLOR"))frame(c,r,hover?HOVER_LINE:label.equals("Done")?SELECTED_LINE:LINE,
                    hover?HOVER:label.equals("Done")?0xFFB0B8C9:FIELD);
            if(colorButton) {
                boolean fps=label.equals("FPS COLOR");
                int color=fps?PerformanceConfig.fpsColor():EnchantGlintConfig.color();
                label(c,textRenderer,fps?"FPS Color":"Glint Color",r.x()+13,r.y()+14,INK,.9f,Math.min(100,r.width()/3),true);
                int fieldX=r.x()+Math.min(123,r.width()/2);
                rounded(c,fieldX,r.y()+8,20,20,color);
                frame(c,new Rect(fieldX+26,r.y()+8,Math.max(20,r.right()-fieldX-34),20),hover?HOVER_LINE:LINE,FIELD);
                label(c,textRenderer,String.format(Locale.ROOT,"#%06X",color&0xFFFFFF),fieldX+34,r.y()+14,MUTED,.85f,r.right()-fieldX-42,false);
                setMessage(text((fps?"FPS COLOR: ":"ENCHANT GLINT COLOR: ")+String.format(Locale.ROOT,"#%06X",color&0xFFFFFF)));return;
            }
            centered(c,label,r.x()+r.width()/2,r.y()+(r.height()-9)/2,label.equals("Done")&&!hover?0xFF14171B:INK,.8f,r.width()-10);
        }
    }
    private final class ToggleButton extends SettingsButton {
        final BooleanSupplier state;final ToggleAnimation motion;final String description;
        ToggleButton(String label,String description,BooleanSupplier state,Consumer<Boolean> set) {
            super(label,new Rect(layout.controls.x(),0,layout.controls.width(),38),()->change(()->set.accept(!state.getAsBoolean())));
            this.description=description;this.state=state;motion=new ToggleAnimation(state.getAsBoolean());
        }
        @Override protected void drawIcon(DrawContext c,int mx,int my,float delta) {
            boolean on=state.getAsBoolean();float progress=motion.value(on);
            frame(c,new Rect(getX(),getY(),width,height),LINE,FIELD);
            label(c,textRenderer,label,getX()+13,getY()+8,INK,.95f,width-65,true);
            label(c,textRenderer,description,getX()+13,getY()+23,MUTED,.8f,width-65,false);
            ModMenuUi.toggle(c,getRight()-43,getY()+11,30,16,14,progress,INK);
            setMessage(text(label+": "+(on?"ON":"OFF")));
        }
    }
    private final class StrengthSlider extends SliderWidget {
        StrengthSlider(){super(layout.controls.x(),0,layout.controls.width(),36,text("STRENGTH"),0);refresh();}
        void refresh(){value=(EnchantGlintConfig.strength()-1)/(EnchantGlintConfig.MAX_STRENGTH-1);updateMessage();}
        private String shown(){return String.format(Locale.ROOT,"%.0f%%",EnchantGlintConfig.strength()*100);}
        @Override protected void updateMessage(){setMessage(text("ENCHANT GLINT STRENGTH: "+shown()));}
        @Override protected void applyValue(){change(()->EnchantGlintConfig.setStrength((float)(1+value*(EnchantGlintConfig.MAX_STRENGTH-1))));updateMessage();}
        @Override public void playDownSound(SoundManager sounds){AxialUiTheme.playButtonClickSound();}
        @Override public void onRelease(Click click){}
        int trackX(){return getX()+Math.min(123,width/3);}
        int trackWidth(){return Math.max(8,getRight()-58-trackX());}
        @Override public void renderWidget(DrawContext c,int mx,int my,float delta) {
            label(c,textRenderer,"Glint Strength",getX()+13,getY()+14,INK,.9f,trackX()-getX()-20,true);
            int y=getY()+17;
            rounded(c,trackX(),y,trackWidth(),4,DIVIDER);
            int filled=(int)Math.round(value*(trackWidth()-10));
            rounded(c,trackX(),y,Math.max(4,filled+5),4,EnchantGlintConfig.color());
            circle(c,trackX()+filled,y-3,10,isHovered()||isFocused()?0xFFFFFFFF:INK);
            centered(c,shown(),getRight()-29,getY()+14,MUTED,.9f,48);

        }
        @Override public void onClick(Click click,boolean doubled){fromPosition(click.x());}
        @Override protected void onDrag(Click click,double dx,double dy){fromPosition(click.x());}
        private void fromPosition(double x){setValue((x-trackX()-5)/Math.max(1,trackWidth()-10));}
    }
}
