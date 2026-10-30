package com.axial.cosmetics.client;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

/** Authoritative, read-only grant snapshots. No admin credentials are shipped in the client. */
public final class CosmeticAssignments {
    private static final URI ENDPOINT=URI.create("https://api.axialclient.com/v1/cosmetics");
    private static final URI TEMPORARY_ENDPOINT=URI.create("https://37-59-98-176.sslip.io/v1/cosmetics");
    private static final int MAX_BYTES=2*1024*1024;
    private static volatile Map<UUID,Set<String>> grants;
    private static ScheduledExecutorService worker;
    private static String etag;
    private CosmeticAssignments() { }

    static boolean owns(UUID uuid,String cape,boolean bundled) {
        var snapshot=grants;
        return snapshot==null?bundled:snapshot.getOrDefault(uuid,Set.of()).contains(cape);
    }
    static Map<UUID,Set<String>> parse(String json) throws IOException {
        try {
            var root=JsonParser.parseString(json).getAsJsonObject();
            if(root.get("version").getAsBigDecimal().intValueExact()!=1)throw new IllegalArgumentException();
            var players=root.getAsJsonObject("players");
            if(players==null||players.size()>10_000)throw new IllegalArgumentException();
            Map<UUID,Set<String>> result=new HashMap<>();
            for(var entry:players.entrySet()) {
                if(!entry.getKey().matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"))throw new IllegalArgumentException();
                var values=entry.getValue().getAsJsonArray();
                if(values.size()>64)throw new IllegalArgumentException();
                Set<String> ids=new HashSet<>();
                for(var value:values) {
                    if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString())throw new IllegalArgumentException();
                    String id=value.getAsString();
                    if(id.length()>64)throw new IllegalArgumentException();
                    if(WardrobeCape.fromId(id)!=null)ids.add(id);
                }
                result.put(UUID.fromString(entry.getKey()),Set.copyOf(ids));
            }
            return Map.copyOf(result);
        } catch(RuntimeException ex) {throw new IOException("Invalid cosmetics grant snapshot",ex);}
    }
    public static synchronized void start() {
        if(worker!=null)return;
        var client=MinecraftClient.getInstance();
        var cache=FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetic-grants-cache.json");
        try {if(Files.exists(cache)&&Files.size(cache)<=MAX_BYTES)grants=parse(Files.readString(cache));}
        catch(Exception ignored) { }
        worker=Executors.newSingleThreadScheduledExecutor(r->{Thread thread=new Thread(r,"axial-cosmetic-grants");thread.setDaemon(true);return thread;});
        worker.scheduleWithFixedDelay(()->{
            HttpURLConnection connection=null;
            try {
                int status;
                try {
                    connection=connect(ENDPOINT);status=connection.getResponseCode();
                    if(status!=200&&status!=304)throw new IOException("Primary API unavailable");
                } catch(IOException ex) {
                    if(connection!=null)connection.disconnect();
                    connection=connect(TEMPORARY_ENDPOINT);status=connection.getResponseCode();
                }
                if(status==304)return;
                if(status!=200)throw new IOException("Cosmetics API unavailable");
                String json;
                try(var input=connection.getInputStream()) {
                    byte[] bytes=input.readNBytes(MAX_BYTES+1);if(bytes.length>MAX_BYTES)throw new IOException("Snapshot too large");
                    json=new String(bytes,StandardCharsets.UTF_8);
                }
                var snapshot=parse(json);
                grants=snapshot;
                client.execute(()->{if(client.currentScreen instanceof WardrobeScreen wardrobe)wardrobe.refreshCosmetics();});
                Files.createDirectories(cache.getParent());var temporary=Files.createTempFile(cache.getParent(),"axial-grants-",".tmp");
                try {Files.writeString(temporary,json);Files.move(temporary,cache,StandardCopyOption.REPLACE_EXISTING);}
                finally {Files.deleteIfExists(temporary);}
                etag=connection.getHeaderField("ETag");
            } catch(Exception ignored) {
                // Retain the last successful snapshot; an outage must not revoke everyone's cosmetics.
            } finally {if(connection!=null)connection.disconnect();}
        },0,60,TimeUnit.SECONDS);
    }
    private static HttpURLConnection connect(URI endpoint) throws IOException {
        var connection=(HttpURLConnection)endpoint.toURL().openConnection();
        connection.setConnectTimeout(4000);connection.setReadTimeout(6000);connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept","application/json");connection.setRequestProperty("User-Agent","AxialClient-Cosmetics/1.0");
        if(etag!=null)connection.setRequestProperty("If-None-Match",etag);
        return connection;
    }
    public static synchronized void stop() {if(worker!=null){worker.shutdownNow();worker=null;}etag=null;}
}
