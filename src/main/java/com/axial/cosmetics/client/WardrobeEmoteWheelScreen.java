package com.axial.cosmetics.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import com.axial.cosmetics.client.ModMenuLayout.Rect;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** Native wheel positions, index zero at the bottom, shown in their actual spatial order. */
public final class WardrobeEmoteWheelScreen extends NavigationScreen {
    private final WardrobeEmote emote;private int page,slot;private Rect panel;private String notice="";
    WardrobeEmoteWheelScreen(Screen parent,WardrobeEmote emote){super("Emote wheel",parent);this.emote=emote;}
    @Override protected void init(){rebuild();}
    private void rebuild() {
        clearChildren();int w=Math.min(470,width-16),h=Math.min(330,height-16);panel=new Rect((width-w)/2,(height-h)/2,w,h);
        for(int i=0;i<8;i++) {
            int index=i;var r=EmoteWheelLoadout.slotBounds(panel,i);
            var current=EmoteWheelLoadout.at(page,i);var managed=WardrobeEmote.fromAnimation(current);
            var name=managed!=null?managed.label:current==null?"Empty":"Assigned emote";
            var b=button((i+1)+" • "+name,r,()->{slot=index;notice="";rebuild();});b.selected=slot==i;b.setTooltip(Tooltip.of(text(name)));
        }
        button("< Page",new Rect(panel.x()+12,panel.y()+51,65,19),()->{page--;rebuild();}).active=page>0;
        button("Page >",new Rect(panel.right()-77,panel.y()+51,65,19),()->{page++;rebuild();}).active=page+1<EmoteWheelLoadout.pages();
        int y=panel.bottom()-29;
        button("Back",new Rect(panel.x()+12,y,60,20),this::close);
        button("Clear slot",new Rect(panel.x()+78,y,86,20),()->{EmoteWheelLoadout.clear(page,slot);notice="Slot cleared";rebuild();});
        var assign=button("Assign here",new Rect(panel.right()-126,y,114,20),()->{EmoteWheelLoadout.assign(emote,page,slot);notice="Saved to wheel "+(page+1)+", slot "+(slot+1);rebuild();});assign.primary=true;
    }
    @Override public boolean shouldPause(){return false;}
    @Override public void render(DrawContext c,int mx,int my,float delta) {
        c.fill(0,0,width,height,0xA0080A0D);frame(c,panel,PANEL_BORDER,PANEL);
        MenuHeader.draw(c,textRenderer,new Rect(panel.x()+12,panel.y()+4,panel.width()-24,28),"EMOTE WHEEL");
        line(c,emote.label+" • Choose a position",panel.x()+12,panel.y()+36,INK,.8f,panel.width()-24,true);
        line(c,"Wheel "+(page+1)+" / "+EmoteWheelLoadout.pages(),panel.x()+83,panel.y()+57,MUTED,.75f,panel.width()-166,true);
        line(c,error.isEmpty()?notice:error,panel.x()+12,panel.bottom()-47,error.isEmpty()?0xFFB5F5CB:RED,.65f,panel.width()-24,true);
        super.render(c,mx,my,delta);
    }
}
