package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.axial.axialutils.client.AxialConfig;
import org.axial.axialutils.client.AxialConfigManager;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.*;

import static com.axial.cosmetics.client.FeatureSettingsScreen.*;

/** Binds the new controls to the existing config stores, without changing saved formats. */
final class ModFeatureSettings {
    private ModFeatureSettings() {}
    static Screen create(String feature, Screen parent, Screen backend) {
        if(feature.equals("CROSSHAIR"))return new CrosshairSettingsScreen(parent);
        if(feature.equals("MINIMAP"))return new MinimapSettingsScreen(parent);
        if(feature.equals("WAYPOINTS"))return new WaypointSettingsScreen(parent);
        Model model=switch(feature) {
            case "KEYSTROKES" -> keystrokes();
            case "COOLDOWNS" -> model("COOLDOWNS",CooldownsConfig::enabled,CooldownsConfig::toggle,
                    List.of(section("VISIBLE COOLDOWNS"),
                            new Toggle("PET COOLDOWNS",CooldownsConfig::pets,CooldownsConfig::setPets),
                            new Toggle("COMBAT TAG",CooldownsConfig::combat,CooldownsConfig::setCombat)),CooldownsConfig::reset);
            case "ARMOR HUD" -> armor();
            case "HUD" -> information();
            case "SATCHEL HELPER" -> satchel();
            case "CPS" -> cps();
            case "COMPASS" -> compass();
            case "CHUNK BORDERS" -> chunks();
            case "ITEM SCALER" -> items();
            case "POTIONS HUD" -> potions();
            case "TITLE OVERLAY" -> titles();
            default -> null;
        };
        return model==null?null:new FeatureSettingsScreen(parent,model);
    }
    static boolean open(String feature, Screen parent, Screen backend) {
        Screen screen=create(feature,parent,backend);
        if(screen==null)return false;
        MinecraftClient.getInstance().setScreen(screen);return true;
    }
    private static Model model(String title,BooleanSupplier enabled,Runnable toggle,List<Setting> rows,Runnable reset) {
        return new Model(title,enabled,toggle,List.copyOf(rows),reset,(c,area)->ModFeaturePreviews.draw(title,c,area));
    }
    private static Section section(String label){return new Section(label);}
    private static Range range(String label,DoubleSupplier get,DoubleConsumer set,double min,double max,double step,String suffix) {
        return new Range(label,get,set,new SettingRange(min,max,step),suffix);
    }
    private static List<Setting> rows(Setting...settings){return new ArrayList<>(List.of(settings));}

    private static Model keystrokes() {
        List<Setting> settings=rows(section("UNIT"),
                new Choice("LAYOUT",List.of("W / ASD","W / AS / D"),()->KeystrokesConfig.stacked()?1:0,i->KeystrokesConfig.setStacked(i==1),false),
                range("SCALE",KeystrokesConfig::scale,v->KeystrokesConfig.setScale((float)v),.5,3,.05,"X"),
                range("BACKGROUND OPACITY",()->KeystrokesConfig.opacity()*100,v->KeystrokesConfig.setOpacity((float)v/100),20,100,1,"%"),
                new Color("PRESSED BACKGROUND",KeystrokesConfig::pressedColor,KeystrokesConfig::setPressedColor),section("VISIBLE KEYS"));
        for(int i=0;i<6;i++){int bit=i;settings.add(new Toggle(KeystrokesLayout.LABELS[i],()->KeystrokesConfig.visible(bit),value->KeystrokesConfig.setVisible(bit,value)));}
        settings.add(section("INDIVIDUAL TEXT COLORS"));
        for(int i=0;i<6;i++){int bit=i;settings.add(new Color(KeystrokesLayout.LABELS[i]+" TEXT COLOR",()->KeystrokesConfig.textColor(bit),value->KeystrokesConfig.setTextColor(bit,value)));}
        return model("KEYSTROKES",KeystrokesConfig::enabled,KeystrokesConfig::toggle,settings,KeystrokesConfig::reset);
    }
    private static Model armor() {
        List<Setting> r=rows(section("ELEMENTS"));
        String[] fields={"armorHudShowHelmet","armorHudShowChestplate","armorHudShowLeggings","armorHudShowBoots","armorHudShowHeldItem","armorHudShowDurability"};
        String[] labels={"HELMET","CHEST","LEGS","BOOTS","HELD ITEM","DURABILITY"};
        for(int i=0;i<fields.length;i++)r.add(bool(labels[i],fields[i]));
        r.add(section("COLORS"));r.add(color("TITLE COLOR","armorHudColor"));r.add(color("DURABILITY COLOR","armorHudDurabilityColor"));
        r.add(new Choice("FORMAT",List.of("NUMBER","PERCENT"),()->flag("armorHudDurabilityPercent")?1:0,i->set("armorHudDurabilityPercent",i==1),false));
        return nativeModel("ARMOR HUD","showArmorHud",r,"armorHudColor","armorHudDurabilityColor","armorHudDurabilityPercent",
                "armorHudShowHelmet","armorHudShowChestplate","armorHudShowLeggings","armorHudShowBoots","armorHudShowHeldItem","armorHudShowDurability");
    }
    private static Model information() {
        List<Setting> r=rows(section("HUD OPTIONS"),
                new Toggle("PING",InformationHudExtrasConfig::showPing,InformationHudExtrasConfig::setPing),
                new Toggle("COORDINATES",InformationHudExtrasConfig::showCoordinates,InformationHudExtrasConfig::setCoordinates),
                bool("CHARGE PER MIN","showPickaxeChargePerMinute"),bool("XP PER MIN","showMiningXpPerMinute"),
                new Toggle("SERVER",InformationHudExtrasConfig::showServer,InformationHudExtrasConfig::setServer),
                new Toggle("FACING",InformationHudExtrasConfig::showFacing,InformationHudExtrasConfig::setFacing),
                section("COLORS"),color("TITLE COLOR","informationHudTitleColor"),
                new Color("PING COLOR",InformationHudExtrasConfig::pingColor,InformationHudExtrasConfig::setPingColor),
                new Color("COORDINATES COLOR",InformationHudExtrasConfig::coordinatesColor,InformationHudExtrasConfig::setCoordinatesColor),
                color("CHARGE / MIN COLOR","informationHudChargeColor"),color("XP / MIN COLOR","informationHudXpColor"),
                new Color("SERVER COLOR",InformationHudExtrasConfig::serverColor,InformationHudExtrasConfig::setServerColor),
                new Color("FACING COLOR",InformationHudExtrasConfig::facingColor,InformationHudExtrasConfig::setFacingColor));
        return model("HUD",()->flag("hudEnabled"),()->set("hudEnabled",!flag("hudEnabled")),r,()->{
            resetNative("hudEnabled","showPickaxeChargePerMinute","showMiningXpPerMinute","informationHudTitleColor","informationHudChargeColor","informationHudXpColor");
            InformationHudExtrasConfig.setPing(false);InformationHudExtrasConfig.setCoordinates(false);InformationHudExtrasConfig.setServer(false);InformationHudExtrasConfig.setFacing(false);
            InformationHudExtrasConfig.setPingColor(HudColorDefaults.SUBTITLE);InformationHudExtrasConfig.setCoordinatesColor(HudColorDefaults.SUBTITLE);
            InformationHudExtrasConfig.setServerColor(HudColorDefaults.SUBTITLE);InformationHudExtrasConfig.setFacingColor(HudColorDefaults.SUBTITLE);
        });
    }
    private static Model satchel() {
        return nativeModel("SATCHEL HELPER","showSatchelsHud",rows(section("COLORS"),color("TITLE COLOR","satchelHudTitleColor"),
                color("SATCHEL COUNT COLOR","satchelHudCountColor"),color("SATCHEL EMPTY COLOR","satchelHudEmptyColor"),
                section("DISPLAY TITLE"),new Input("TITLE",()->(String)get("satchelHudTitleText"),v->set("satchelHudTitleText",v))),
                "satchelHudTitleColor","satchelHudCountColor","satchelHudEmptyColor","satchelHudTitleText");
    }
    private static Model cps() {
        return nativeModel("CPS","showCps",rows(section("COLORS"),color("TITLE COLOR","cpsHudTitleColor"),color("CPS COLOR","cpsHudColor")),"cpsHudTitleColor","cpsHudColor");
    }
    private static Model titles() {
        return nativeModel("TITLE OVERLAY","titleOverlayEnabled",rows(section("APPEARANCE"),bool("CENTERED","titleOverlayCentered"),
                range("SCALE",()->((Number)get("titleOverlayScale")).doubleValue(),v->set("titleOverlayScale",(float)v),.5,3,.01,"X")),"titleOverlayCentered","titleOverlayScale");
    }
    private static Model potions() {
        return model("POTIONS HUD",PotionsHudConfig::isEnabled,PotionsHudConfig::toggle,rows(section("COLORS"),
                new Color("TITLE COLOR",PotionsHudConfig::titleColor,v->{PotionsHudConfig.setTitleColor(v);PotionsHudConfig.save();})),()->{
            if(PotionsHudConfig.isEnabled())PotionsHudConfig.toggle();PotionsHudConfig.setTitleColor(HudColorDefaults.TITLE);PotionsHudConfig.save();
        });
    }
    private static Model compass() {
        return model("COMPASS",CompassConfig::isEnabled,CompassConfig::toggle,rows(section("APPEARANCE"),
                range("SCALE",CompassConfig::scale,v->CompassConfig.setScale((float)v),.5,3,.01,"X")),()->{
            CompassConfig.setEnabled(true);CompassConfig.setScale(1);
        });
    }
    private static Model chunks() {
        return model("CHUNK BORDERS",ChunkBordersConfig::enabled,()->ChunkBordersConfig.setEnabled(!ChunkBordersConfig.enabled(),MinecraftClient.getInstance()),
                rows(section("APPEARANCE"),new Color("BORDER COLOR",ChunkBordersConfig::color,ChunkBordersConfig::setColor),
                        section("KEYBIND"),new Key("TOGGLE KEY",AxialCosmetics::chunkBordersToggleKey)),()->{
                    ChunkBordersConfig.setEnabled(false,MinecraftClient.getInstance());ChunkBordersConfig.setColor(0xFFFF4040);
                });
    }
    private static Model items() {
        return model("ITEM SCALER",null,null,rows(section("HELD ITEM RENDER SCALE"),
                range("MAIN HAND",ItemScalerConfig::mainHandScale,v->ItemScalerConfig.setMainHandScale((float)v),.25,3,.01,"X"),
                range("OFF HAND",ItemScalerConfig::offHandScale,v->ItemScalerConfig.setOffHandScale((float)v),.25,3,.01,"X")),()->{
            ItemScalerConfig.setMainHandScale(1);ItemScalerConfig.setOffHandScale(1);
        });
    }
    private static Model nativeModel(String title,String flag,List<Setting> rows,String...fields) {
        return model(title,()->flag(flag),()->set(flag,!flag(flag)),rows,()->{
            List<String> names=new ArrayList<>(List.of(fields));names.add(flag);resetNative(names.toArray(String[]::new));
        });
    }
    private static Toggle bool(String label,String field){return new Toggle(label,()->flag(field),v->set(field,v));}
    private static Color color(String label,String field){return new Color(label,()->(Integer)get(field),v->set(field,v));}
    private static Object get(String name) {
        try{return AxialConfig.class.getField(name).get(AxialConfigManager.get());}
        catch(ReflectiveOperationException ex){throw new IllegalStateException("Missing mod setting "+name,ex);}
    }
    private static boolean flag(String name){return Boolean.TRUE.equals(get(name));}
    private static void set(String name,Object value) {
        try {AxialConfig.class.getField(name).set(AxialConfigManager.get(),value);AxialConfigManager.save();}
        catch(ReflectiveOperationException ex){throw new IllegalStateException("Could not set "+name,ex);}
    }
    private static void resetNative(String...names) {
        AxialConfig defaults=new AxialConfig();
        try {
            for(String name:names) {
                Field field=AxialConfig.class.getField(name);
                Object value=field.get(defaults);
                if(name.endsWith("Color"))value=name.contains("Title")||name.equals("armorHudColor")?HudColorDefaults.TITLE:HudColorDefaults.SUBTITLE;
                field.set(AxialConfigManager.get(),value);
            }
            AxialConfigManager.save();
        }catch(ReflectiveOperationException ex){throw new IllegalStateException("Could not reset mod settings",ex);}
    }
}
