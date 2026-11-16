package com.axial.cosmetics.client;

import java.util.UUID;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import com.axial.cosmetics.client.ModMenuLayout.Rect;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** Spatial native wheel slots with immediate, persisted assignment and one-step undo. */
public final class WardrobeEmoteWheelScreen extends NavigationScreen {
    private record Edit(int page,int slot,UUID previous) {}
    private final WardrobeEmote emote;
    private int page,slot=-1;
    private Rect panel;
    private String notice="";
    private Edit undo;
    private long savedAt;
    private EmoteWheelPreview preview;
    WardrobeEmoteWheelScreen(Screen parent,WardrobeEmote emote){super("Emote wheel",parent);this.emote=emote;}
    @Override protected void init(){rebuild();}
    private void turnPage(int direction){page+=direction;slot=-1;notice="";rebuild();}
    private void assign(int index) {
        var previous=EmoteWheelLoadout.at(page,index);
        if(emote.animation.equals(previous)){slot=index;notice="Already assigned to slot "+(index+1);rebuild();return;}
        EmoteWheelLoadout.assign(emote,page,index);
        undo=new Edit(page,index,previous);slot=index;savedAt=System.nanoTime();
        notice="Saved to wheel "+(page+1)+" · slot "+(index+1);rebuild();
    }
    private void rebuild() {
        clearChildren();int w=Math.min(600,width-16),h=Math.min(380,height-16);panel=new Rect((width-w)/2,(height-h)/2,w,h);
        for(int i=0;i<8;i++) {
            var current=EmoteWheelLoadout.at(page,i);var managed=WardrobeEmote.fromAnimation(current);
            var holder=current==null?null:io.github.kosmx.emotes.main.EmoteHolder.getEmoteFromUuid(current);
            String name=managed!=null?managed.label:holder!=null?holder.name.getString():current==null?"Empty slot":"Assigned emote";
            var card=addDrawableChild(new SlotCard(i,name,current,EmoteWheelLoadout.slotBounds(panel,i)));
            card.setTooltip(Tooltip.of(text("Slot "+(i+1)+" · "+name+"\nClick to assign "+emote.label)));
        }
        button("<",new Rect(panel.x()+15,panel.y()+51,25,19),()->turnPage(-1)).active=page>0;
        button(">",new Rect(panel.x()+45,panel.y()+51,25,19),()->turnPage(1)).active=page+1<EmoteWheelLoadout.pages();
        int y=panel.bottom()-29;
        button("Back",new Rect(panel.x()+15,y,58,20),this::close);
        var clear=button("Clear selected",new Rect(panel.x()+79,y,100,20),()->{
            var previous=EmoteWheelLoadout.at(page,slot);EmoteWheelLoadout.clear(page,slot);undo=new Edit(page,slot,previous);
            notice="Slot "+(slot+1)+" cleared";savedAt=System.nanoTime();rebuild();
        });clear.active=slot>=0&&EmoteWheelLoadout.at(page,slot)!=null;
        button("Undo",new Rect(panel.right()-75,y,60,20),()->{
            var edit=undo;EmoteWheelLoadout.restore(edit.page(),edit.slot(),edit.previous());page=edit.page();slot=edit.slot();undo=null;
            notice="Previous slot restored";savedAt=System.nanoTime();rebuild();
        }).active=undo!=null;
    }
    private final class SlotCard extends NavButton {
        private final int index;private final String name;private final UUID assigned;
        private final UiMotion hover=new UiMotion(0);
        SlotCard(int index,String name,UUID assigned,Rect r){super("Slot "+(index+1)+": "+name+". Assign "+emote.label,r,()->assign(index));this.index=index;this.name=name;this.assigned=assigned;}
        @Override protected void drawContents(DrawContext c,int mx,int my,float delta) {
            boolean over=active&&(isHovered()||isFocused()),placed=emote.animation.equals(assigned);
            float motion=(float)hover.toward(over?1:0);
            var r=new Rect(getX(),getY(),width,height);
            frame(c,r,placed?BRAND:mixColor(LINE,HOVER_LINE,motion),mixColor(placed?0xFF292137:FIELD,HOVER,motion));
            c.fill(r.x()+1,r.y()+1,r.right()-1,r.y()+2,placed?BRAND:mixColor(0xFF34313E,BRAND,motion));
            boolean roomy=height>=34;
            if(roomy) {
                line(c,"SLOT "+(index+1),r.x()+9,r.y()+7,MUTED,.65f,r.width()-30,true);
                line(c,name,r.x()+9,r.y()+21,placed?0xFFDCC8FF:INK,.8f,r.width()-18,false);
                if(placed)icon(c,"check",r.right()-20,r.y()+7,10,10,0xFFB5F5CB);
            } else line(c,(index+1)+" · "+name,r.x()+7,r.y()+(r.height()-8)/2,placed?0xFFDCC8FF:INK,.65f,r.width()-14,false);
            if(slot==index)rounded(c,r.x()+r.width()/2-9,r.bottom()-3,18,2,BRAND);
        }
    }
    @Override public boolean shouldPause(){return false;}
    @Override public void render(DrawContext c,int mx,int my,float delta) {
        c.fill(0,0,width,height,0xB0080A0D);frame(c,panel,PANEL_BORDER,PANEL);
        MenuHeader.draw(c,textRenderer,new Rect(panel.x()+12,panel.y()+4,panel.width()-24,28),"EMOTE LOADOUT");
        line(c,emote.label,panel.x()+16,panel.y()+34,0xFFE5D4FF,.9f,panel.width()-32,true);
        line(c,"WHEEL "+(page+1)+" / "+EmoteWheelLoadout.pages(),panel.x()+82,panel.y()+57,MUTED,.65f,100,true);
        line(c,"Click a slot to assign",panel.x()+190,panel.y()+57,MUTED,.65f,panel.width()-205,false);
        c.fill(panel.x()+15,panel.y()+75,panel.right()-15,panel.y()+76,DIVIDER);
        var left=EmoteWheelLoadout.slotBounds(panel,6);var right=EmoteWheelLoadout.slotBounds(panel,2);
        int centerX=(left.right()+right.x())/2,centerY=left.y()+left.height()/2;
        var holder=emote.holder();
        if(panel.height()>=300&&holder!=null) {
            if(preview==null)preview=new EmoteWheelPreview();
            int cellHeight=(panel.height()-142)/3,h=cellHeight-6,w=Math.min(140,right.x()-left.right()-12);
            preview.draw(c,holder,centerX-w/2,centerY-h/2,w,h,centerX,centerY,false,false);
        } else {
            icon(c,"replay",centerX-9,centerY-15,18,18,BRAND);
            line(c,"CLICK TO SAVE",centerX-38,centerY+8,MUTED,.55f,76,true);
        }
        boolean recent=savedAt!=0&&(System.nanoTime()-savedAt)<2_000_000_000L;
        String status=!error.isEmpty()?error:notice.isEmpty()?"Changes save automatically":notice;
        line(c,status,panel.x()+15,panel.bottom()-46,!error.isEmpty()?RED:recent?0xFFB5F5CB:MUTED,.7f,panel.width()-30,false);
        c.fill(panel.x()+15,panel.bottom()-35,panel.right()-15,panel.bottom()-34,DIVIDER);
        super.render(c,mx,my,delta);
    }
}
