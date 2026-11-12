package com.axial.cosmetics.client;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import com.axial.cosmetics.client.ModMenuLayout.Rect;
import static com.axial.cosmetics.client.ModMenuUi.*;

public final class PollsHud {
    private static final PollVisuals visuals=new PollVisuals();
    private PollsHud() {}
    public static Rect bounds(MinecraftClient client){
        int w=PollHudLayout.width(client.getWindow().getScaledWidth()),h=PollHudLayout.HEIGHT;
        return new Rect(PollsConfig.x(client.getWindow().getScaledWidth(),w),PollsConfig.y(client.getWindow().getScaledHeight(),h),w,h);
    }
    private static ClientPoll featured(List<ClientPoll> polls){return polls.stream().filter(p->p.voted()==null).findFirst().orElse(polls.getFirst());}
    public static void render(DrawContext c){
        var client=MinecraftClient.getInstance();
        if(!PollsConfig.enabled()||client.player==null||client.options.hudHidden||client.currentScreen!=null)return;
        var polls=PollService.active();if(polls.isEmpty())return;
        draw(c,bounds(client),featured(polls),polls.size());
    }
    public static void preview(DrawContext c,MinecraftClient client){
        if(!PollsConfig.enabled())return;
        var polls=PollService.active();
        var sample=new ClientPoll(0,"What should we add to Axial next?",List.of(
                new ClientPoll.Option(1,"Prisons features",64),new ClientPoll.Option(2,"New cosmetics",36)),null,System.nanoTime()+86_340_000_000_000L);
        draw(c,bounds(client),polls.isEmpty()?sample:featured(polls),Math.max(1,polls.size()));
    }
    private static void draw(DrawContext c,Rect b,ClientPoll poll,int count){
        var font=MinecraftClient.getInstance().textRenderer;
        frame(c,b,0xFF464052,0xE6171B20);
        label(c,font,(b.width()<175?"POLL":"COMMUNITY POLL")+(count>1?" · "+count:""),b.x()+9,b.y()+7,BRAND,.65f,Math.max(1,b.width()-86),true);
        var timer=UiTypography.text(poll.timeLeft(),.65f,false);
        c.drawText(font,timer,b.right()-9-font.getWidth(timer),b.y()+7,MUTED,false);
        var lines=font.wrapLines(UiTypography.text(poll.question(),.8f,true),Math.max(1,b.width()-18));
        for(int i=0;i<Math.min(2,lines.size());i++)c.drawText(font,lines.get(i),b.x()+9,b.y()+20+i*9,INK,false);
        for(int i=0;i<Math.min(2,poll.options().size());i++) {
            var option=poll.options().get(i);var row=PollHudLayout.option(b,i);
            boolean chosen=poll.voted()!=null&&poll.voted()==option.id();
            label(c,font,(chosen?"✓ ":"")+option.label(),row.x(),row.y(),chosen?BRAND:INK,.7f,Math.max(1,row.width()-37),false);
            var percent=UiTypography.text(PollVisuals.percentage(poll,option),.7f,true);
            c.drawText(font,percent,row.right()-font.getWidth(percent),row.y(),INK,false);
            visuals.bar(c,poll,option,PollHudLayout.bar(row),chosen);
        }
        String action=PollService.keyLabel()+" · "+(poll.voted()!=null?"Voted":"Vote");
        String more=poll.options().size()>2?" · +"+(poll.options().size()-2)+" choices":"";
        label(c,font,action+more,b.x()+9,b.bottom()-10,poll.voted()!=null?GREEN:BRAND,.65f,Math.max(1,b.width()/2-12),false);
        var votes=UiTypography.text(PollVisuals.votes(poll),.6f,false);
        String trimmed=font.trimToWidth(votes,Math.max(1,b.width()/2-12)).getString();
        var total=UiTypography.text(trimmed,.6f,false);
        c.drawText(font,total,b.right()-9-font.getWidth(total),b.bottom()-10,MUTED,false);
    }
}
