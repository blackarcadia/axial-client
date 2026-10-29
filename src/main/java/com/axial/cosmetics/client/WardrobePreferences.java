package com.axial.cosmetics.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

/** One equipped Axial cape per account; vanilla cape settings remain untouched. */
public final class WardrobePreferences {
    private static final Properties selections=new Properties();
    private static boolean loaded;
    private WardrobePreferences() { }

    private static void load() {
        if(loaded)return;
        loaded=true;
        var directory=FabricLoader.getInstance().getConfigDir();
        var path=directory.resolve("axial-wardrobe.properties");
        try {
            if(Files.exists(path))try(var reader=Files.newBufferedReader(path)){selections.load(reader);}
            // Preserve the owner's previous on/off preference during migration.
            String owner=CreatorCosmetics.OWNER.toString();
            var legacy=directory.resolve("axial-wardrobe-cape.txt");
            if(!selections.containsKey(owner)&&Files.exists(legacy))
                selections.setProperty(owner,Files.readString(legacy).trim().equals("hidden")?"minecraft":"gold_owner");
        } catch(IOException|IllegalArgumentException ex) {
            org.slf4j.LoggerFactory.getLogger(WardrobePreferences.class).warn("Could not read wardrobe preferences",ex);
        }
    }
    static WardrobeCape selection(Properties values,UUID uuid) {
        if(uuid==null)return null;
        String id=values.getProperty(uuid.toString());
        if("minecraft".equals(id))return null;
        WardrobeCape selected=WardrobeCape.fromId(id);
        return selected!=null&&selected.ownedBy(uuid)?selected:WardrobeCape.defaultFor(uuid);
    }
    static WardrobeCape current() {
        load();return selection(selections,MinecraftClient.getInstance().getSession().getUuidOrNull());
    }
    static WardrobeCape nextSelection(WardrobeCape equipped,WardrobeCape clicked) {
        return equipped==clicked?null:clicked;
    }
    static void toggle(WardrobeCape cape) throws IOException {
        if(!WardrobeAccess.allowed())return;
        UUID account=MinecraftClient.getInstance().getSession().getUuidOrNull();
        if(!cape.ownedBy(account))return;
        WardrobeCape selected=nextSelection(current(),cape);
        var updated=new Properties();updated.putAll(selections);
        updated.setProperty(account.toString(),selected==null?"minecraft":selected.id);
        var directory=FabricLoader.getInstance().getConfigDir();Files.createDirectories(directory);
        var temporary=Files.createTempFile(directory,"axial-wardrobe-",".tmp");
        try {
            try(var writer=Files.newBufferedWriter(temporary)){updated.store(writer,"Equipped Axial cape by Minecraft UUID");}
            Files.move(temporary,directory.resolve("axial-wardrobe.properties"),StandardCopyOption.REPLACE_EXISTING);
            selections.clear();selections.putAll(updated);
        } finally {Files.deleteIfExists(temporary);}
    }
    public static net.minecraft.entity.player.SkinTextures applyCape(UUID uuid,net.minecraft.entity.player.SkinTextures original,float age) {
        UUID account=MinecraftClient.getInstance().getSession().getUuidOrNull();
        WardrobeCape cape=uuid.equals(account)&&WardrobeAccess.allowed()?current():WardrobeCape.defaultFor(uuid);
        return cape==null?original:cape.apply(uuid,original,age);
    }
}
