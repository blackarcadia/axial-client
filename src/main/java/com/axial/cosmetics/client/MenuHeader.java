package com.axial.cosmetics.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import com.axial.cosmetics.client.ModMenuLayout.Rect;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** Shared menu branding. The quick bar deliberately retains its independent logo. */
public final class MenuHeader {
    private static final Identifier MARK=Identifier.of("axial_cosmetics","textures/gui/branding/menu_mark.png");
    private static final String BRAND_NAME="AXIALCLIENT";
    private MenuHeader(){}
    public static void draw(DrawContext c,TextRenderer font,int x,int y,int width,int height,String context){
        draw(c,font,new Rect(x,y,Math.max(1,width),Math.max(1,height)),context);
    }
    static void draw(DrawContext c,TextRenderer font,Rect area,String context){
        if(area.width()<12||area.height()<8)return;
        var layout=new MenuHeaderLayout(area);var mark=layout.mark;
        c.drawTexture(RenderPipelines.GUI_TEXTURED,MARK,mark.x(),mark.y(),0,0,mark.width(),mark.height(),512,302,512,302,tint(0xFFFFFFFF));
        float scale=1f;
        for(float candidate:new float[]{1f,.8f}){
            scale=candidate;
            if(UiTypography.width(font,BRAND_NAME,candidate,true)<=layout.textWidth&&UiTypography.size(candidate)<=mark.height())break;
        }
        int brandWidth=UiTypography.width(font,BRAND_NAME,scale,true);
        if(brandWidth>layout.textWidth&&!context.isBlank()){
            // Preserve the submenu's identity when a very narrow header cannot fit the wordmark.
            label(c,font,context,layout.textX,layout.textY(8),INK,.8f,layout.textWidth,true);return;
        }
        label(c,font,BRAND_NAME,layout.textX,layout.textY(UiTypography.size(scale)),BRAND,scale,layout.textWidth,true);
        int remaining=area.right()-(layout.textX+brandWidth+16);
        if(!context.isBlank()&&remaining>=24){
            int measured=Math.min(remaining,UiTypography.width(font,context,.8f,false));
            label(c,font,context,area.right()-measured,layout.textY(8),MUTED,.8f,remaining,false);
        }
    }
}
