package com.axial.cosmetics.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import com.axial.cosmetics.client.ModMenuLayout.Rect;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** Explicit selection and confirmation prevent accidental one-time votes. */
public final class PollsScreen extends NavigationScreen {
    private Rect panel,viewport;
    private ClientPoll poll;
    private int index,selected,scroll,contentHeight;
    private boolean sending;
    private long revision=-1;
    private List<OrderedText> question=List.of();
    private final List<OptionButton> options=new ArrayList<>();
    private NavButton submit;
    private String status="";
    public PollsScreen(Screen parent){super("Community Polls",parent);PollService.refresh();}
    @Override protected void init(){rebuild();}
    private void rebuild(){
        clearChildren();options.clear();
        var polls=PollService.active();index=Math.clamp(index,0,Math.max(0,polls.size()-1));
        var next=polls.isEmpty()?null:polls.get(index);
        if(poll==null||next==null||poll.id()!=next.id()){selected=0;scroll=0;status="";}
        poll=next;
        int w=Math.min(440,width-20),h=Math.min(360,height-16);
        panel=new Rect((width-w)/2,(height-h)/2,w,h);
        if(poll!=null)question=textRenderer.wrapLines(copy(poll.question(),.85f,true),w-32);else question=List.of();
        int top=panel.y()+54;
        viewport=new Rect(panel.x()+16,top,w-32,Math.max(24,panel.bottom()-top-62));
        if(polls.isEmpty())button("Refresh",new Rect(panel.right()-91,panel.y()+29,75,18),PollService::refresh).active=PollsConfig.enabled()&&!PollService.loading();
        if(polls.size()>1){
            button("<",new Rect(panel.right()-70,panel.y()+29,24,18),()->change(-1)).active=!sending;
            button(">",new Rect(panel.right()-42,panel.y()+29,24,18),()->change(1)).active=!sending;
        }
        contentHeight=question.size()*11+8;
        if(poll!=null)for(var option:poll.options()){
            var lines=textRenderer.wrapLines(copy(option.label(),.85f,false),viewport.width()-20);
            int rowHeight=Math.max(24,lines.size()*11+12);
            options.add(addDrawableChild(new OptionButton(option,lines,contentHeight,rowHeight)));
            contentHeight+=rowHeight+5;
        }
        scroll=Math.clamp(scroll,0,Math.max(0,contentHeight-viewport.height()));
        button("Back",new Rect(panel.x()+16,panel.bottom()-32,65,22),this::close);
        submit=button("Submit Vote",new Rect(panel.right()-132,panel.bottom()-32,116,22),this::vote);submit.primary=true;
        positionOptions();revision=PollService.revision();
    }
    private void change(int step){int count=PollService.active().size();if(count==0){rebuild();return;}index=Math.floorMod(index+step,count);selected=0;scroll=0;status="";rebuild();}
    private void positionOptions(){
        for(var row:options){row.setY(viewport.y()+row.offset-scroll);row.visible=row.getBottom()>viewport.y()&&row.getY()<viewport.bottom();row.active=!sending&&poll!=null&&poll.voted()==null&&poll.remaining(System.nanoTime())>0;}
    }
    private void vote(){
        if(sending||poll==null||selected==0||poll.voted()!=null||poll.remaining(System.nanoTime())==0)return;
        sending=true;error="";status="Recording your vote…";
        PollService.vote(poll,selected).whenComplete((ignored,failure)->{
            sending=false;
            if(failure==null)status="Thank you! Your vote has been recorded.";
            else {status="";Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();error=cause instanceof java.io.IOException?cause.getMessage():"Could not vote. Please retry.";}
            rebuild();
        });
    }
    @Override public void tick(){
        if(revision!=PollService.revision()||poll!=null&&poll.remaining(System.nanoTime())==0)rebuild();
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(viewport.contains(x,y)){scroll=Math.clamp(scroll-(int)(vertical*28),0,Math.max(0,contentHeight-viewport.height()));positionOptions();return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public void render(DrawContext c,int mx,int my,float dt){
        heading(c,panel,"Community Polls","");
        var polls=PollService.active();
        String timer=poll==null?"":poll.timeLeft()+" · "+(index+1)+" / "+polls.size();
        line(c,timer,panel.x()+16,panel.y()+34,BRAND,.75f,panel.width()-100,false);
        if(poll!=null){
            c.enableScissor(viewport.x(),viewport.y(),viewport.right(),viewport.bottom());
            for(int i=0;i<question.size();i++)c.drawText(textRenderer,question.get(i),viewport.x(),viewport.y()+i*11-scroll,INK,false);
            c.disableScissor();
            if(contentHeight>viewport.height())line(c,"Scroll for more choices",viewport.x(),viewport.bottom()+4,MUTED,.65f,viewport.width(),false);
        }else {
            String empty=!PollsConfig.enabled()?"Enable Polls in the mod menu to see community polls.":PollService.loading()?"Checking for polls…":!PollService.error().isBlank()?PollService.error():"No active polls. Check back soon!";
            var lines=textRenderer.wrapLines(copy(empty,.85f,false),panel.width()-32);
            for(int i=0;i<Math.min(4,lines.size());i++)c.drawText(textRenderer,lines.get(i),panel.x()+16,panel.y()+60+i*12,MUTED,false);
        }
        submit.active=!sending&&poll!=null&&poll.voted()==null&&selected!=0&&poll.remaining(System.nanoTime())>0;
        submit.label(sending?"Voting…":poll!=null&&poll.voted()!=null?"Voted":"Submit Vote");
        positionOptions();
        String hint=!error.isBlank()?error:!status.isBlank()?status:poll!=null&&poll.voted()!=null?"Your vote is recorded. One vote per account.":"Choose one option, then submit. Votes cannot be changed.";
        line(c,hint,panel.x()+16,panel.bottom()-46,!error.isBlank()?RED:poll!=null&&poll.voted()!=null?GREEN:MUTED,.7f,panel.width()-32,false);
        for(var child:children())if(child instanceof net.minecraft.client.gui.Drawable drawable)drawable.render(c,mx,my,dt);
    }
    private final class OptionButton extends NavButton {
        final ClientPoll.Option option;final List<OrderedText> lines;final int offset;
        OptionButton(ClientPoll.Option option,List<OrderedText> lines,int offset,int height){
            super(option.label(),new Rect(viewport.x(),viewport.y()+offset,viewport.width(),height),()->PollsScreen.this.selected=option.id());
            this.option=option;this.lines=lines;this.offset=offset;
        }
        @Override public boolean mouseClicked(net.minecraft.client.gui.Click click,boolean doubled){
            return viewport.contains(click.x(),click.y())&&super.mouseClicked(click,doubled);
        }
        @Override protected void drawContents(DrawContext c,int mx,int my,float dt){
            c.enableScissor(viewport.x(),viewport.y(),viewport.right(),viewport.bottom());
            boolean chosen=poll.voted()!=null?poll.voted()==option.id():PollsScreen.this.selected==option.id();
            frame(c,new Rect(getX(),getY(),getWidth(),getHeight()),chosen?BRAND:isHovered()?HOVER_LINE:LINE,chosen?SELECTED:FIELD);
            for(int i=0;i<lines.size();i++)c.drawText(textRenderer,lines.get(i),getX()+10,getY()+7+i*11,chosen?BRAND:INK,false);
            c.disableScissor();
        }
    }
}
