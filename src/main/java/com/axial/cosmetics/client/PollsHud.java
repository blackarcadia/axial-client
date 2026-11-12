package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import com.axial.cosmetics.client.ModMenuLayout.Rect;
import static com.axial.cosmetics.client.ModMenuUi.*;

public final class PollsHud {
    private PollsHud() {}
    public static Rect bounds(MinecraftClient client){
        int w=Math.min(238,Math.max(100,client.getWindow().getScaledWidth()-12)),h=108;
        return new Rect(PollsConfig.x(client.getWindow().getScaledWidth(),w),PollsConfig.y(client.getWindow().getScaledHeight(),h),w,h);
    }
    public static void render(DrawContext c){
        var client=MinecraftClient.getInstance();
        if(!PollsConfig.enabled()||client.player==null||client.options.hudHidden||client.currentScreen!=null)return;
        var polls=PollService.active();if(polls.isEmpty())return;
        var poll=polls.stream().filter(p->p.voted()==null).findFirst().orElse(polls.getFirst());
        draw(c,bounds(client),poll.question(),poll.timeLeft(),poll.voted()!=null,polls.size());
    }
    public static void preview(DrawContext c,MinecraftClient client){
        if(!PollsConfig.enabled())return;
        var polls=PollService.active();var poll=polls.isEmpty()?null:polls.getFirst();
        draw(c,bounds(client),poll==null?"What should we add to Axial next?":poll.question(),poll==null?"23h 59m left":poll.timeLeft(),poll!=null&&poll.voted()!=null,Math.max(1,polls.size()));
    }
    private static void draw(DrawContext c,Rect b,String question,String countdown,boolean voted,int count){
        var font=MinecraftClient.getInstance().textRenderer;
        frame(c,b,LINE,0xE6171B20);
        c.drawText(font,UiTypography.text("COMMUNITY POLL"+(count>1?" · "+count:""),.75f,true),b.x()+10,b.y()+9,BRAND,false);
        var lines=font.wrapLines(UiTypography.text(question,.85f,false),b.width()-20);
        for(int i=0;i<Math.min(3,lines.size());i++)c.drawText(font,lines.get(i),b.x()+10,b.y()+26+i*11,INK,false);
        c.drawText(font,UiTypography.text(countdown,.75f,false),b.x()+10,b.bottom()-33,MUTED,false);
        var hint=UiTypography.text(voted?"Vote recorded · "+PollService.keyLabel()+": view polls":PollService.keyLabel()+": open & vote",.75f,false);
        c.drawText(font,UiTypography.text(font.trimToWidth(hint,b.width()-20).getString(),.75f,false),b.x()+10,b.bottom()-17,voted?GREEN:BRAND,false);
    }
}
