package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.axial.axialutils.client.AxialUiTheme;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import static com.axial.cosmetics.client.ModMenuUi.*;
import com.axial.cosmetics.client.ModMenuLayout.Rect;
import com.axial.cosmetics.client.ServerBoardLayout.Area;

/** Official-server notices with independent browser and connection actions. */
public final class ServerBulletinBoards {
    private static final Identifier PRISONS_EMBLEM=Identifier.of("axial_cosmetics","textures/gui/title/bulletins/prisons-emblem.png");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private ServerUpdateFeed.State displayed = new ServerUpdateFeed.State(null, "Loading latest update…");
    private float prisonsHover,skyHover,updateHover;
    private long lastFrame=System.nanoTime();
    public void synchronizeClock(){lastFrame=System.nanoTime();}

    public void render(DrawContext c,int screenWidth,int screenHeight,int mouseX,int mouseY,boolean active,String count){
        var layout=new ServerBoardLayout(screenWidth,screenHeight);
        long now=System.nanoTime();float seconds=(now-lastFrame)/1_000_000_000f;lastFrame=now;
        prisonsHover=MenuAnimation.hover(prisonsHover,active&&layout.prisons.contains(mouseX,mouseY),seconds);
        skyHover=MenuAnimation.hover(skyHover,active&&layout.sky.contains(mouseX,mouseY),seconds);
        updateHover=MenuAnimation.hover(updateHover,active&&ServerBoardLayout.prisonsNotice(layout.prisons).contains(mouseX,mouseY),seconds);
        displayed=ServerUpdateFeed.current();
        var font=MinecraftClient.getInstance().textRenderer;
        label(c,font,"OFFICIAL SERVERS",layout.headingX,layout.headingY,INK,.85f,layout.prisons.width(),true);
        board(c,layout.prisons,prisonsHover,"AXIAL PRISONS",count,true);
        board(c,layout.sky,skyHover,"AXIAL SKY","Coming soon",false);
    }
    private void board(DrawContext c,Area board,float hover,String title,String status,boolean prisons){
        var font=MinecraftClient.getInstance().textRenderer;Rect r=rect(board);
        rounded(c,r.x(),r.y()+3,r.width(),r.height(),0x66000000);
        frame(c,r,mixColor(LINE,BRAND,hover),0xF0171B22);
        boolean compact=r.height()<(prisons?152:120);
        if(prisons){
            Area logo=ServerBoardLayout.prisonsLogo(board);
            c.drawTexture(RenderPipelines.GUI_TEXTURED,PRISONS_EMBLEM,logo.x(),logo.y(),0,0,
                    logo.width(),logo.height(),2172,724,2172,724);
        }else label(c,font,title,r.x()+9,r.y()+7,INK,compact?.7f:.85f,r.width()-18,true);
        if(!compact){
            int statusColor=status.equals("Offline")?RED:status.equals("Loading")||!prisons?MUTED:GREEN;
            int statusY=r.y()+(prisons?45:22);
            circle(c,r.x()+9,statusY,4,statusColor);
            label(c,font,status,r.x()+17,statusY-1,statusColor,.7f,r.width()-26,false);
        }
        Area notice=prisons?ServerBoardLayout.prisonsNotice(board):ServerBoardLayout.notice(board);
        c.enableScissor(notice.x(),notice.y(),notice.right(),notice.bottom());
        if(prisons)rounded(c,notice.x(),notice.y(),notice.width(),notice.height(),mixColor(0xFF20232D,0xFF32273F,updateHover));
        int padding=compact?3:7, x=notice.x()+padding, y=notice.y()+padding;
        int textWidth=notice.width()-padding*2;
        String badge=prisons?"LATEST UPDATE":"COMING SOON";
        int badgeWidth=Math.min(textWidth,font.getWidth(UiTypography.text(badge,.55f,true))+8);
        rounded(c,x,y,badgeWidth,10,prisons?0xFF694488:0xFF343743);
        label(c,font,badge,x+4,y+2,INK,.55f,badgeWidth-8,true);
        int titleY=y+13,lineHeight=compact?9:12;
        int reserved=compact?0:17;
        int lines=Math.max(1,(notice.bottom()-padding-reserved-titleY)/lineHeight);
        String copy=prisons?(displayed.update()==null?displayed.message():displayed.update().title()):"Skyblock is coming soon.";
        var wrapped=font.wrapLines(UiTypography.text(copy,compact?.65f:.85f,true),Math.max(1,textWidth));
        for(int i=0;i<Math.min(lines,wrapped.size());i++){
            if(i==lines-1&&wrapped.size()>lines){
                // Leave an explicit continuation marker when the smallest layouts cannot fit the title.
                c.enableScissor(x,titleY+i*lineHeight,Math.max(x,notice.right()-padding-9),titleY+(i+1)*lineHeight);
                c.drawText(font,wrapped.get(i),x,titleY+i*lineHeight,INK,false);
                c.disableScissor();
                label(c,font,"…",notice.right()-padding-8,titleY+i*lineHeight,INK,.7f,8,false);
            }else c.drawText(font,wrapped.get(i),x,titleY+i*lineHeight,prisons?INK:MUTED,false);
        }
        if(!compact&&prisons){
            String date=displayed.update()!=null&&displayed.update().date()!=null?DATE.format(displayed.update().date()):"Server updates";
            label(c,font,date,x,notice.bottom()-12,MUTED,.6f,Math.max(1,textWidth-65),false);
            label(c,font,"Read post →",notice.right()-padding-60,notice.bottom()-12,mixColor(MUTED,INK,updateHover),.65f,60,true);
        }
        c.disableScissor();
        int divider=notice.bottom();c.fill(r.x()+7,divider,r.right()-7,divider+1,DIVIDER);
        Area join=ServerBoardLayout.join(board);
        rounded(c,join.x(),join.y(),join.width(),join.height(),mixColor(0xFF30343F,0xFF765393,hover));
        String action=prisons?"Join Prisons":"Connect to Sky";
        label(c,font,action,join.x()+7,join.y()+(join.height()-7)/2,INK,.75f,join.width()-22,true);
        int arrowX=join.right()-13+Math.round(hover*2);
        c.drawGuiTexture(RenderPipelines.GUI_TEXTURED,Identifier.of("axial_cosmetics","accounts/next"),arrowX,join.y()+(join.height()-8)/2,8,8,INK);
    }

    /** The announcement opens its displayed post; it never falls through to joining the server. */
    public boolean mouseClicked(Click click,int screenWidth,int screenHeight){
        if(click.button()!=0)return false;
        Area notice=ServerBoardLayout.prisonsNotice(new ServerBoardLayout(screenWidth,screenHeight).prisons);
        if(!notice.contains(click.x(),click.y()))return false;
        AxialUiTheme.playButtonClickSound();
        Util.getOperatingSystem().open(displayed.update()==null?ServerUpdateFeed.FORUM:displayed.update().link());
        return true;
    }
    private static Rect rect(Area r){return new Rect(r.x(),r.y(),r.width(),r.height());}
}
