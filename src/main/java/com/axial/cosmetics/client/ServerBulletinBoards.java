package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.axial.axialutils.client.AxialUiTheme;

import java.util.List;
import static com.axial.cosmetics.client.ModMenuUi.*;
import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** Only replaces the artwork within the existing official-server button rectangles. */
public final class ServerBulletinBoards {
    private record Image(Identifier texture,int width) {}
    private static Image image(String name,int width) {
        return new Image(Identifier.of("axial_cosmetics","textures/gui/title/bulletins/"+name+".png"),width);
    }
    private static final List<Image> PRISONS=List.of(image("prisons-halloween",1671),image("prisons-spears",1672),image("prisons-cells",1672));
    private static final Image SKY=image("sky-coming-soon",1670);
    private final BulletinCarousel carousel=new BulletinCarousel(PRISONS.size());

    public void synchronizeClock(){carousel.synchronizeClock();}

    public void render(DrawContext c,int x,int y,int width,int prisonsHeight,int skyHeight,int gap,int mouseX,int mouseY,boolean active) {
        Rect prisons=new Rect(x,y,width,prisonsHeight),sky=new Rect(x,y+prisonsHeight+gap,width,skyHeight);
        carousel.tick(!active||prisons.contains(mouseX,mouseY));
        rounded(c,x,y,width,prisonsHeight,0xC0101018);
        Rect picture=picture(prisons);
        drawImage(c,PRISONS.get(carousel.current()),picture,1);
        if(carousel.progress()>0)drawImage(c,PRISONS.get(carousel.target()),picture,carousel.progress());
        var renderer=MinecraftClient.getInstance().textRenderer;
        label(c,renderer,"AXIAL PRISONS",x+5,prisons.bottom()-10,INK,.75f,width-64,true);
        for(int i=0;i<3;i++) {
            Rect dot=dot(prisons,i);
            circle(c,dot.x()+2,dot.y()+4,4,i==carousel.selected()?GREEN:dot.contains(mouseX,mouseY)?INK:MUTED);
        }
        arrow(c,previous(prisons),"previous",mouseX,mouseY);
        arrow(c,next(prisons),"next",mouseX,mouseY);
        rounded(c,sky.x(),sky.y(),sky.width(),sky.height(),0xC0101018);
        drawImage(c,SKY,picture(sky),1);
        label(c,renderer,"AXIAL SKY",sky.x()+5,sky.bottom()-10,INK,.75f,width-10,true);
    }

    /** Bulletin navigation consumes its own clicks; the rest still uses the existing server connection action. */
    public boolean mouseClicked(Click click,int x,int y,int width,int height) {
        if(click.button()!=0)return false;
        Rect board=new Rect(x,y,width,height);
        if(previous(board).contains(click.x(),click.y()))carousel.step(-1);
        else if(next(board).contains(click.x(),click.y()))carousel.step(1);
        else {
            int selected=-1;
            for(int i=0;i<3;i++)if(dot(board,i).contains(click.x(),click.y()))selected=i;
            if(selected<0)return false;
            carousel.select(selected);
        }
        AxialUiTheme.playButtonClickSound();return true;
    }

    static Rect picture(Rect board){return new Rect(board.x()+1,board.y()+1,board.width()-2,board.height()-14);}
    static Rect previous(Rect board){return new Rect(board.right()-55,board.bottom()-13,10,12);}
    static Rect next(Rect board){return new Rect(board.right()-13,board.bottom()-13,10,12);}
    static Rect dot(Rect board,int page){return new Rect(board.right()-43+page*9,board.bottom()-13,8,12);}
    static Rect fit(Rect area,int imageWidth) {
        double scale=Math.min(area.width()/(double)imageWidth,area.height()/941.0);
        int w=Math.max(1,(int)Math.round(imageWidth*scale)),h=Math.max(1,(int)Math.round(941*scale));
        return new Rect(area.x()+(area.width()-w)/2,area.y()+(area.height()-h)/2,w,h);
    }
    private static void drawImage(DrawContext c,Image image,Rect area,float opacity) {
        Rect r=fit(area,image.width());
        c.drawTexture(RenderPipelines.GUI_TEXTURED,image.texture(),r.x(),r.y(),0,0,r.width(),r.height(),image.width(),941,image.width(),941,
                Math.round(opacity*255)<<24|0xFFFFFF);
    }
    private static void arrow(DrawContext c,Rect r,String name,int mx,int my) {
        c.drawGuiTexture(RenderPipelines.GUI_TEXTURED,Identifier.of("axial_cosmetics","accounts/"+name),r.x()+1,r.y()+2,8,8,r.contains(mx,my)?INK:MUTED);
    }
}
