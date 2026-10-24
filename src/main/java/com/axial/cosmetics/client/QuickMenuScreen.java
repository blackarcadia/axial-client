package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.SoundManager;
import org.axial.axialutils.client.AxialUiTheme;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.slf4j.LoggerFactory;

import java.net.URI;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** Compact first-open launcher; the larger module library is one explicit action away. */
public final class QuickMenuScreen extends Screen {
    private final Screen backend,parent;
    private MenuAnimation animation=new MenuAnimation();
    private QuickMenuLayout layout;
    private String error="";
    private boolean reopen;

    private QuickMenuScreen(Screen backend,Screen parent) {
        super(text("AxialClient quick menu"));
        this.backend=backend;this.parent=parent;
    }

    public static void open(Screen backend) {
        try {
            if(!ModMenuCatalog.isMain(backend))return;
            Screen parent=ModMenuCatalog.parent(backend);
            if(parent instanceof QuickMenuScreen) ModMenuScreen.open(backend);
            else MinecraftClient.getInstance().setScreen(new QuickMenuScreen(backend,parent));
        } catch(ReflectiveOperationException ex) {
            LoggerFactory.getLogger(QuickMenuScreen.class).warn("Could not open quick menu; retaining original menu",ex);
        }
    }

    @Override protected void init() {
        if(reopen){animation=new MenuAnimation();reopen=false;}
        layout=new QuickMenuLayout(width,height);
        add("Accounts","account",0,()->client.setScreen(new AccountsScreen(this)),true);
        add("Waypoints","pin",1,()->ModFeatureSettings.open("WAYPOINTS",this,backend),true);
        add("Move HUD","move",2,()->client.setScreen(ModMenuCatalog.child(backend,this,"MOVE")),true);
        add("Mods","gear",3,()->client.setScreen(ModMenuCatalog.child(backend,this,"MAIN")),true);
        addLink("Store","store",4,QuickMenuLinks.get("store"));
        addLink("Forums","forums",5,QuickMenuLinks.get("forums"));
        addLink("Wiki","book",6,QuickMenuLinks.get("wiki"));
    }

    private void addLink(String label,String icon,int index,URI uri) {
        var button=add(label,icon,index,()->ConfirmLinkScreen.open(this,uri),uri!=null);
        if(uri==null)button.setTooltip(Tooltip.of(text(label+" is currently unavailable")));
    }

    private QuickButton add(String label,String icon,int index,Runnable action,boolean enabled) {
        var button=new QuickButton(label,icon,layout.button(index),action);
        button.active=enabled;
        button.setTooltip(Tooltip.of(text(label)));
        return addDrawableChild(button);
    }

    @Override public void close() { animation.close(); }
    @Override public void removed() { reopen=true; }
    @Override public void tick() {
        if(animation.finished())client.setScreen(parent);
    }

    static boolean isMenuKey(KeyInput input) {
        for(String id:new String[]{"key.axialutils.open_settings","key.axial_cosmetics.modmenu"}) {
            KeyBinding key=KeyBinding.byId(id);
            if(key!=null&&key.matchesKey(input))return true;
        }
        return false;
    }

    @Override public boolean keyPressed(KeyInput input) {
        if(animation.closing())return true;
        if(isMenuKey(input)){close();return true;}
        return super.keyPressed(input);
    }

    @Override public boolean mouseClicked(Click click,boolean doubled) {
        if(animation.closing())return true;
        return super.mouseClicked(new Click(click.x(),click.y()-animation.offset(),click.buttonInfo()),doubled);
    }

    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta) {
        float progress=animation.progress();
        float previous=opacity(progress);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0,animation.offset());
        try {
            var logo=layout.logo;
            context.drawTexture(RenderPipelines.GUI_TEXTURED,Identifier.of("axialutils","textures/gui/menu_logo.png"),
                    logo.x(),logo.y(),0,0,logo.width(),logo.height(),240,117,240,117,tint(0xFFFFFFFF));
            var bar=layout.bar;
            rounded(context,bar.x(),bar.y()+3,bar.width(),bar.height(),0x44000000);
            frame(context,bar,0xFF51575F,0xD9171B20);
            for(int i=1;i<QuickMenuLayout.ACTION_COUNT;i++) {
                int x=bar.x()+6+i*layout.tileWidth;
                context.fill(x,bar.y()+12,x+1,bar.bottom()-12,tint(0xFF3D444D));
            }
            super.render(context,mouseX,(int)(mouseY-animation.offset()),delta);
            String hint=error.isBlank()?"Esc to close":error;
            int textWidth=UiTypography.width(textRenderer,hint,.8f,false);
            label(context,textRenderer,hint,(width-textWidth)/2,layout.hintY,error.isBlank()?MUTED:RED,.8f,width-24,false);
        } finally {
            context.getMatrices().popMatrix();
            opacity(previous);
        }
    }

    private final class QuickButton extends ButtonWidget {
        private final String icon;
        private float hover;
        private long lastFrame=System.nanoTime();

        QuickButton(String label,String icon,ModMenuLayout.Rect r,Runnable action) {
            super(r.x(),r.y(),r.width(),r.height(),text(label),ignored->{
                try { error="";action.run(); }
                catch(RuntimeException ex) {
                    error="Could not open this option. Please try again.";
                    LoggerFactory.getLogger(QuickMenuScreen.class).warn("Quick menu action failed",ex);
                }
            },DEFAULT_NARRATION_SUPPLIER);
            this.icon=icon;
        }

        @Override public void playDownSound(SoundManager soundManager) {
            AxialUiTheme.playButtonClickSound();
        }

        @Override protected void drawIcon(DrawContext context,int mouseX,int mouseY,float delta) {
            long now=System.nanoTime();
            hover=MenuAnimation.hover(hover,active&&(isHovered()||isFocused()),(now-lastFrame)/1_000_000_000f);
            lastFrame=now;
            if(hover>.01f) {
                int alpha=Math.round(hover*255);
                frame(context,new ModMenuLayout.Rect(getX()+3,getY(),width-6,height),
                        (alpha<<24)|(icon.equals("gear")?0x86649C:0x65717D),(Math.round(hover*190)<<24)|0x343B45);
            }
            int size=layout.labels?22:20;
            int iconY=getY()+(layout.labels?7:(height-size)/2)-Math.round(hover*2);
            ModMenuUi.icon(context,icon,getX()+(width-size)/2,iconY,size,size,active?INK:0xFF727A84);
            if(layout.labels) {
                String label=getMessage().getString();
                int labelWidth=UiTypography.width(textRenderer,label,.8f,false);
                label(context,textRenderer,label,getX()+(width-labelWidth)/2,getBottom()-13,active?INK:MUTED,.8f,width-6,false);
            }
        }
    }
}
