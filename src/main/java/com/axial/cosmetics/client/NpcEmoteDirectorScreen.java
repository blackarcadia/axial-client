package com.axial.cosmetics.client;

import io.github.kosmx.emotes.main.EmoteHolder;
import java.util.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import static com.axial.cosmetics.client.ModMenuUi.*;
import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** Explicit actor selection and emote assignment; all controls use the client's shared theme. */
public final class NpcEmoteDirectorScreen extends NavigationScreen {
    private Rect panel,searchBounds;private TextFieldWidget search;
    private UUID actor,emote;private int actorPage,emotePage;private boolean loop=true;
    private String query="";
    public NpcEmoteDirectorScreen(Screen parent){super("NPC Emotes",parent);}
    @Override protected void init() {
        if(actor==null&&client.targetedEntity instanceof net.minecraft.client.network.AbstractClientPlayerEntity p&&p!=client.player)actor=p.getUuid();
        rebuild();
    }
    private void rebuild() {
        boolean focused=search!=null&&search.isFocused();int cursor=search==null?query.length():search.getCursor();
        clearChildren();int w=Math.min(560,width-16),h=Math.min(330,height-16);
        panel=new Rect((width-w)/2,(height-h)/2,w,h);int gap=10,col=(w-42)/2,left=panel.x()+16,right=left+col+gap;
        int top=panel.y()+82;int rows=Math.max(1,(h-164)/22);
        var actors=NpcEmoteDirector.actors();var emotes=NpcEmoteDirector.emotes(query);
        actorPage=Math.clamp(actorPage,0,Math.max(0,(actors.size()-1)/rows));emotePage=Math.clamp(emotePage,0,Math.max(0,(emotes.size()-1)/rows));
        button("Refresh actors",new Rect(left,panel.y()+54,col/2-2,20),this::rebuild);
        button("Clear actor",new Rect(left+col/2+2,panel.y()+54,col-col/2-2,20),()->{NpcEmoteDirector.clear(actor);rebuild();}).active=actor!=null&&NpcEmoteDirector.editable();
        searchBounds=new Rect(right,panel.y()+54,col,20);search=field("Find emote...",query,searchBounds,80);
        search.setCursor(Math.min(cursor,query.length()),false);
        search.setChangedListener(value->{query=value;emotePage=0;rebuild();setFocused(search);});if(focused)setFocused(search);
        for(int row=0;row<rows;row++) {
            int ai=actorPage*rows+row,ei=emotePage*rows+row;
            if(ai<actors.size()){
                var a=actors.get(ai);var b=button((NpcEmoteDirector.assigned(a.id())?"• ":"")+a.name(),new Rect(left,top+row*22,col,20),()->{actor=a.id();rebuild();});
                b.selected=a.id().equals(actor);b.setTooltip(Tooltip.of(text(a.name()+" • "+a.id())));
            }
            if(ei<emotes.size()){
                var e=emotes.get(ei);var b=button(e.name.getString(),new Rect(right,top+row*22,col,20),()->{emote=e.getUuid();rebuild();});b.selected=e.getUuid().equals(emote);
                b.setTooltip(Tooltip.of(text(e.name.getString())));
            }
        }
        int pagesY=top+rows*22+3;
        button("<",new Rect(left,pagesY,24,18),()->{actorPage--;rebuild();}).active=actorPage>0;
        button(">",new Rect(left+col-24,pagesY,24,18),()->{actorPage++;rebuild();}).active=(actorPage+1)*rows<actors.size();
        button("<",new Rect(right,pagesY,24,18),()->{emotePage--;rebuild();}).active=emotePage>0;
        button(">",new Rect(right+col-24,pagesY,24,18),()->{emotePage++;rebuild();}).active=(emotePage+1)*rows<emotes.size();
        var assign=button("Assign & play",new Rect(left,panel.bottom()-54,col,20),()->{
            var selected=EmoteHolder.getEmoteFromUuid(emote);if(selected==null)throw new IllegalStateException("Select an available emote");
            NpcEmoteDirector.assign(actor,selected,loop);rebuild();
        });assign.primary=true;assign.active=actor!=null&&emote!=null&&NpcEmoteDirector.editable();
        button("Loop: "+(loop?"On":"Off"),new Rect(right,panel.bottom()-54,col,20),()->{loop=!loop;rebuild();}).selected=loop;
        button("Back",new Rect(left,panel.bottom()-28,58,20),this::close);
        int actionsX=left+64,actionsWidth=(panel.right()-16-actionsX-6)/2;
        button("Start all together",new Rect(actionsX,panel.bottom()-28,actionsWidth,20),()->{NpcEmoteDirector.startAll();rebuild();}).active=NpcEmoteDirector.assignedCount()>0&&NpcEmoteDirector.editable();
        button("Stop all",new Rect(actionsX+actionsWidth+6,panel.bottom()-28,actionsWidth,20),()->{NpcEmoteDirector.stopAll();rebuild();}).active=NpcEmoteDirector.assignedCount()>0&&NpcEmoteDirector.editable();
    }
    @Override public boolean shouldPause(){return false;}
    @Override public void render(DrawContext c,int mx,int my,float delta) {
        c.fill(0,0,width,height,0x8C080A0D);frame(c,panel,PANEL_BORDER,PANEL);
        MenuHeader.draw(c,textRenderer,new Rect(panel.x()+12,panel.y()+3,panel.width()-24,28),"NPC Emotes");
        line(c,error.isEmpty()?NpcEmoteDirector.status():error,panel.x()+16,panel.y()+35,error.isEmpty()?MUTED:RED,.7f,panel.width()-32,false);
        inputFrame(c,search,searchBounds);super.render(c,mx,my,delta);
        int col=(panel.width()-42)/2,rows=Math.max(1,(panel.height()-164)/22),y=panel.y()+82+rows*22+8;
        var actors=NpcEmoteDirector.actors();var emotes=NpcEmoteDirector.emotes(query);
        line(c,actors.isEmpty()?"No nearby player-type NPCs":"Actors "+(actorPage+1)+" / "+Math.max(1,(actors.size()+rows-1)/rows),panel.x()+45,y,MUTED,.65f,col-58,false);
        line(c,emotes.isEmpty()?"No matching emotes":"Emotes "+(emotePage+1)+" / "+Math.max(1,(emotes.size()+rows-1)/rows),searchBounds.x()+29,y,MUTED,.65f,col-58,false);
    }
}
