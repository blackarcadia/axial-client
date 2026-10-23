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
        Model model=switch(feature) {
            case "CROSSHAIR" -> crosshair();
            case "MINIMAP" -> minimap(backend,parent);
            case "ARMOR HUD" -> armor();
            case "HUD" -> information();
            case "SATCHEL HELPER" -> satchel();
            case "CPS" -> cps();
            case "COMPASS" -> compass();
            case "CHUNK BORDERS" -> chunks();
            case "ITEM SCALER" -> items();
            case "POTIONS HUD" -> potions();
            case "TITLE OVERLAY" -> titles();
            case "WAYPOINTS" -> waypoints(parent,backend);
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

    private static Model crosshair() {
        CrosshairConfig c=CrosshairConfigManager.get();
        List<Setting> rows=rows(new Choice("STYLE",Arrays.stream(CrosshairConfig.CrosshairStyle.values()).map(Enum::name).toList(),
                ()->c.style.ordinal(),i->{CrosshairPresets.apply(c,CrosshairConfig.CrosshairStyle.values()[i]);CrosshairConfigManager.save();},true),
                section("APPEARANCE"),
                range("SIZE",()->c.size,v->{c.size=(float)v;CrosshairConfigManager.save();},.1,4,.01,"X"),
                range("LENGTH",()->c.length,v->{c.length=(float)v;CrosshairConfigManager.save();},0,32,2,""),
                range("WIDTH",()->c.width,v->{c.width=(float)v;CrosshairConfigManager.save();},2,12,2,""),
                range("GAP",()->c.gap,v->{c.gap=(float)v;CrosshairConfigManager.save();},0,20,2,""),
                new Color("COLOR",()->c.color,v->{c.color=v;CrosshairConfigManager.save();}),
                new Color("OUTLINE COLOR",()->c.outlineColor,v->{c.outlineColor=v;CrosshairConfigManager.save();}),
                new Toggle("OUTLINE",()->c.outlineEnabled,v->{c.outlineEnabled=v;CrosshairConfigManager.save();}),
                new Toggle("DYNAMIC",()->c.dynamicEnabled,v->{c.dynamicEnabled=v;CrosshairConfigManager.save();}));
        return model("CROSSHAIR",()->c.enabled,()->{c.enabled=!c.enabled;CrosshairConfigManager.save();},rows,()->{
            // Preserve the existing configure screen's reset preset.
            c.enabled=true;c.style=CrosshairConfig.CrosshairStyle.LUNAR;c.color=0xFFFFFFFF;c.size=1;c.length=6;c.width=2;c.gap=2;
            c.outlineEnabled=true;c.outlineColor=0xFF000000;c.dynamicEnabled=false;CrosshairConfigManager.save();
        });
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
    private static Model minimap(Screen backend,Screen parent) {
        return model("MINIMAP",MinimapConfig::isEnabled,MinimapConfig::toggle,rows(section("APPEARANCE"),
                new Choice("STYLE",List.of("SQUARE","CIRCLE"),()->MinimapConfig.isCircular()?1:0,i->MinimapConfig.setCircular(i==1),false),
                new Choice("SIZE",List.of("1.0X","1.5X","2.0X","3.0X"),()->MinimapConfig.sizeIndex(),MinimapConfig::setSizeIndex,false),
                new Choice("ZOOM",List.of("0.75X","1.0X","1.5X","2.0X"),()->MinimapConfig.zoomIndex(),MinimapConfig::setZoomIndex,false),
                new Action("MOVE HUD",()->MinecraftClient.getInstance().setScreen(ModMenuCatalog.child(backend,parent,"MOVE"))),
                section("KEYBIND"),new Key("OPEN FULL MAP",AxialCosmetics::minimapMapKey)),MinimapConfig::resetAppearance);
    }
    private static Model waypoints(Screen parent,Screen backend) {
        List<Setting> rows=rows(section("KEYBIND"),new Key("CREATE WAYPOINT",AxialCosmetics::createWaypointKey),section("SAVED LOCATIONS"));
        var entries=WaypointConfig.waypointsFor(MinecraftClient.getInstance());
        if(entries.isEmpty())rows.add(section("NO CURRENT WAYPOINTS SET"));
        for(var entry:entries) {
            var current=new java.util.concurrent.atomic.AtomicReference<>(entry);
            rows.add(section(entry.x()+" / "+entry.y()+" / "+entry.z()));
            rows.add(new Input("NAME",()->current.get().name(),value->{
                var old=current.get();WaypointConfig.rename(old,value);
                if(!value.isBlank())current.set(new WaypointConfig.Entry(value.trim(),old.world(),old.dimension(),old.x(),old.y(),old.z(),old.color()));
            }));
            rows.add(new Color("WAYPOINT COLOR",()->current.get().color(),value->current.set(WaypointConfig.setColor(current.get(),value))));
            rows.add(new Action("DELETE WAYPOINT",()->{
                WaypointConfig.delete(current.get());open("WAYPOINTS",parent,backend);
            }));
        }
        return new Model("WAYPOINTS",WaypointConfig::enabled,()->WaypointConfig.setEnabled(!WaypointConfig.enabled()),List.copyOf(rows),null,null);
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
