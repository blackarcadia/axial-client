package com.axial.cosmetics.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import com.axial.cosmetics.client.ModMenuLayout.Rect;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** Shared result bars for the compact HUD and voting menu. */
final class PollVisuals {
    private int pollId=Integer.MIN_VALUE;
    private final Map<Integer,UiMotion> bars=new HashMap<>();
    static String percentage(ClientPoll poll,ClientPoll.Option option){return poll.resultsAvailable()?poll.percentage(option)+"%":"—";}
    static String votes(ClientPoll poll){return !poll.resultsAvailable()?"Results unavailable":poll.totalVotes()+(poll.totalVotes()==1?" vote":" votes");}
    void bar(DrawContext c,ClientPoll poll,ClientPoll.Option option,Rect r,boolean chosen) {
        if(pollId!=poll.id()){pollId=poll.id();bars.clear();}
        double target=poll.resultsAvailable()?poll.percentage(option)/100.0:0;
        double progress=bars.computeIfAbsent(option.id(),key->new UiMotion(0)).toward(target);
        rounded(c,r.x(),r.y(),r.width(),r.height(),0xFF303440);
        int fill=Math.clamp((int)Math.round(r.width()*progress),0,r.width());
        if(fill>0)rounded(c,r.x(),r.y(),fill,r.height(),chosen?BRAND:0xFF8665BA);
    }
}
