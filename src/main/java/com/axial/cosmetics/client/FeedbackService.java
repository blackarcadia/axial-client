package com.axial.cosmetics.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;

/** Network work and account verification never block the render thread. Tokens stay in memory. */
public final class FeedbackService {
    private static final String ENDPOINT="https://api.axialclient.com/v1/feedback/";
    private static final ScheduledExecutorService WORKER=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"axial-feedback");t.setDaemon(true);return t;});
    private static String token="";
    private static Session authenticated;
    private static long expires;
    private FeedbackService() {}

    public static void initialize() {
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->{
            var session=client.getSession();
            var service=client.getApiServices().sessionService();
            WORKER.schedule(()->poll(client,session,service),8,TimeUnit.SECONDS);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client->WORKER.shutdownNow());
    }
    public static CompletableFuture<Integer> submit(String message,UUID requestId,FeedbackCategory category,FeedbackKind kind) {
        var client=MinecraftClient.getInstance();var session=client.getSession();var service=client.getApiServices().sessionService();
        return CompletableFuture.supplyAsync(()->{
            try {
                authenticate(session,service);
                var body=new JsonObject();body.addProperty("message",message);body.addProperty("requestId",requestId.toString());body.addProperty("category",category.id);body.addProperty("type",kind.id);
                return request("submit",body,token).get("id").getAsInt();
            }catch(Exception ex){throw new java.util.concurrent.CompletionException(ex);}
        },WORKER);
    }
    /** Polls share the verified account and serialized network worker with feedback. */
    static CompletableFuture<JsonObject> authenticatedRequest(String action,JsonObject body) {
        var client=MinecraftClient.getInstance();var session=client.getSession();var service=client.getApiServices().sessionService();
        return CompletableFuture.supplyAsync(()->{
            try {authenticate(session,service);return request(action,body,token);}
            catch(Exception ex){throw new java.util.concurrent.CompletionException(ex);}
        },WORKER);
    }
    private static void authenticate(Session session,com.mojang.authlib.minecraft.MinecraftSessionService service) throws Exception {
        if(authenticated==session&&System.currentTimeMillis()<expires)return;
        token="";authenticated=null;
        if(session.getUuidOrNull()==null||session.getAccessToken().isBlank())throw new IOException("Sign in with your Minecraft account first.");
        String challenge=request("challenge",new JsonObject(),"").get("challenge").getAsString();
        if(!challenge.matches("[a-f0-9]{40}"))throw new IOException("Invalid verification response.");
        service.joinServer(session.getUuidOrNull(),session.getAccessToken(),challenge);
        var body=new JsonObject();body.addProperty("challenge",challenge);body.addProperty("username",session.getUsername());
        token=request("authenticate",body,"").get("token").getAsString();authenticated=session;expires=System.currentTimeMillis()+3_000_000;
    }
    private static void poll(MinecraftClient client,Session session,com.mojang.authlib.minecraft.MinecraftSessionService service) {
        try {
            authenticate(session,service);
            var notifications=request("notifications",new JsonObject(),token).getAsJsonArray("notifications");
            String receiptToken=token;
            client.execute(()->{
                if(client.player==null||client.getSession()!=session)return;
                var ids=new com.google.gson.JsonArray();
                for(var element:notifications) {
                    var note=element.getAsJsonObject();
                    String reply=note.get("reply").getAsString();
                    client.inGameHud.getChatHud().addMessage(Text.literal("[Axial] Your feedback #"+note.get("feedback_id").getAsInt()+" was approved. Thank you!").withColor(0xC78DF5));
                    if(!reply.isBlank())client.inGameHud.getChatHud().addMessage(Text.literal("Development team: "+reply));
                    ids.add(note.get("id").getAsInt());
                }
                if(!ids.isEmpty())WORKER.execute(()->{
                    var body=new JsonObject();body.add("ids",ids);
                    try{request("ack",body,receiptToken);}catch(Exception ignored){/* Unacknowledged notices are retried next login. */}
                });
            });
        }catch(Exception ignored){/* Feedback availability must never interfere with joining a server. */}
    }
    private static JsonObject request(String action,JsonObject body,String bearer) throws IOException {
        var connection=(HttpURLConnection)URI.create(ENDPOINT+action).toURL().openConnection();
        connection.setConnectTimeout(6000);connection.setReadTimeout(12000);connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json");
        if(!bearer.isBlank())connection.setRequestProperty("Authorization","Bearer "+bearer);
        byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);connection.setFixedLengthStreamingMode(bytes.length);
        try {
            try(var out=connection.getOutputStream()){out.write(bytes);}
            int status=connection.getResponseCode();
            if(status==401){token="";authenticated=null;}
            var stream=status==200?connection.getInputStream():connection.getErrorStream();
            if(stream==null)throw new IOException("Feedback is unavailable. Please try again.");
            try(stream) {
                byte[] response=stream.readNBytes(65537);
                if(response.length>65536)throw new IOException("Feedback response too large.");
                JsonObject result;
                try{result=JsonParser.parseString(new String(response,StandardCharsets.UTF_8)).getAsJsonObject();}
                catch(RuntimeException ex){throw new IOException("Feedback is unavailable. Please try again.");}
                if(status!=200)throw new IOException(result.has("error")?result.get("error").getAsString():"Could not send feedback. Please retry.");
                return result;
            }
        }finally{connection.disconnect();}
    }
}
