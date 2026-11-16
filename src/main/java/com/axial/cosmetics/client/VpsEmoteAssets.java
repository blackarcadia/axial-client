package com.axial.cosmetics.client;

import com.google.gson.*;
import io.github.kosmx.emotes.main.EmoteHolder;
import io.github.kosmx.emotes.server.serializer.type.impl.BinaryFormat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** VPS-hosted assets; bundled copies bootstrap existing installs and remain an offline fallback. */
public final class VpsEmoteAssets {
    private static final String PRIMARY="https://api.axialclient.com/assets/emotes/",FALLBACK="https://37-59-98-176.sslip.io/assets/emotes/";
    private static ScheduledExecutorService worker;
    public static synchronized void start() {
        if(worker!=null)return;
        worker=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"axial-vps-emotes");t.setDaemon(true);return t;});
        worker.scheduleWithFixedDelay(()->{
            try{fetch(PRIMARY);}catch(Exception first){try{fetch(FALLBACK);}catch(Exception ex){org.slf4j.LoggerFactory.getLogger(VpsEmoteAssets.class).debug("Retaining cached emotes: VPS assets unavailable",ex);}}
        },0,30,TimeUnit.MINUTES);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(c->stop());
    }
    private static void fetch(String base)throws Exception {
        var root=JsonParser.parseString(new String(get(base+"catalog.json",32768),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        if(root.get("version").getAsInt()!=1)throw new IOException("Unsupported emote catalog");
        var definitions=root.getAsJsonArray("emotes");if(definitions.size()>64)throw new IOException("Emote catalog too large");
        for(var value:definitions) {
            var entry=value.getAsJsonObject();var e=WardrobeEmote.fromId(entry.get("id").getAsString());if(e==null)continue;
            String file=entry.get("file").getAsString(),hash=entry.get("sha256").getAsString();
            if(!file.equals(e.file+".emotecraft")||!entry.get("animation").getAsString().equals(e.animation.toString())||!hash.matches("[a-f0-9]{64}"))throw new IOException("Invalid emote catalog entry");
            var target=FabricLoader.getInstance().getGameDir().resolve("emotes").resolve(file);byte[] data;
            if(Files.isRegularFile(target)&&Files.size(target)<=2*1024*1024&&digest(Files.readAllBytes(target)).equals(hash))data=Files.readAllBytes(target);
            else data=get(base+file,2*1024*1024);
            verify(data,hash);
            var animations=new BinaryFormat().read(new ByteArrayInputStream(data),file);
            if(animations.size()!=1)throw new IOException("Expected one emote per asset");
            var animation=animations.values().iterator().next();if(!animation.uuid().equals(e.animation))throw new IOException("Emote animation identity mismatch");
            Files.createDirectories(target.getParent());var temporary=Files.createTempFile(target.getParent(),"axial-emote-",".tmp");
            try{Files.write(temporary,data);Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temporary);}
            MinecraftClient.getInstance().execute(()->{
                if(e.holder()==null)EmoteHolder.addEmoteToList(animation,null);
                if(MinecraftClient.getInstance().currentScreen instanceof WardrobeScreen wardrobe)wardrobe.refreshCosmetics();
            });
        }
    }
    static String digest(byte[] data)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));}
    static void verify(byte[] data,String expected)throws Exception {if(data.length>2*1024*1024||!digest(data).equals(expected))throw new IOException("Emote asset checksum mismatch");}
    private static byte[] get(String url,int limit)throws IOException {
        var c=(HttpURLConnection)URI.create(url).toURL().openConnection();c.setConnectTimeout(4000);c.setReadTimeout(6000);c.setInstanceFollowRedirects(false);
        try {if(c.getResponseCode()!=200)throw new IOException("Emote asset unavailable");try(var in=c.getInputStream()){var bytes=in.readNBytes(limit+1);if(bytes.length>limit)throw new IOException("Emote asset too large");return bytes;}}
        finally{c.disconnect();}
    }
    public static synchronized void stop(){if(worker!=null){worker.shutdownNow();worker=null;}}
}
