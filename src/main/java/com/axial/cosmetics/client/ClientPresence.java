package com.axial.cosmetics.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.fabricmc.loader.api.FabricLoader;

/** Anonymous, approximate online sessions. All HTTP happens off the game thread. */
public final class ClientPresence {
    private static final String SESSION_ID = UUID.randomUUID().toString();
    private static ScheduledExecutorService worker;
    private static volatile int online = -1;
    private static volatile long updatedAt;
    private static volatile boolean configured;
    private static final PresenceDisplayOffset DISPLAY_OFFSET = new PresenceDisplayOffset(new java.util.Random(), System.nanoTime());
    private ClientPresence() { }

    public static synchronized void start() {
        if (worker != null) return;
        try {
            JsonObject config;
            var path=FabricLoader.getInstance().getConfigDir().resolve("axial-presence.json");
            if(Files.exists(path)) {
                try(var reader=Files.newBufferedReader(path)) {config=JsonParser.parseReader(reader).getAsJsonObject();}
            } else {
                try(var input=ClientPresence.class.getResourceAsStream("/assets/axial_cosmetics/presence.json")) {
                    if(input==null)return;
                    config=JsonParser.parseString(new String(input.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
                }
            }
            if(config.has("enabled")&&!config.get("enabled").getAsBoolean())return;
            String url=config.has("endpoint")?config.get("endpoint").getAsString():"";
            if(url.isBlank())return;
            URI endpoint=URI.create(url);
            if(!validEndpoint(endpoint))throw new IOException("Presence endpoint must be an HTTPS URL.");
            configured=true;
            worker=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"axial-presence");t.setDaemon(true);return t;});
            worker.scheduleWithFixedDelay(()->{
                try {online=heartbeat(endpoint,SESSION_ID);updatedAt=System.nanoTime();}
                catch(Exception ignored) {online=-1;}
            },java.util.concurrent.ThreadLocalRandom.current().nextInt(6),60,TimeUnit.SECONDS);
        } catch(Exception ex) {
            org.slf4j.LoggerFactory.getLogger(ClientPresence.class).warn("Online count disabled: invalid presence configuration");
        }
    }

    static boolean validEndpoint(URI endpoint) {
        return "https".equalsIgnoreCase(endpoint.getScheme())&&endpoint.getHost()!=null
                &&endpoint.getUserInfo()==null&&endpoint.getFragment()==null;
    }

    static int parseCount(String body) throws IOException {
        try {
            var object=JsonParser.parseString(body).getAsJsonObject();
            var value=object.get("online");
            if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException();
            return value.getAsBigDecimal().intValueExact()>=0?value.getAsBigDecimal().intValueExact():reject();
        } catch(RuntimeException ex) {throw new IOException("Invalid presence response",ex);}
    }
    private static int reject() throws IOException {throw new IOException("Negative presence count");}

    static int heartbeat(URI endpoint,String session) throws IOException {
        var connection=(HttpURLConnection)endpoint.toURL().openConnection();
        connection.setConnectTimeout(3_000);connection.setReadTimeout(5_000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type","application/json");
        connection.setRequestProperty("Accept","application/json");
        connection.setRequestProperty("User-Agent","AxialClient-Presence/1.0");
        var request=new JsonObject();request.addProperty("sessionId",session);
        byte[] bytes=request.toString().getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);
        try {
            try(var output=connection.getOutputStream()){output.write(bytes);}
            if(connection.getResponseCode()!=200)throw new IOException("Presence unavailable");
            try(var input=connection.getInputStream()) {
                byte[] response=input.readNBytes(2049);
                if(response.length>2048)throw new IOException("Presence response too large");
                return parseCount(new String(response,StandardCharsets.UTF_8));
            }
        } finally {connection.disconnect();}
    }

    public static String label() {
        if(!configured||online<0||updatedAt==0||System.nanoTime()-updatedAt>TimeUnit.SECONDS.toNanos(130))return formatCount(-1);
        return formatCount(DISPLAY_OFFSET.displayedCount(online,System.nanoTime()));
    }

    static String formatCount(long count) {
        return (count<0?"—":Long.toString(count))+" Space Rangers Online";
    }

    public static synchronized void stop() {
        if(worker!=null){worker.shutdownNow();worker=null;}
        online=-1;updatedAt=0;configured=false;
    }
}
