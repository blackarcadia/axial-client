package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.axial.axialutils.client.AxialConfigManager;

import static com.axial.cosmetics.client.ModMenuUi.*;
import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** Preview values are illustrative, while visibility, style, colours and scale follow live settings. */
final class ModFeaturePreviews {
    private ModFeaturePreviews() {}
    static void draw(String feature,DrawContext c,Rect area) {
        switch(feature) {
            case "COOLDOWNS" -> {
                if(CooldownsConfig.combat())CombatCooldownHud.example(c,area.x()+(area.width()-CombatCooldownHud.SIZE)/2,area.y()+Math.max(0,(area.height()-CombatCooldownHud.SIZE)/2));
                else line(c,area,"COMBAT TIMER HIDDEN",area.y()+24,MUTED);
            }
            case "CROSSHAIR" -> CrosshairRenderer.render(c,area.x()+area.width()/2,area.y()+area.height()/2,CrosshairConfigManager.get(),1);
            case "ARMOR HUD" -> armor(c,area);
            case "MINIMAP" -> MinimapRenderer.renderSettingsPreview(c,MinecraftClient.getInstance(),area.x(),area.y(),area.width(),area.height());
            case "COMPASS" -> {
                float scale=CompassConfig.scale();
                c.getMatrices().pushMatrix();c.getMatrices().translate(area.x()+area.width()/2f,area.y()+area.height()/2f);c.getMatrices().scale(scale,scale);
                var renderer=MinecraftClient.getInstance().textRenderer;
                label(c,renderer,"NW     N     NE",-45,-10,INK,.85f,95,false);
                label(c,renderer,"|   |   |   |   |",-45,4,MUTED,.85f,95,false);
                label(c,renderer,"N",-3,18,GREEN,.85f,12,true);c.getMatrices().popMatrix();
                sample(c,area);
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
            case "SATCHEL HELPER" -> {
                var config=AxialConfigManager.get();
                line(c,area,config.satchelHudTitleText,area.y()+32,config.satchelHudTitleColor);
                line(c,area,"SATCHELS: 3",area.y()+51,config.satchelHudCountColor);
                line(c,area,"EMPTY: 1",area.y()+67,config.satchelHudEmptyColor);sample(c,area);
            }
            case "CPS" -> {
                var config=AxialConfigManager.get();line(c,area,config.cpsHudTitleText,area.y()+35,config.cpsHudTitleColor);
                line(c,area,"8 CPS",area.y()+56,config.cpsHudColor);sample(c,area);
            }
            case "POTIONS HUD" -> {
                line(c,area,PotionsHudConfig.title(),area.y()+30,PotionsHudConfig.titleColor());
                line(c,area,"SPEED II   1:42",area.y()+52,INK);line(c,area,"HASTE II   0:58",area.y()+69,INK);sample(c,area);
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
    private static void armor(DrawContext c,Rect area) {
        var config=AxialConfigManager.get();
        Item[] items={Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS,Items.DIAMOND_PICKAXE};
        String[] names={"HELMET","CHESTPLATE","LEGGINGS","BOOTS","HELD ITEM"};
        Boolean[] visible={config.armorHudShowHelmet,config.armorHudShowChestplate,config.armorHudShowLeggings,config.armorHudShowBoots,config.armorHudShowHeldItem};
        int y=area.y()+3;
        for(int i=0;i<items.length;i++)if(Boolean.TRUE.equals(visible[i])) {
            c.drawItem(new ItemStack(items[i]),area.x()+2,y);
            label(c,MinecraftClient.getInstance().textRenderer,names[i],area.x()+25,y+1,config.armorHudColor,.78f,area.width()-26,false);
            if(Boolean.TRUE.equals(config.armorHudShowDurability)) label(c,MinecraftClient.getInstance().textRenderer,
                    Boolean.TRUE.equals(config.armorHudDurabilityPercent)?(100-i*3)+"%":(400-i*20)+" / 400",area.x()+25,y+12,config.armorHudDurabilityColor,.78f,area.width()-26,false);
            y+=29;
        }
        sample(c,area);
    }
    private static void information(DrawContext c,Rect a) {
        var config=AxialConfigManager.get();int y=a.y()+4;
        line(c,a,config.informationHudTitleText,y,config.informationHudTitleColor);y+=18;
        if(InformationHudExtrasConfig.showPing()){line(c,a,"PING: 24 MS",y,InformationHudExtrasConfig.pingColor());y+=16;}
        if(InformationHudExtrasConfig.showCoordinates()){line(c,a,"XYZ: 120 64 -32",y,InformationHudExtrasConfig.coordinatesColor());y+=16;}
        if(config.showPickaxeChargePerMinute){line(c,a,"CHARGE / MIN: 420",y,config.informationHudChargeColor);y+=16;}
        if(config.showMiningXpPerMinute){line(c,a,"XP / MIN: 1,250",y,config.informationHudXpColor);y+=16;}
        if(InformationHudExtrasConfig.showServer()){line(c,a,"SERVER: EXAMPLE",y,InformationHudExtrasConfig.serverColor());y+=16;}
        if(InformationHudExtrasConfig.showFacing())line(c,a,"FACING: NORTH",y,InformationHudExtrasConfig.facingColor());
        sample(c,a);
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
