package com.axial.cosmetics.client;

import com.axial.cosmetics.mixin.InGameHudScoreboardInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.axial.axialutils.client.AxialConfigManager;

/** Uses vanilla's measured sidebar so custom team prefixes and number formats fit. */
public final class ScoreboardHud {
    public record Bounds(int x,int y,int width,int height){
        public boolean contains(double mx,double my){return mx>=x&&mx<x+width&&my>=y&&my<y+height;}
    }
    private static Bounds bounds;
    private static ScoreboardObjective sample;
    private ScoreboardHud(){}
    public static boolean enabled(){return Boolean.TRUE.equals(AxialConfigManager.get().showScoreboard);}
    public static Bounds bounds(){return bounds;}
    public static void place(DrawContext context,int left,int top,int right,int bottom){
        int width=right-left,height=bottom-top;
        int x=ScoreboardHudConfig.x(left,context.getScaledWindowWidth(),width);
        int y=ScoreboardHudConfig.y(top,context.getScaledWindowHeight(),height);
        bounds=new Bounds(x,y,width,height);
        context.getMatrices().translate(x-left,y-top);
    }
    public static void renderPreview(DrawContext context,MinecraftClient client){
        if(!enabled()){bounds=null;return;}
        ScoreboardObjective objective=null;
        if(client.world!=null){
            var scoreboard=client.world.getScoreboard();
            if(client.player!=null){
                var team=scoreboard.getScoreHolderTeam(client.player.getNameForScoreboard());
                var slot=team==null?null:ScoreboardDisplaySlot.fromFormatting(team.getColor());
                if(slot!=null)objective=scoreboard.getObjectiveForSlot(slot);
            }
            if(objective==null)objective=scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        }
        if(objective==null)objective=sample();
        ((InGameHudScoreboardInvoker)client.inGameHud).axial$renderSidebar(context,objective);
    }
    private static ScoreboardObjective sample(){
        if(sample==null){
            var scoreboard=new Scoreboard();
            sample=scoreboard.addObjective("axial_hud_preview",ScoreboardCriterion.DUMMY,Text.literal("AxialPrisons").formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD),ScoreboardCriterion.RenderType.INTEGER,false,BlankNumberFormat.INSTANCE);
            String[] lines={"Player: Your name","Rank: Prisoner","Balance: $10,000","Blocks: 1,234","play.axialprisons.com"};
            for(int i=0;i<lines.length;i++)scoreboard.getOrCreateScore(ScoreHolder.fromName(lines[i]),sample).setScore(lines.length-i);
        }
        return sample;
    }
}
