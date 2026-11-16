package com.axial.cosmetics.client;

import io.github.kosmx.emotes.main.EmoteHolder;
import net.minecraft.client.gui.DrawContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** One detached animated mannequin per native wheel slot; the wheel remains native. */
public final class EmoteWheelPreview {
    private final WardrobeEmotePreview animation=new WardrobeEmotePreview();
    static Rect bounds(int x,int y,int width,int height,int centerX,int centerY,boolean legacy,boolean hovered) {
        if(!legacy)return new Rect(x,y,width,height);
        int movedX=x+Math.round((centerX-(x+width/2))*.28f),movedY=y+Math.round((centerY-(y+height/2))*.28f);
        float scale=.60f*(hovered?1.10f:1);
        int w=Math.max(24,Math.round(width*scale)),h=Math.max(24,Math.round(height*scale));
        return new Rect(movedX+(width-w)/2,movedY+(height-h)/2,w,h);
    }
    public void draw(DrawContext context,EmoteHolder emote,int x,int y,int width,int height,int centerX,int centerY,boolean legacy,boolean hovered) {
        var r=bounds(x,y,width,height,centerX,centerY,legacy,hovered);
        var state=WardrobePreviewState.mannequin((System.nanoTime()/50_000_000L)%1_000_000,205);
        animation.attach(state,emote.emote);
        // Enough room for raised arms, kicks and falling poses inside the existing medallion.
        float scale=Math.max(1,Math.min(r.height()/2.9f,r.width()/2.4f));
        context.addEntity(state,scale,new Vector3f(0,.9f,0),new Quaternionf().rotateZ((float)Math.PI),new Quaternionf(),r.x(),r.y(),r.right(),r.bottom());
    }
}
