package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import static com.axial.cosmetics.client.ModMenuUi.*;

public final class CombatCooldownHud {
    public static final int SIZE=CooldownRing.SIZE;
    public record Bounds(int x,int y,int width,int height){public boolean contains(double x,double y){return x>=this.x&&x<this.x+width&&y>=this.y&&y<this.y+height;}}
    private static final Identifier ATLAS=Identifier.of("axial_cosmetics","cooldown_ring");
    private static final int COLUMNS=16,ATLAS_WIDTH=COLUMNS*SIZE,ATLAS_HEIGHT=9*SIZE;
    private static final CombatCooldownState STATE=new CombatCooldownState();
    private static NativeImageBackedTexture texture;
    private static ItemStack sword;
    private CombatCooldownHud(){}
    public static void reset(){STATE.reset();}
    public static void receive(byte[] data){try{STATE.receive(data);}catch(java.io.IOException ex){org.slf4j.LoggerFactory.getLogger(CombatCooldownHud.class).warn("Invalid combat cooldown update",ex);}}
    public static boolean visible(){return CooldownsConfig.combatVisible();}
    public static Bounds bounds(MinecraftClient client){return new Bounds(CooldownsConfig.x(client.getWindow().getScaledWidth()),CooldownsConfig.y(client.getWindow().getScaledHeight()),SIZE,SIZE);}
    public static void render(DrawContext context){
        var client=MinecraftClient.getInstance();
        if(!visible()||client.player==null||client.options.hudHidden)return;
        long remaining=STATE.remainingMillis();if(remaining<=0)return;
        var bounds=bounds(client);draw(context,bounds.x(),bounds.y(),STATE.fraction(),remaining);
    }
    public static void preview(DrawContext context,MinecraftClient client){if(visible()){var b=bounds(client);example(context,b.x(),b.y());}}
    public static void example(DrawContext context,int x,int y){draw(context,x,y,2f/3,10000);}
    private static void draw(DrawContext c,int x,int y,float fraction,long remaining){
        prepare();
        circle(c,x+5,y+5,SIZE-10,0xFF171B22);
        ring(c,x,y,CooldownRing.STEPS,LINE);
        int step=Math.clamp((int)Math.ceil(fraction*CooldownRing.STEPS),1,CooldownRing.STEPS);
        ring(c,x,y,step,remaining<=3000?RED:BRAND);
        c.drawItem(sword,x+20,y+10);
        centered(c,"COMBAT",x+SIZE/2,y+29,INK,.75f,true);
        centered(c,((remaining+999)/1000)+"s",x+SIZE/2,y+40,remaining<=3000?RED:BRAND,.85f,true);
    }
    private static void centered(DrawContext c,String label,int x,int y,int color,float size,boolean bold){
        var font=MinecraftClient.getInstance().textRenderer;var text=UiTypography.text(label,size,bold);
        c.drawText(font,text,x-font.getWidth(text)/2,y,color,false);
    }
    private static void ring(DrawContext c,int x,int y,int frame,int color){
        c.drawTexture(RenderPipelines.GUI_TEXTURED,ATLAS,x,y,(frame%COLUMNS)*SIZE,(frame/COLUMNS)*SIZE,SIZE,SIZE,SIZE,SIZE,ATLAS_WIDTH,ATLAS_HEIGHT,color);
    }
    private static void prepare(){
        if(texture!=null)return;
        var image=new NativeImage(ATLAS_WIDTH,ATLAS_HEIGHT,true);
        for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){
            int alpha=CooldownRing.alpha(x,y);if(alpha==0)continue;
            int first=Math.max(1,(int)Math.ceil(CooldownRing.angle(x,y)/(Math.PI*2)*CooldownRing.STEPS));
            for(int step=first;step<=CooldownRing.STEPS;step++)image.setColorArgb(step%COLUMNS*SIZE+x,step/COLUMNS*SIZE+y,(alpha<<24)|0xFFFFFF);
        }
        texture=new NativeImageBackedTexture(()->"axial/combat_cooldown",image);
        MinecraftClient.getInstance().getTextureManager().registerTexture(ATLAS,texture);texture.upload();
        sword=new ItemStack(Items.DIAMOND_SWORD);
    }
}
