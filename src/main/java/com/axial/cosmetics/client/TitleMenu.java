package com.axial.cosmetics.client;

import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import org.axial.axialutils.client.AxialUiTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.axial.cosmetics.client.ModMenuUi.*;
import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** Native title-screen controls, kept on TitleScreen for normal music and account-modal behaviour. */
public final class TitleMenu {
    private record Bulletin(Identifier texture,int width,int height) {}
    private static Bulletin bulletin(String name,int width) {
        return new Bulletin(Identifier.of("axial_cosmetics","textures/gui/title/bulletins/"+name+".png"),width,941);
    }
    private static final List<Bulletin> PRISONS=List.of(bulletin("prisons-halloween",1671),bulletin("prisons-spears",1672),bulletin("prisons-cells",1672));
    private static final Bulletin SKY=bulletin("sky-coming-soon",1670);
    private static final Identifier BACKGROUND=Identifier.of("axial_cosmetics","textures/gui/title/sub_menu_background.png");
    private static final Identifier LOGO=Identifier.of("axialutils","textures/gui/menu_logo.png");
    private static final int PANEL_FILL=0x3014171B,PANEL_LINE=0x60454C53,BUTTON_FILL=0x48202429;
    private final MinecraftClient client=MinecraftClient.getInstance();
    private final TitleScreen screen;
    private final List<ClickableWidget> widgets=new ArrayList<>();
    private final BulletinCarousel carousel=new BulletinCarousel(PRISONS.size());
    private TitleMenuLayout layout;
    private Supplier<SkinTextures> skin;

    public TitleMenu(TitleScreen screen){this.screen=screen;}
    public void init(Consumer<ClickableWidget> add) {
        layout=new TitleMenuLayout(screen.width,screen.height);
        skin=client.getSkinProvider().supplySkinTextures(client.getGameProfile(),false);
        widgets.clear();carousel.synchronizeClock();
        add(button("SINGLEPLAYER",layout.nav(0),"mountain",()->client.setScreen(new SelectWorldScreen(screen))),add);
        add(button("MULTIPLAYER",layout.nav(1),"globe",()->client.setScreen(client.options.skipMultiplayerWarning?new MultiplayerScreen(screen):new MultiplayerWarningScreen(screen))),add);
        add(button("MOD MENU",layout.nav(2),"cube",()->ModMenuScreen.openFromTitle(screen)),add);
        add(button("OPTIONS",layout.nav(3),"gear",()->client.setScreen(new OptionsScreen(screen,client.options))),add);
        add(button("MANAGE ACCOUNTS",layout.account,"account",()->client.setScreen(new AccountsScreen(screen))),add);
        add(button("QUIT",layout.quit,"power",client::scheduleStop),add);
        add(button("CONNECT TO AXIAL PRISONS",layout.prisonsConnect,"connect",()->connect("Axial Prisons","mc.axialprisons.com:25565")),add);
        add(button("CONNECT TO AXIAL SKY",layout.skyConnect,"connect",()->connect("Axial Sky","37.230.138.62:25603")),add);
        add(button("PREVIOUS PRISONS BULLETIN",layout.previous(),"previous",()->carousel.step(-1)),add);
        add(button("NEXT PRISONS BULLETIN",layout.next(),"next",()->carousel.step(1)),add);
        for(int i=0;i<PRISONS.size();i++) {
            int page=i;var button=button("PRISONS BULLETIN "+(i+1),layout.dot(i),"dot",()->carousel.select(page));
            button.page=i;add(button,add);
        }
        add(new MusicSlider(layout.music),add);
    }
    private void add(ClickableWidget widget,Consumer<ClickableWidget> add){widgets.add(widget);add.accept(widget);}
    private TitleButton button(String label,Rect r,String icon,Runnable action) {
        var b=new TitleButton(label,r,icon,action);
        if(icon.equals("power")||icon.equals("previous")||icon.equals("next")||icon.equals("dot"))b.setTooltip(Tooltip.of(text(label)));
        return b;
    }
    private void connect(String name,String address) {
        ConnectScreen.connect(screen,client,ServerAddress.parse(address),new ServerInfo(name,address,ServerInfo.ServerType.OTHER),false,null);
    }

    public void render(DrawContext c,int mouseX,int mouseY,float delta) {
        int width=screen.width,height=screen.height;
        float scale=Math.max(width/1717f,height/916f);
        int bgWidth=(int)Math.ceil(1717*scale),bgHeight=(int)Math.ceil(916*scale);
        c.drawTexture(RenderPipelines.GUI_TEXTURED,BACKGROUND,(width-bgWidth)/2,(height-bgHeight)/2,0,0,bgWidth,bgHeight,1717,916,1717,916);
        // The original background without baked-in branding allows the logo to keep its aspect ratio.
        int logoWidth=layout.compact?Math.min(100,width/5):Math.min(180,width/4),logoHeight=logoWidth*117/240;
        c.drawTexture(RenderPipelines.GUI_TEXTURED,LOGO,(width-logoWidth)/2,layout.compact?5:16,0,0,logoWidth,logoHeight,240,117,240,117);
        label(c,client.textRenderer,"AXIAL CLIENT",layout.navigation.x(),16,INK,layout.compact?.75f:.85f,layout.compact?100:150,true);
        translucentFrame(c,layout.navigation);translucentFrame(c,layout.gamemodes);
        heading(c,"PLAY",layout.navigation.x()+10,layout.playHeading(),layout.navigation.width()-20);
        heading(c,"CLIENT",layout.navigation.x()+10,layout.clientHeading(),layout.navigation.width()-20);
        heading(c,"OFFICIAL GAMEMODES",layout.gamemodes.x()+9,layout.gamemodes.y()+8,layout.gamemodes.width()-18);
        carousel.tick(client.currentScreen!=screen||layout.prisonsImage.contains(mouseX,mouseY)||layout.pages.contains(mouseX,mouseY));
        drawBulletin(c,PRISONS.get(carousel.current()),layout.prisonsImage,1);
        if(carousel.progress()>0)drawBulletin(c,PRISONS.get(carousel.target()),layout.prisonsImage,carousel.progress());
        drawBulletin(c,SKY,layout.skyImage,1);
        String version="MINECRAFT "+SharedConstants.getGameVersion().name();
        int textWidth=client.textRenderer.getWidth(UiTypography.text(version,.75f,false));
        label(c,client.textRenderer,version,width-layout.navigation.x()-textWidth,height-22,MUTED,.75f,textWidth,false);
        int nameWidth=client.textRenderer.getWidth(UiTypography.text("AXIAL CLIENT",.75f,true));
        label(c,client.textRenderer,"AXIAL CLIENT",width-layout.navigation.x()-nameWidth,height-11,INK,.75f,nameWidth,true);
        for(var widget:widgets)widget.render(c,mouseX,mouseY,delta);
    }
    private void heading(DrawContext c,String text,int x,int y,int maxWidth) {
        label(c,client.textRenderer,text,x,y,MUTED,.78f,maxWidth,true);
        int end=x+client.textRenderer.getWidth(UiTypography.text(text,.78f,true))+7;
        if(end<x+maxWidth)c.fill(end,y+4,x+maxWidth,y+5,0x50454C53);
    }
    private static void translucentFrame(DrawContext c,Rect r){frame(c,r,PANEL_LINE,PANEL_FILL);}
    private static void drawBulletin(DrawContext c,Bulletin bulletin,Rect area,float opacity) {
        Rect image=TitleMenuLayout.contain(area,bulletin.width(),bulletin.height());
        c.drawTexture(RenderPipelines.GUI_TEXTURED,bulletin.texture(),image.x(),image.y(),
                0,0,image.width(),image.height(),bulletin.width(),bulletin.height(),bulletin.width(),bulletin.height(),Math.round(opacity*255)<<24|0xFFFFFF);
    }

    private final class TitleButton extends ButtonWidget {
        final String icon,label;int page=-1;float hoverProgress;long lastFrame=System.nanoTime();
        TitleButton(String label,Rect r,String icon,Runnable action) {
            super(r.x(),r.y(),r.width(),r.height(),text(label),ignored->action.run(),DEFAULT_NARRATION_SUPPLIER);this.icon=icon;this.label=label;
        }
        @Override public void playDownSound(SoundManager sounds){AxialUiTheme.playButtonClickSound();}
        @Override protected void drawIcon(DrawContext c,int mx,int my,float delta) {
            boolean hover=isHovered()||isFocused();long now=System.nanoTime();
            hoverProgress=MenuAnimation.hover(hoverProgress,hover,(now-lastFrame)/1_000_000_000f);lastFrame=now;
            Rect r=new Rect(getX(),getY(),width,height);
            if(icon.equals("dot")) {
                circle(c,r.x()+(r.width()-4)/2,r.y()+(r.height()-4)/2,4,page==carousel.selected()?GREEN:hover?INK:MUTED);return;
            }
            if(icon.equals("previous")||icon.equals("next")) {
                c.drawGuiTexture(RenderPipelines.GUI_TEXTURED,Identifier.of("axial_cosmetics","accounts/"+icon),r.x()+(r.width()-8)/2,r.y()+(r.height()-8)/2,8,8,hover?INK:MUTED);return;
            }
            int fill=(Math.round(72+hoverProgress*34)<<24)|0x202429;
            frame(c,r,hover?0xB07C858E:PANEL_LINE,fill);
            if(icon.equals("power")){ModMenuUi.icon(c,"power",r.x()+5,r.y()+5,14,14,hover?RED:INK);return;}
            if(icon.equals("connect")) {
                String server=label.contains("PRISONS")?"AXIAL PRISONS":"AXIAL SKY";
                int connectWidth=layout.compact?50:64;
                label(c,client.textRenderer,server,r.x()+5,r.y()+(height-8)/2,INK,layout.compact?.7f:.8f,r.width()-connectWidth-10,true);
                Rect connect=new Rect(r.right()-connectWidth-3,r.y()+3,connectWidth,r.height()-6);
                frame(c,connect,hover?GREEN:0x8046D57D,0x3014171B);
                label(c,client.textRenderer,"CONNECT",connect.x()+5,connect.y()+(connect.height()-7)/2,GREEN,.7f,connect.width()-10,true);return;
            }
            if(icon.equals("account")) {
                var texture=skin.get().body().texturePath();
                c.drawTexture(RenderPipelines.GUI_TEXTURED,texture,r.x()+5,r.y()+5,8,8,14,14,8,8,64,64);
                c.drawTexture(RenderPipelines.GUI_TEXTURED,texture,r.x()+5,r.y()+5,40,8,14,14,8,8,64,64);
                label(c,client.textRenderer,AccountSessions.isSignedIn()?client.getSession().getUsername():"SIGNED OUT",r.x()+25,r.y()+(layout.compact?8:4),INK,.78f,r.width()-38,true);
                if(!layout.compact)label(c,client.textRenderer,"MANAGE ACCOUNTS",r.x()+25,r.y()+14,MUTED,.7f,r.width()-32,false);
                c.drawGuiTexture(RenderPipelines.GUI_TEXTURED,Identifier.of("axial_cosmetics","accounts/next"),r.right()-10,r.y()+9,6,6,MUTED);return;
            }
            if(hoverProgress>.01f)rounded(c,r.x()+1,r.y()+3,2,r.height()-6,(Math.round(hoverProgress*255)<<24)|(GREEN&0xFFFFFF));
            int size=layout.compact?12:16;
            ModMenuUi.icon(c,icon,r.x()+9,r.y()+(height-size)/2,size,size,INK);
            label(c,client.textRenderer,label,r.x()+size+18,r.y()+(height-9)/2,INK,layout.compact?.8f:1,r.width()-size-24,true);
        }
    }
    private final class MusicSlider extends SliderWidget {
        MusicSlider(Rect r){super(r.x(),r.y(),r.width(),r.height(),text("MENU MUSIC"),MenuMusicConfig.volume());updateMessage();}
        @Override protected void updateMessage(){setMessage(text("MENU MUSIC: "+Math.round(value*100)+"%"));}
        @Override protected void applyValue(){MenuMusicConfig.setVolume((float)value);updateMessage();}
        @Override public void playDownSound(SoundManager sounds){AxialUiTheme.playButtonClickSound();}
        @Override public void onRelease(Click click){}
        @Override public void renderWidget(DrawContext c,int mx,int my,float delta) {
            Rect r=new Rect(getX(),getY(),width,height);translucentFrame(c,r);
            ModMenuUi.icon(c,"music",r.x()+5,r.y()+4,11,11,INK);
            label(c,client.textRenderer,"MENU MUSIC",r.x()+22,r.y()+6,MUTED,.7f,60,false);
            int x=r.x()+85,w=r.width()-104,handle=x+(int)Math.round(value*(w-6));
            rounded(c,x,r.y()+8,w,3,DIVIDER);rounded(c,x,r.y()+8,Math.max(3,handle-x+3),3,INK);circle(c,handle,r.y()+6,6,INK);
            ModMenuUi.icon(c,"speaker",r.right()-14,r.y()+4,11,11,MUTED);
        }
        @Override public void onClick(Click click,boolean doubled){setFromPosition(click.x());}
        @Override protected void onDrag(Click click,double dx,double dy){setFromPosition(click.x());}
        private void setFromPosition(double x){setValue((x-getX()-88)/Math.max(1,width-110));}
    }
}
