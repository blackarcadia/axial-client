package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.SoundManager;
import org.axial.axialutils.client.AxialUiTheme;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.Identifier;
import org.slf4j.LoggerFactory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static com.axial.cosmetics.client.ModMenuUi.*;

/** A native widget menu backed by the original modules and settings screens. */
public final class ModMenuScreen extends Screen {
    private final Screen backend;
    private final Screen returnTo;
    private MenuAnimation animation=new MenuAnimation();
    private boolean reopen;
    private final List<ModMenuEntry> entries;
    private ModMenuEntry.Category category = ModMenuEntry.Category.ALL;
    private String query = "", error = "";
    private boolean enabledOnly, draggingScrollbar;
    private int scroll;
    private ModMenuLayout layout;
    private TextFieldWidget search;
    private final List<MenuButton> cardButtons = new ArrayList<>();
    private final java.util.Map<ModMenuEntry,ToggleAnimation> toggleAnimations = new java.util.HashMap<>();

    private ModMenuScreen(Screen backend, List<ModMenuEntry> entries,Screen returnTo) {
        super(text("Mod menu"));
        this.backend = backend;
        this.returnTo=returnTo;
        this.entries = entries;
    }

    public static void open(Screen backend) {
        try {
            if (ModMenuCatalog.isMain(backend)) MinecraftClient.getInstance().setScreen(new ModMenuScreen(backend, ModMenuCatalog.read(backend),ModMenuCatalog.parent(backend)));
        } catch (ReflectiveOperationException ex) {
            LoggerFactory.getLogger(ModMenuScreen.class).warn("Could not open mod library; retaining the original menu",ex);
        }
    }

    @Override protected void init() {
        if(reopen){animation=new MenuAnimation();reopen=false;}
        layout = new ModMenuLayout(width,height);
        if(layout.tight) category=ModMenuEntry.Category.ALL;
        rebuild();
    }

    private List<ModMenuEntry> filtered() {
        return entries.stream().filter(e -> e.matches(category,query,enabledOnly)).toList();
    }

    private void rebuild() {
        boolean focus = search != null && search.isFocused();
        int cursor = search == null ? query.length() : search.getCursor();
        clearChildren();
        cardButtons.clear();
        add("Close",layout.close,"close",this::close,false,null);
        var nav = layout.navigation;
        int tabWidth = Math.min(102, nav.width()/3);
        int navX = nav.x()+(nav.width()-tabWidth*3)/2;
        String[] tabs = {"Mods","Settings","Edit HUD"}, icons = {"cube","gear","move"};
        for (int i=0;i<3;i++) {
            int tab=i;
            add(tabs[i],new ModMenuLayout.Rect(navX+i*tabWidth,nav.y(),tabWidth,nav.height()),icons[i],
                    () -> { if(tab==1)client.setScreen(new ClientSettingsScreen(this));
                        else if(tab==2)run(() -> ModMenuCatalog.openMode(backend,"MOVE")); },i==0,null);
        }
        var box=layout.search;
        search = new TextFieldWidget(textRenderer,box.x()+23,box.y()+(box.height()-9)/2,box.width()-29,12,text("Search mods"));
        search.setMaxLength(64);
        search.setDrawsBackground(false);
        search.setTextShadow(false);
        search.setEditableColor(INK);
        search.setPlaceholder(text("Search mods..."));
        search.addFormatter((value,index)->text(value).asOrderedText());
        search.setText(query);
        search.setCursor(Math.min(cursor,query.length()),false);
        search.setChangedListener(value -> { query=value; scroll=0; rebuild(); });
        addDrawableChild(search);
        if(focus) setFocused(search);
        add("Enabled only",layout.enabledFilter,"filter",()->{enabledOnly=!enabledOnly;scroll=0;rebuild();},enabledOnly,null);
        for(var item:ModMenuEntry.Category.values()) {
            if(layout.tight) break;
            add(item.label,layout.category(item.ordinal()),item.icon,()->{category=item;scroll=0;rebuild();},category==item,null);
        }
        var matches=filtered();
        scroll=Math.clamp(scroll,0,layout.maxScroll(matches.size()));
        for(int i=0;i<matches.size();i++) {
            var entry=matches.get(i);
            var card=layout.card(i,scroll);
            if(entry.toggle()!=null) {
                var button = new MenuButton("Toggle "+entry.title(),layout.toggle(card),"toggle",()->run(()->{
                    entry.toggle().run();
                    rebuild();
                }),false,entry,true);
                button.visible = button.getY() >= layout.grid.y() && button.getBottom() <= layout.grid.bottom();
                cardButtons.add(addSelectableChild(button));
            }
            if(entry.configure()!=null) {
                var button = new MenuButton("Configure "+entry.title(),layout.settings(card),"gear",()->run(()->{
                    if(!ModFeatureSettings.open(entry.title().toUpperCase(java.util.Locale.ROOT),this,backend))entry.configure().run();
                }),false,entry,true);
                button.visible = button.getY() >= layout.grid.y() && button.getBottom() <= layout.grid.bottom();
                button.setTooltip(Tooltip.of(text("Configure "+entry.title())));
                cardButtons.add(addSelectableChild(button));
            }
        }
    }

    private void run(Runnable action) {
        try { error=""; action.run(); }
        catch(RuntimeException ex) {
            error="Could not update this mod. Please try again.";
            LoggerFactory.getLogger(ModMenuScreen.class).warn("Mod menu action failed",ex);
        }
    }

    private MenuButton add(String label,ModMenuLayout.Rect bounds,String icon,Runnable action,boolean selected,ModMenuEntry entry) {
        var button=new MenuButton(label,bounds,icon,action,selected,entry,false);
        button.setTooltip(Tooltip.of(text(label)));
        return addDrawableChild(button);
    }

    void openHudEditor() {
        run(() -> ModMenuCatalog.openMode(backend,"MOVE"));
    }

    @Override public void removed() { reopen=true; }
    @Override public void close() { animation.close(); }
    @Override public void tick() {
        if(animation.finished()) {
            if(returnTo instanceof QuickMenuScreen)client.setScreen(returnTo);
            else backend.close();
        }
    }

    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0,animation.offset());
        try { renderContent(context,mouseX,(int)(mouseY-animation.offset()),delta); }
        finally { context.getMatrices().popMatrix(); }
    }

    private void renderContent(DrawContext context,int mouseX,int mouseY,float delta) {
        var panel=layout.panel;
        frame(context,panel,PANEL_BORDER,PANEL);
        boolean small = panel.height()<320;
        int logoSize=layout.tight?23:small?29:36;
        context.drawTexture(RenderPipelines.GUI_TEXTURED,Identifier.of("axialutils","textures/gui/menu_logo.png"),
                panel.x()+10,panel.y()+4,0,0,logoSize*2,logoSize,240,117,240,117);
        if(!layout.tight) {
            label(context,textRenderer,"AXIAL CLIENT",panel.x()+logoSize*2+18,panel.y()+9,BRAND,small?1f:1.2f,panel.width()-logoSize*2-55,true);
            label(context,textRenderer,"Mod menu",panel.x()+logoSize*2+18,panel.y()+25,MUTED,.85f,panel.width()-logoSize*2-55,false);
        }
        int dividerY=layout.navigation.bottom()+3;
        context.fill(panel.x()+10,dividerY,panel.right()-10,dividerY+1,DIVIDER);
        frame(context,layout.search,search.isFocused()?FOCUS:LINE,FIELD);
        icon(context,"search",layout.search.x()+7,layout.search.y()+5,11,11,MUTED);
        if(layout.sidebar) context.fill(layout.categories.right()+4,layout.categories.y(),layout.categories.right()+5,layout.grid.bottom(),DIVIDER);
        super.render(context,mouseX,mouseY,delta);
        context.enableScissor(layout.grid.x(),layout.grid.y(),layout.grid.right(),layout.grid.bottom());
        var matches=filtered();
        for(int i=0;i<matches.size();i++) {
            var card=layout.card(i,scroll);
            if(card.bottom()<=layout.grid.y() || card.y()>=layout.grid.bottom()) continue;
            var entry=matches.get(i);
            frame(context,card,card.contains(mouseX,mouseY)?HOVER_LINE:LINE,SURFACE);
            int size=card.height()<55?14:22;
            icon(context,entry.icon(),card.x()+9,card.y()+10,size,size,0xFFD6DCE1);
            int titleX=card.x()+size+17;
            label(context,textRenderer,entry.title(),titleX,card.y()+9,INK,.95f,card.right()-titleX-7,true);
            if(card.height()>=60) {
                var lines=textRenderer.wrapLines(UiTypography.text(entry.description(),.78f,false),Math.max(1,card.right()-titleX-8));
                for(int line=0;line<Math.min(2,lines.size());line++) {
                    context.drawText(textRenderer,lines.get(line),titleX,card.y()+22+line*9,MUTED,false);
                }
            }
            if(entry.toggle()==null) label(context,textRenderer,"Configure",card.x()+9,card.bottom()-17,MUTED,.8f,card.width()-40,false);
        }
        if(matches.isEmpty()) label(context,textRenderer,"No matching mods",layout.grid.x()+10,layout.grid.y()+15,MUTED,1f,layout.grid.width()-20,false);
        for(var button:cardButtons) {
            if(button.getBottom()>layout.grid.y() && button.getY()<layout.grid.bottom()) button.render(context,mouseX,mouseY,delta);
        }
        context.disableScissor();
        drawScrollbar(context,matches.size());
        context.fill(panel.x()+10,panel.bottom()-17,panel.right()-10,panel.bottom()-16,DIVIDER);
        String count=error.isBlank()?matches.size()+" of "+entries.size()+" mods":error;
        label(context,textRenderer,count,panel.x()+12,panel.bottom()-12,error.isBlank()?MUTED:RED,.75f,panel.width()-90,false);
        label(context,textRenderer,"Esc to close",panel.right()-68,panel.bottom()-12,MUTED,.75f,60,false);
    }

    private void drawScrollbar(DrawContext context,int count) {
        if(layout.maxScroll(count)==0)return;
        var track=layout.scrollbar;
        rounded(context,track.x(),track.y(),track.width(),track.height(),DIVIDER);
        int thumb=Math.max(12,track.height()*layout.grid.height()/layout.contentHeight(count));
        int y=track.y()+(track.height()-thumb)*scroll/layout.maxScroll(count);
        rounded(context,track.x(),y,track.width(),thumb,0xFF8F99A2);
    }

    private void scrollTo(int value) {
        int next=Math.clamp(value,0,layout.maxScroll(filtered().size()));
        if(next!=scroll){scroll=next;rebuild();}
    }
    private void dragScroll(double mouseY) {
        var track=layout.scrollbar;
        scrollTo((int)((mouseY-track.y())/track.height()*layout.maxScroll(filtered().size())));
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(animation.closing())return true;
        y-=animation.offset();
        if(layout.panel.contains(x,y)){scrollTo(scroll-(int)Math.round(vertical*30));return true;}
        return false;
    }
    @Override public boolean mouseClicked(Click click,boolean doubled) {
        if(animation.closing())return true;
        click=animatedClick(click);
        var track=layout.scrollbar;
        if(click.button()==0 && click.x()>=track.x()-2 && click.x()<track.right()+3 && click.y()>=track.y() && click.y()<track.bottom()) {
            draggingScrollbar=true;dragScroll(click.y());return true;
        }
        return super.mouseClicked(click,doubled);
    }
    @Override public boolean mouseDragged(Click click,double dx,double dy) {
        click=animatedClick(click);
        if(draggingScrollbar){dragScroll(click.y());return true;}
        return super.mouseDragged(click,dx,dy);
    }
    @Override public boolean mouseReleased(Click click) {
        draggingScrollbar=false;
        return super.mouseReleased(animatedClick(click));
    }

    private Click animatedClick(Click click) { return new Click(click.x(),click.y()-animation.offset(),click.buttonInfo()); }

    @Override public boolean keyPressed(KeyInput input) {
        if(animation.closing())return true;
        if(!search.isFocused()&&QuickMenuScreen.isMenuKey(input)){close();return true;}
        if (!search.isFocused()) {
            switch(input.key()) {
                case GLFW.GLFW_KEY_PAGE_DOWN -> { scrollTo(scroll+layout.grid.height()); return true; }
                case GLFW.GLFW_KEY_PAGE_UP -> { scrollTo(scroll-layout.grid.height()); return true; }
                case GLFW.GLFW_KEY_HOME -> { scrollTo(0); return true; }
                case GLFW.GLFW_KEY_END -> { scrollTo(layout.maxScroll(filtered().size())); return true; }
                default -> { }
            }
        }
        return super.keyPressed(input);
    }

    private final class MenuButton extends ButtonWidget {
        private final String icon;
        private final boolean selected,clipped;
        private final ModMenuEntry entry;
        private final ToggleAnimation toggleAnimation;
        private float hoverProgress;
        private long lastFrame=System.nanoTime();
        MenuButton(String label,ModMenuLayout.Rect r,String icon,Runnable action,boolean selected,ModMenuEntry entry,boolean clipped) {
            super(r.x(),r.y(),r.width(),r.height(),text(label),ignored->action.run(),DEFAULT_NARRATION_SUPPLIER);
            this.icon=icon;this.selected=selected;this.entry=entry;this.clipped=clipped;
            toggleAnimation=icon.equals("toggle")?toggleAnimations.computeIfAbsent(entry,key->new ToggleAnimation(key.isEnabled())):null;
            if(toggleAnimation!=null)toggleAnimation.value(entry.isEnabled());
        }
        @Override public void playDownSound(SoundManager soundManager) {
            AxialUiTheme.playButtonClickSound();
        }

        @Override public boolean isMouseOver(double x,double y) {
            return (!clipped || layout.grid.contains(x,y)) && super.isMouseOver(x,y);
        }
        @Override protected void drawIcon(DrawContext context,int mouseX,int mouseY,float delta) {
            boolean hover=isHovered()||isFocused();
            long now=System.nanoTime();
            hoverProgress=MenuAnimation.hover(hoverProgress,hover,(now-lastFrame)/1_000_000_000f);
            lastFrame=now;
            var bounds=new ModMenuLayout.Rect(getX(),getY(),width,height);
            if(icon.equals("toggle")) {
                boolean on=entry.isEnabled();
                float progress=toggleAnimation.value(on);
                int color=mixColor(RED,GREEN,progress);
                // The state is present in narration as well as in color and knob position.
                setMessage(text("Toggle "+entry.title()+": "+(on?"On":"Off")));
                ModMenuUi.toggle(context,getX(),getY()+1,27,14,12,progress,0xFFFFFFFF);
                label(context,textRenderer,on?"On":"Off",getX()+33,getY()+4,color,.85f,width-35,false);
                if(hover) context.drawStrokedRectangle(getX()-2,getY()-1,Math.min(width,60),height+2,0xFFA9B3BC);
                return;
            }
            boolean close=icon.equals("close"),gear=icon.equals("gear")&&clipped;
            if(selected||hoverProgress>.01f||gear)frame(context,bounds,selected?SELECTED_LINE:mixColor(LINE,0xFF7C8791,hoverProgress),selected?SELECTED:mixColor(0xFF20262C,HOVER,hoverProgress));
            if(close||gear){ModMenuUi.icon(context,icon,getX()+(width-12)/2,getY()+(height-12)/2,12,12,hover?INK:MUTED);return;}
            if(icon.equals("filter")) {
                int square=10;
                frame(context,new ModMenuLayout.Rect(getX()+3,getY()+(height-square)/2,square,square),selected?GREEN:MUTED,FIELD);
                if(selected)ModMenuUi.icon(context,"check",getX()+4,getY()+(height-8)/2,8,8,GREEN);
                label(context,textRenderer,layout.sidebar?"Enabled only":"On only",getX()+18,getY()+(height-8)/2,INK,.8f,width-20,false);
                return;
            }
            boolean categoryButton=false;
            for(var value:ModMenuEntry.Category.values())if(getMessage().getString().equalsIgnoreCase(value.label))categoryButton=true;
            if(categoryButton&&!layout.sidebar){ModMenuUi.icon(context,icon,getX()+(width-13)/2,getY()+(height-13)/2,13,13,INK);return;}
            if(!categoryButton && width<85) {
                label(context,textRenderer,getMessage().getString(),getX()+5,getY()+(height-9)/2,INK,.8f,width-10,false);
                return;
            }
            ModMenuUi.icon(context,icon,getX()+8,getY()+(height-13)/2,13,13,INK);
            label(context,textRenderer,getMessage().getString(),getX()+27,getY()+(height-9)/2,INK,.9f,width-30,false);
        }
    }
}
