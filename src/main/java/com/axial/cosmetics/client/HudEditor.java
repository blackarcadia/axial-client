package com.axial.cosmetics.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.axial.axialutils.client.*;
import org.lwjgl.glfw.GLFW;
import static com.axial.cosmetics.client.HudSnap.Box;

/** Shared hit boxes and positioning for every movable HUD, including the bundled HUDs. */
public final class HudEditor {
    private record Element(String id, Box bounds, BiConsumer<Integer,Integer> move, Runnable save) {}
    private Element dragging;
    private String selected;
    private int offsetX, offsetY;
    private HudSnap.Result alignment;

    private static List<Element> elements(MinecraftClient client) {
        var result = new ArrayList<Element>();
        var config = AxialConfigManager.get();
        if (Boolean.TRUE.equals(config.showSatchelsHud)) add(result, "Satchels", config.satchelsHudX, config.satchelsHudY,
                SatchelHudRenderer.getWidth(client), SatchelHudRenderer.getHeight(client),
                (x,y) -> { config.satchelsHudX=x; config.satchelsHudY=y; }, AxialConfigManager::save);
        if (config.hudEnabled) add(result, "Information HUD", config.hudX, config.hudY,
                AxialHudRenderer.getWidth(client), AxialHudRenderer.getHeight(client),
                (x,y) -> { config.hudX=x; config.hudY=y; }, AxialConfigManager::save);
        if (Boolean.TRUE.equals(config.showCps)) add(result, "CPS", config.cpsHudX, config.cpsHudY,
                CpsHudRenderer.getWidth(client), CpsHudRenderer.getHeight(client),
                (x,y) -> { config.cpsHudX=x; config.cpsHudY=y; }, AxialConfigManager::save);
        if (Boolean.TRUE.equals(config.showArmorHud)) add(result, "Armor", config.armorHudX, config.armorHudY,
                ArmorHudRenderer.getWidth(client), ArmorHudRenderer.getHeight(client),
                (x,y) -> { config.armorHudX=x; config.armorHudY=y; }, AxialConfigManager::save);
        int width=client.getWindow().getScaledWidth(),height=client.getWindow().getScaledHeight();
        if (CompassConfig.isEnabled()) {
            int w=CompassHudRenderer.getWidth(width),h=CompassHudRenderer.getHeight();
            add(result,"Compass",CompassConfig.getX(width,w),CompassConfig.getY(height,h),w,h,
                    CompassConfig::setPosition,AxialConfigManager::save);
        }
        if (MinimapConfig.isEnabled()) {
            int size=Math.min(MinimapConfig.getSize(),Math.min(width-12,height-32));
            if(size>=16) {
                int x=Math.clamp(MinimapConfig.getX(width),6,width-size-6),y=Math.clamp(MinimapConfig.getY(height),6,height-size-26);
                add(result,"Minimap",x-3,y-3,size+6,size+(MinimapConfig.showCoordinates()?24:6),
                        (left,top)->MinimapConfig.setPosition(left+3,top+3),MinimapConfig::save);
            }
        }
        if(PotionsHudConfig.isEnabled()) {
            var b=PotionsHudRenderer.bounds(client,true);
            add(result,"Potions",b.x(),b.y(),b.width(),b.height(),PotionsHudConfig::setPosition,PotionsHudConfig::save);
        }
        if(ScoreboardHud.enabled()&&ScoreboardHud.bounds()!=null) {
            var b=ScoreboardHud.bounds();
            add(result,"Scoreboard",b.x(),b.y(),b.width(),b.height(),ScoreboardHudConfig::setPosition,ScoreboardHudConfig::save);
        }
        if(CombatCooldownHud.visible()) {
            var b=CombatCooldownHud.bounds(client);
            add(result,"Combat cooldown",b.x(),b.y(),b.width(),b.height(),CooldownsConfig::setPosition,CooldownsConfig::save);
        }
        if(KeystrokesConfig.enabled()&&KeystrokesConfig.visibleMask()!=0) {
            var b=KeystrokesHud.bounds(client);
            add(result,"Keystrokes",b.x(),b.y(),b.width(),b.height(),KeystrokesConfig::setPosition,KeystrokesConfig::save);
        }
        var xp=IslandXpPopup.bounds(client);
        add(result,"AxialSky XP",xp.x(),xp.y(),xp.width(),xp.height(),IslandXpHudConfig::setPosition,IslandXpHudConfig::save);
        return result;
    }
    private static void add(List<Element> elements,String id,int x,int y,int width,int height,
                            BiConsumer<Integer,Integer> move,Runnable save) {
        if(width>0&&height>0)elements.add(new Element(id,new Box(x,y,width,height),move,save));
    }
    public boolean begin(MinecraftClient client,double x,double y) {
        finish();
        var candidates=new ArrayList<Element>();
        var all=elements(client);
        for(int i=all.size()-1;i>=0;i--)if(all.get(i).bounds().contains(x,y))candidates.add(all.get(i));
        if(candidates.isEmpty()){selected=null;return false;}
        int index=0;
        if(control(client))for(int i=0;i<candidates.size();i++)if(candidates.get(i).id().equals(selected)){index=(i+1)%candidates.size();break;}
        dragging=candidates.get(index);selected=dragging.id();
        offsetX=(int)x-dragging.bounds().x();offsetY=(int)y-dragging.bounds().y();
        return true;
    }
    public boolean drag(MinecraftClient client,double x,double y) {
        if(dragging==null)return false;
        var peers=new ArrayList<Box>();
        for(var element:elements(client))if(!element.id().equals(dragging.id()))peers.add(element.bounds());
        var b=dragging.bounds();
        alignment=HudSnap.place(new Box((int)x-offsetX,(int)y-offsetY,b.width(),b.height()),peers,
                client.getWindow().getScaledWidth(),client.getWindow().getScaledHeight(),alt(client));
        dragging.move().accept(alignment.x(),alignment.y());
        return true;
    }
    public boolean finish() {
        if(dragging==null)return false;
        dragging.save().run();dragging=null;alignment=null;return true;
    }
    public void borders(DrawContext c,MinecraftClient client,int mouseX,int mouseY) {
        if(dragging!=null&&alignment!=null&&!alt(client)) {
            if(alignment.guideX()!=null)c.fill(alignment.guideX(),0,alignment.guideX()+1,c.getScaledWindowHeight(),0x70C78DF5);
            if(alignment.guideY()!=null)c.fill(0,alignment.guideY(),c.getScaledWindowWidth(),alignment.guideY()+1,0x70C78DF5);
        }
        Element hovered=null,selection=null;
        for(var element:elements(client)) {
            var b=element.bounds();boolean picked=element.id().equals(selected);
            boolean hover=b.contains(mouseX,mouseY);
            int color=picked?0xFFC78DF5:hover?0xFFDED2EA:0xAA8B8195;
            border(c,b,color);
            if(hover)hovered=element;
            if(picked)selection=element;
        }
        var labelled=selection!=null?selection:hovered;
        if(labelled!=null) {
            var b=labelled.bounds();int w=client.textRenderer.getWidth(UiTypography.text(labelled.id(),.75f,true))+8;
            int left=Math.clamp(b.x(),0,Math.max(0,c.getScaledWindowWidth()-w));
            int top=b.y()>=13?b.y()-12:b.bottom()+2;
            top=Math.clamp(top,0,Math.max(0,c.getScaledWindowHeight()-11));
            c.fill(left,top,left+w,top+11,0xE8171B22);
            c.drawText(client.textRenderer,UiTypography.text(labelled.id(),.75f,true),left+4,top+2,0xFFC78DF5,false);
        }
        String hint="Drag to move • Alt: free placement • Ctrl-click: select overlapping HUDs";
        var text=UiTypography.text(hint,.65f,false);
        int textWidth=client.textRenderer.getWidth(text);
        if(textWidth<=c.getScaledWindowWidth()-12)c.drawTextWithShadow(client.textRenderer,text,
                (c.getScaledWindowWidth()-textWidth)/2,c.getScaledWindowHeight()-12,0xFFD6CCDE);
    }
    private static void border(DrawContext c,Box b,int color) {
        c.fill(b.x(),b.y(),b.right(),b.y()+1,color);c.fill(b.x(),b.bottom()-1,b.right(),b.bottom(),color);
        c.fill(b.x(),b.y(),b.x()+1,b.bottom(),color);c.fill(b.right()-1,b.y(),b.right(),b.bottom(),color);
    }
    private static boolean alt(MinecraftClient c) { return key(c,GLFW.GLFW_KEY_LEFT_ALT)||key(c,GLFW.GLFW_KEY_RIGHT_ALT); }
    private static boolean control(MinecraftClient c) { return key(c,GLFW.GLFW_KEY_LEFT_CONTROL)||key(c,GLFW.GLFW_KEY_RIGHT_CONTROL); }
    private static boolean key(MinecraftClient c,int key) { return InputUtil.isKeyPressed(c.getWindow(),key); }
}
