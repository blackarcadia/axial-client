package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.axial.axialutils.client.AxialConfigManager;

import static com.axial.cosmetics.client.ModMenuUi.*;
import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** HUD previews reuse in-game rendering; scene illustrations are kept separate. */
final class ModFeaturePreviews {
    private ModFeaturePreviews() {}
    static void draw(String feature,DrawContext c,Rect area) {
        switch(feature) {
            case "KEYSTROKES" -> {
                if(KeystrokesConfig.visibleMask()==0)line(c,area,"NO KEYS SELECTED",area.y()+24,MUTED);
                else KeystrokesHud.example(c,area.x()+(area.width()-KeystrokesConfig.width())/2,area.y()+Math.max(0,(area.height()-KeystrokesConfig.height())/2));
            }
            case "COOLDOWNS" -> {
                if(CooldownsConfig.combat())CombatCooldownHud.example(c,area.x()+(area.width()-CombatCooldownHud.SIZE)/2,area.y()+Math.max(0,(area.height()-CombatCooldownHud.SIZE)/2));
                else line(c,area,"COMBAT TIMER HIDDEN",area.y()+24,MUTED);
            }
            case "CROSSHAIR" -> CrosshairRenderer.render(c,area.x()+area.width()/2,area.y()+area.height()/2,CrosshairConfigManager.get(),1);
            case "ARMOR HUD" -> armor(c,area);
            case "MINIMAP" -> MinimapRenderer.renderSettingsPreview(c,MinecraftClient.getInstance(),area.x(),area.y(),area.width(),area.height());
            case "COMPASS" -> {
                var client=MinecraftClient.getInstance();
                int w=CompassHudRenderer.getWidth(client.getWindow().getScaledWidth()),h=CompassHudRenderer.getHeight();
                hud(c,area,w,h,CompassConfig.getX(client.getWindow().getScaledWidth(),w),
                        CompassConfig.getY(client.getWindow().getScaledHeight(),h),()->CompassHudRenderer.renderPreview(c,client));
            }
            case "ITEM SCALER" -> {
                scaledItem(c,Items.DIAMOND_PICKAXE,area.x()+area.width()/3,area.y()+area.height()/2,ItemScalerConfig.mainHandScale());
                scaledItem(c,Items.SHIELD,area.x()+area.width()*2/3,area.y()+area.height()/2,ItemScalerConfig.offHandScale());sample(c,area);
            }
            case "TITLE OVERLAY" -> {
                float scale=AxialConfigManager.get().titleOverlayScale;
                c.getMatrices().pushMatrix();
                boolean centered=AxialConfigManager.get().titleOverlayCentered;
                c.getMatrices().translate(centered?area.x()+area.width()/2f:area.x()+8,area.y()+area.height()/2f);c.getMatrices().scale(scale,scale);
                label(c,MinecraftClient.getInstance().textRenderer,"AXIAL CLIENT",centered?-42:0,-5,INK,1,100,true);c.getMatrices().popMatrix();sample(c,area);
            }
            case "HUD" -> information(c,area);
            case "SATCHEL HELPER" -> nativeHud(c,area,"SATCHEL HELPER");
            case "CPS" -> nativeHud(c,area,"CPS");
            case "POTIONS HUD" -> {
                var client=MinecraftClient.getInstance();
                if(client.player==null){joinWorld(c,area);break;}
                if(client.player.getStatusEffects().isEmpty()){line(c,area,"No active effects",area.y()+20,MUTED);break;}
                var b=PotionsHudRenderer.bounds(client,false);
                hud(c,area,b.width(),b.height(),b.x(),b.y(),()->PotionsHudRenderer.renderPreview(c,client));
            }
            case "CHUNK BORDERS" -> {
                int color=ChunkBordersConfig.color(),size=Math.min(area.width()-12,area.height()-30),left=area.x()+(area.width()-size)/2,top=area.y()+8;
                for(int i=0;i<=4;i++){int p=i*size/4;c.fill(left+p,top,left+p+1,top+size,color);c.fill(left,top+p,left+size,top+p+1,color);}
                sample(c,area);
            }
            default -> {}
        }
    }
    static void crosshairStyle(DrawContext c,Rect area,int index) {
        CrosshairConfig config=new CrosshairConfig();CrosshairPresets.apply(config,CrosshairConfig.CrosshairStyle.values()[index]);
        CrosshairRenderer.render(c,area.x()+area.width()/2,area.y()+area.height()/2,config,1);
    }
    private static void armor(DrawContext c,Rect area) {nativeHud(c,area,"ARMOR HUD");}
    private static void information(DrawContext c,Rect area) {nativeHud(c,area,"HUD");}
    private static void joinWorld(DrawContext c,Rect area) {line(c,area,"Join a world to preview",area.y()+20,MUTED);}
    private static void nativeHud(DrawContext c,Rect area,String feature) {
        var client=MinecraftClient.getInstance();
        if(client.player==null){joinWorld(c,area);return;}
        var config=AxialConfigManager.get();
        switch(feature) {
            case "ARMOR HUD" -> hud(c,area,org.axial.axialutils.client.ArmorHudRenderer.getWidth(client),
                    org.axial.axialutils.client.ArmorHudRenderer.getHeight(client),config.armorHudX,config.armorHudY,
                    ()->org.axial.axialutils.client.ArmorHudRenderer.render(c,client.getRenderTickCounter()));
            case "HUD" -> hud(c,area,org.axial.axialutils.client.AxialHudRenderer.getWidth(client),
                    org.axial.axialutils.client.AxialHudRenderer.getHeight(client),config.hudX,config.hudY,
                    ()->org.axial.axialutils.client.AxialHudRenderer.render(c,client.getRenderTickCounter()));
            case "CPS" -> hud(c,area,org.axial.axialutils.client.CpsHudRenderer.getWidth(client),
                    org.axial.axialutils.client.CpsHudRenderer.getHeight(client),config.cpsHudX,config.cpsHudY,
                    ()->org.axial.axialutils.client.CpsHudRenderer.render(c,client.getRenderTickCounter()));
            case "SATCHEL HELPER" -> hud(c,area,org.axial.axialutils.client.SatchelHudRenderer.getWidth(client),
                    org.axial.axialutils.client.SatchelHudRenderer.getHeight(client),config.satchelsHudX,config.satchelsHudY,
                    ()->org.axial.axialutils.client.SatchelHudRenderer.render(c,client.getRenderTickCounter()));
            default -> { }
        }
    }
    private static void hud(DrawContext c,Rect area,int width,int height,int originX,int originY,Runnable render) {
        if(width<=0||height<=0)return;
        c.getMatrices().pushMatrix();
        try {
            float fit=Math.min(1,Math.min(area.width()/(float)width,area.height()/(float)height));
            c.getMatrices().translate(area.x()+(area.width()-width*fit)/2,area.y()+(area.height()-height*fit)/2);
            c.getMatrices().scale(fit,fit);
            c.getMatrices().translate(-originX,-originY);
            render.run();
        } finally {c.getMatrices().popMatrix();}
    }
    private static void scaledItem(DrawContext c,Item item,int x,int y,float scale) {
        c.getMatrices().pushMatrix();c.getMatrices().translate(x,y);c.getMatrices().scale(scale,scale);
        c.drawItem(new ItemStack(item),-8,-8);c.getMatrices().popMatrix();
    }
    private static void line(DrawContext c,Rect area,String text,int y,int color) {
        label(c,MinecraftClient.getInstance().textRenderer,text==null?"":text,area.x()+4,y,color,.85f,area.width()-8,false);
    }
    private static void sample(DrawContext c,Rect area){line(c,area,"EXAMPLE",area.bottom()-10,MUTED);}
}
