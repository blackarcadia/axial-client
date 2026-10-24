package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import org.axial.axialutils.client.AxialUiTheme;
import org.lwjgl.glfw.GLFW;
import java.text.NumberFormat;
import java.util.*;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** Read-only public gang browser, sharing the native mod-menu shell and controls. */
public final class GangsScreen extends NavigationScreen {
    private static final int ROW=34,MEMBER_ROW=40;
    private final NumberFormat numbers=NumberFormat.getIntegerInstance(Locale.US),money=NumberFormat.getCurrencyInstance(Locale.US);
    private FeatureSettingsLayout layout;
    private Rect searchBox,viewport,stats;
    private TextFieldWidget search;
    private final List<GangButton> buttons=new ArrayList<>();
    private List<GangDirectory.Gang> rows=List.of();
    private GangDirectory.Gang selected;
    private String query="",selectedId;
    private long revision=-1;
    private int listScroll,memberScroll,columns;
    private boolean wide,compactDetails,dragging,reopen;
    private MenuAnimation animation=new MenuAnimation();
    GangsScreen(Screen parent){super("Gangs",parent);GangDirectoryGuide.open();}
    @Override protected void init(){
        if(reopen){animation=new MenuAnimation();reopen=false;}
        layout=new FeatureSettingsLayout(width,height,false);refresh();rebuild();
    }
    private void refresh(){
        revision=GangDirectoryGuide.catalogue.revision;
        selected=GangDirectoryGuide.catalogue.gangs.stream().filter(g->g.id().equals(selectedId)).findFirst().orElse(null);
        if(selected==null)selectedId=null;
        String q=query.strip().toLowerCase(Locale.ROOT);
        rows=GangDirectoryGuide.catalogue.gangs.stream().filter(g->(g.name()+" "+g.leaderName()).toLowerCase(Locale.ROOT).contains(q)).toList();
    }
    private void rebuild(){
        clearChildren();buttons.clear();search=null;dragging=false;
        control(selected==null?(parent instanceof QuickMenuScreen?"BACK":"MODS"):"BACK",layout.back,this::close);
        control("CLOSE",layout.close,this::close);control("DONE",layout.done,this::close);
        Rect area=layout.controls;
        if(selected==null){
            searchBox=new Rect(area.x(),area.y(),area.width(),22);
            viewport=new Rect(area.x(),area.y()+42,area.width(),Math.max(1,area.height()-42));
            search=field("Search gangs or leaders...",query,searchBox,100);
            search.setPlaceholder(UiTypography.text("Search gangs or leaders...",.85f,false));
            search.addFormatter((value,index)->UiTypography.text(value,.85f,false).asOrderedText());
            search.setChangedListener(value->{query=value;listScroll=0;refresh();rebuildRows();});
            listScroll=Math.clamp(listScroll,0,maxScroll());rebuildRows();
        }else{
            wide=area.width()>=430&&area.height()>=205;
            compactDetails=area.height()<110;
            if(wide){
                int memberWidth=area.width()*59/100;
                viewport=new Rect(area.x(),area.y()+19,memberWidth,Math.max(1,area.height()-19));
                stats=new Rect(area.x()+memberWidth+12,area.y(),area.width()-memberWidth-12,area.height());
            }else{
                stats=new Rect(area.x(),area.y(),area.width(),44);
                int top=compactDetails?19:64;
                viewport=new Rect(area.x(),area.y()+top,area.width(),Math.max(1,area.height()-top));
            }
            columns=Math.max(1,viewport.width()/132);
            memberScroll=Math.clamp(memberScroll,0,maxScroll());
        }
    }
    private GuideButton control(String label,Rect area,Runnable action){return addDrawableChild(new GuideButton(label,area,action));}
    private void rebuildRows(){
        for(var b:buttons){if(b.isFocused())setFocused(null);remove(b);}buttons.clear();
        int first=listScroll/ROW,last=Math.min(rows.size(),(listScroll+viewport.height()+ROW-1)/ROW);
        for(int index=first;index<last;index++){
            var b=new GangButton(rows.get(index),new Rect(viewport.x(),viewport.y()+index*ROW-listScroll,viewport.width(),ROW-3));
            b.visible=b.getY()>=viewport.y()&&b.getBottom()<=viewport.bottom();buttons.add(addSelectableChild(b));
        }
    }
    private void open(GangDirectory.Gang gang){selected=gang;selectedId=gang.id();memberScroll=0;setFocused(null);rebuild();}
    @Override public void render(DrawContext c,int mx,int my,float delta){
        if(revision!=GangDirectoryGuide.catalogue.revision){
            boolean focus=search!=null&&search.isFocused();int cursor=search==null?0:search.getCursor();
            refresh();rebuild();if(search!=null&&focus){setFocused(search);search.setCursor(Math.min(cursor,query.length()),false);}
        }
        c.getMatrices().pushMatrix();c.getMatrices().translate(0,animation.offset());
        try{drawContent(c,mx,(int)(my-animation.offset()),delta);}finally{c.getMatrices().popMatrix();}
    }
    private void drawContent(DrawContext c,int mx,int my,float delta){
        frame(c,layout.panel,PANEL_BORDER,PANEL);
        int titleWidth=layout.panel.width()-100,center=layout.panel.x()+65+titleWidth/2;
        centered(c,"AXIAL CLIENT",center,layout.panel.y()+8,BRAND,.75f,titleWidth,false);
        centered(c,selected==null?"Gangs":selected.name(),center,layout.panel.y()+21,INK,1,titleWidth,true);
        c.fill(layout.panel.x()+12,layout.controls.y()-5,layout.panel.right()-12,layout.controls.y()-4,DIVIDER);
        Text tooltip=null;
        if(selected==null){
            frame(c,searchBox,search.isFocused()?FOCUS:LINE,FIELD);
            line(c,"GANG TOP",viewport.x()+4,searchBox.bottom()+6,MUTED,.75f,viewport.width()-90,true);
            line(c,"POINTS",viewport.right()-80,searchBox.bottom()+6,MUTED,.75f,76,true);
            c.enableScissor(viewport.x(),viewport.y(),viewport.right(),viewport.bottom());
            for(var b:buttons){b.drawRow(c,mx,my);if(b.visible&&b.isMouseOver(mx,my))tooltip=Text.literal(b.gang.name()+" · #"+b.gang.rank()+" · Leader: "+b.gang.leaderName());}
            if(rows.isEmpty()){
                String status=GangDirectoryGuide.catalogue.revision==0?GangDirectoryGuide.status:GangDirectoryGuide.catalogue.gangs.isEmpty()?"No gangs on this server.":"No matching gangs.";
                int y=viewport.y()+10;for(var value:textRenderer.wrapLines(UiTypography.text(status,.8f,false),Math.max(1,viewport.width()-16))){c.drawText(textRenderer,value,viewport.x()+8,y,MUTED,false);y+=12;}
            }
            c.disableScissor();
        }else tooltip=drawDetails(c,mx,my);
        c.fill(layout.panel.x()+12,layout.panel.bottom()-32,layout.panel.right()-12,layout.panel.bottom()-31,DIVIDER);
        line(c,selected==null?rows.size()+" GANGS · ORDERED BY GANG TOP":compactDetails?"#"+selected.rank()+" · "+numbers.format(selected.points())+" PTS · "+(selected.wealth()==null?"BALANCE UNAVAILABLE":money.format(selected.wealth())):selected.members().size()+" MEMBERS · PUBLIC GANG PROFILE",layout.controls.x(),layout.done.y()+6,MUTED,.75f,layout.done.x()-layout.controls.x()-10,false);
        if(maxScroll()>0){Rect bar=scrollbar();int thumb=thumb();rounded(c,bar.x(),bar.y(),3,bar.height(),DIVIDER);rounded(c,bar.x(),bar.y()+(bar.height()-thumb)*currentScroll()/maxScroll(),3,thumb,MUTED);}
        super.render(c,mx,my,delta);
        if(tooltip!=null)c.drawTooltip(textRenderer,tooltip,mx,my);
    }
    private Text drawDetails(DrawContext c,int mx,int my){
        String balance=selected.wealth()==null?"Unavailable":money.format(selected.wealth());
        String[] labels={"GANG TOP","TOTAL POINTS","MEMBERS' MONEY"};
        String[] values={"#"+selected.rank(),numbers.format(selected.points()),balance};
        Text tooltip=null;
        for(int i=0;!compactDetails&&i<3;i++){
            Rect card=wide?new Rect(stats.x(),stats.y()+i*59,stats.width(),53):new Rect(stats.x()+i*(stats.width()+6)/3,stats.y(),(stats.width()-12)/3,44);
            frame(c,card,LINE,FIELD);
            line(c,labels[i],card.x()+7,card.y()+7,MUTED,.72f,card.width()-14,true);
            line(c,values[i],card.x()+7,card.y()+23,i==0?BRAND:INK,.9f,card.width()-14,true);
            if(card.contains(mx,my))tooltip=Text.literal(labels[i]+": "+values[i]+(i==2?" · Combined personal balances of all members":""));
        }
        if(compactDetails&&my>=layout.done.y()&&mx>=layout.controls.x()&&mx<layout.done.x())tooltip=Text.literal("Rank #"+selected.rank()+" · "+numbers.format(selected.points())+" points · Members’ combined money: "+balance);
        if(wide){
            line(c,"ALL MEMBERS COMBINED",stats.x()+3,stats.y()+183,MUTED,.7f,stats.width()-6,false);
            line(c,selected.members().size()+" MEMBERS",stats.x()+3,stats.y()+198,MUTED,.75f,stats.width()-6,false);
        }
        line(c,"MEMBERS",viewport.x()+1,viewport.y()-14,MUTED,.78f,viewport.width(),true);
        int cardWidth=(viewport.width()-(columns-1)*6)/columns;
        int first=memberScroll/MEMBER_ROW*columns,last=Math.min(selected.members().size(),((memberScroll+viewport.height()+MEMBER_ROW-1)/MEMBER_ROW)*columns);
        c.enableScissor(viewport.x(),viewport.y(),viewport.right(),viewport.bottom());
        for(int i=first;i<last;i++){
            var member=selected.members().get(i);int x=viewport.x()+i%columns*(cardWidth+6),y=viewport.y()+i/columns*MEMBER_ROW-memberScroll;
            Rect card=new Rect(x,y,cardWidth,MEMBER_ROW-4);boolean hover=card.contains(mx,my)&&viewport.contains(mx,my);
            frame(c,card,member.id().equals(selected.leader())?BRAND:hover?HOVER_LINE:LINE,hover?HOVER:FIELD);
            GangPlayerHeads.draw(c,member.id(),x+5,y+6,24);
            line(c,member.name(),x+35,y+7,INK,.8f,cardWidth-40,true);
            line(c,member.role().replace('_',' '),x+35,y+21,member.id().equals(selected.leader())?BRAND:MUTED,.7f,cardWidth-40,false);
            if(hover)tooltip=Text.literal(member.name()+" · "+member.role().replace('_',' ')+(member.online()?" · Online":" · Offline"));
        }
        c.disableScissor();return tooltip;
    }
    private Rect scrollbar(){return new Rect(layout.scrollbar.x(),viewport.y(),3,viewport.height());}
    private int maxScroll(){return selected==null?Math.max(0,rows.size()*ROW-viewport.height()):Math.max(0,((selected.members().size()+columns-1)/columns)*MEMBER_ROW-viewport.height());}
    private int currentScroll(){return selected==null?listScroll:memberScroll;}
    private int thumb(){Rect bar=scrollbar();return Math.min(bar.height(),Math.max(12,bar.height()*bar.height()/(bar.height()+maxScroll())));}
    private void scrollTo(int value){int next=Math.clamp(value,0,maxScroll());if(selected==null){listScroll=next;rebuildRows();}else memberScroll=next;}
    private void dragScroll(double y){Rect bar=scrollbar();int thumb=thumb();scrollTo((int)Math.round((y-bar.y()-thumb/2.0)/Math.max(1,bar.height()-thumb)*maxScroll()));}
    private Click transformed(Click click){return new Click(click.x(),click.y()-animation.offset(),click.buttonInfo());}
    @Override public boolean mouseClicked(Click click,boolean doubled){
        if(animation.closing())return true;click=transformed(click);Rect bar=scrollbar();
        if(click.button()==0&&maxScroll()>0&&click.x()>=bar.x()-2&&click.x()<bar.right()+3&&click.y()>=bar.y()&&click.y()<bar.bottom()){dragging=true;dragScroll(click.y());return true;}
        return super.mouseClicked(click,doubled);
    }
    @Override public boolean mouseDragged(Click click,double dx,double dy){if(animation.closing())return true;if(dragging){dragScroll(click.y()-animation.offset());return true;}return super.mouseDragged(transformed(click),dx,dy);}
    @Override public boolean mouseReleased(Click click){dragging=false;return super.mouseReleased(transformed(click));}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(animation.closing())return true;if(viewport.contains(x,y-animation.offset())){scrollTo(currentScroll()-(int)Math.round(vertical*26));return true;}return false;}
    @Override public boolean keyPressed(KeyInput input){
        if(animation.closing())return true;
        if(!(getFocused() instanceof TextFieldWidget)){
            switch(input.key()){
                case GLFW.GLFW_KEY_PAGE_DOWN->{scrollTo(currentScroll()+viewport.height());return true;}
                case GLFW.GLFW_KEY_PAGE_UP->{scrollTo(currentScroll()-viewport.height());return true;}
                case GLFW.GLFW_KEY_HOME->{scrollTo(0);return true;}
                case GLFW.GLFW_KEY_END->{scrollTo(maxScroll());return true;}
            }
            if(QuickMenuScreen.isMenuKey(input)){close();return true;}
        }
        return super.keyPressed(input);
    }
    @Override public void close(){if(selected!=null){selected=null;selectedId=null;memberScroll=0;setFocused(null);rebuild();}else animation.close();}
    @Override public void tick(){if(animation.finished())client.setScreen(parent);}
    @Override public void removed(){reopen=true;}
    @Override public boolean shouldPause(){return false;}
    @Override protected void line(DrawContext c,String value,int x,int y,int color,float scale,int width,boolean bold){label(c,textRenderer,value,x,y,color,scale,width,bold);}
    private void centered(DrawContext c,String value,int center,int y,int color,float scale,int width,boolean bold){String trimmed=textRenderer.trimToWidth(UiTypography.text(value,scale,bold),Math.max(0,width)).getString();label(c,textRenderer,trimmed,center-textRenderer.getWidth(UiTypography.text(trimmed,scale,bold))/2,y,color,scale,width,bold);}
    private class GuideButton extends ButtonWidget {
        GuideButton(String label,Rect r,Runnable action){super(r.x(),r.y(),r.width(),r.height(),text(label),b->action.run(),DEFAULT_NARRATION_SUPPLIER);}
        @Override public void playDownSound(SoundManager sounds){AxialUiTheme.playButtonClickSound();}
        @Override protected void drawIcon(DrawContext c,int mx,int my,float dt){boolean hover=isHovered()||isFocused();Rect r=new Rect(getX(),getY(),width,height);if(getMessage().getString().equals("CLOSE")){icon(c,"close",getX()+4,getY()+5,11,11,hover?INK:MUTED);return;}frame(c,r,hover?HOVER_LINE:LINE,hover?HOVER:FIELD);centered(c,getMessage().getString(),r.x()+r.width()/2,r.y()+6,INK,.8f,r.width()-14,false);}
    }
    private final class GangButton extends GuideButton {
        final GangDirectory.Gang gang;
        GangButton(GangDirectory.Gang gang,Rect r){super("#"+gang.rank()+" "+gang.name()+", leader "+gang.leaderName()+", "+gang.points()+" points",r,()->open(gang));this.gang=gang;}
        void drawRow(DrawContext c,int mx,int my){
            boolean hover=visible&&(isMouseOver(mx,my)||isFocused());Rect r=new Rect(getX(),getY(),width,height);frame(c,r,hover?HOVER_LINE:LINE,hover?HOVER:FIELD);
            line(c,"#"+gang.rank(),r.x()+6,r.y()+11,BRAND,.78f,35,true);GangPlayerHeads.draw(c,gang.leader(),r.x()+44,r.y()+4,23);
            int textX=r.x()+76,available=Math.max(1,r.width()-164);
            line(c,gang.name(),textX,r.y()+5,INK,.85f,available,true);line(c,gang.leaderName(),textX,r.y()+18,MUTED,.72f,available,false);
            line(c,numbers.format(gang.points()),r.right()-80,r.y()+11,INK,.85f,74,true);
        }
    }
}
