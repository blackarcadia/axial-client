package com.axial.cosmetics.client;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Game-thread snapshots; HTTP/account verification use the existing shared worker. */
public final class PollService {
    private static List<ClientPoll> polls=List.of();
    private static long nextRefresh,updatedAt,revision,generation;
    private static boolean loading;
    private static String error="";
    private static KeyBinding openKey;
    private PollService() {}
    public static void initialize(){
        PollsConfig.load();
        openKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.axial_cosmetics.polls",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_O,KeyBinding.Category.MISC));
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->reset());
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            while(openKey.wasPressed())if(client.player!=null&&client.currentScreen==null&&PollsConfig.enabled())client.setScreen(new PollsScreen(null));
            if(client.player!=null&&PollsConfig.enabled()&&System.nanoTime()>=nextRefresh&&!loading)refresh();
        });
    }
    public static void reset(){generation++;polls=List.of();nextRefresh=System.nanoTime()+2_000_000_000L;updatedAt=0;loading=false;error="";revision++;}
    static long revision(){return revision;}
    static String error(){return error;}
    static boolean loading(){return loading;}
    static String keyLabel(){return openKey==null?"O":openKey.getBoundKeyLocalizedText().getString();}
    static List<ClientPoll> active(){
        if(!PollsConfig.enabled()||updatedAt==0||System.nanoTime()-updatedAt>180_000_000_000L)return List.of();
        return polls.stream().filter(p->p.remaining(System.nanoTime())>0).toList();
    }
    static void refresh(){
        var client=MinecraftClient.getInstance();
        if(loading||client.player==null||!PollsConfig.enabled())return;
        loading=true;long requestGeneration=generation;var session=client.getSession();var world=client.world;
        nextRefresh=System.nanoTime()+30_000_000_000L;
        FeedbackService.authenticatedRequest("polls",new JsonObject()).whenComplete((body,failure)->client.execute(()->{
            if(requestGeneration!=generation||client.getSession()!=session||client.world!=world){if(requestGeneration==generation)reset();return;}
            loading=false;
            try {
                if(failure!=null)throw failure;
                long received=System.nanoTime(),server=body.get("serverTime").getAsLong();
                var rows=body.getAsJsonArray("polls");if(rows.size()>10)throw new IllegalArgumentException();
                var parsed=new java.util.ArrayList<ClientPoll>();
                for(var row:rows)parsed.add(ClientPoll.parse(row.getAsJsonObject(),server,received));
                polls=List.copyOf(parsed);updatedAt=received;error="";
            }catch(Throwable ex){error="Polls are temporarily unavailable. Please retry.";}
            revision++;
        }));
    }
    static CompletableFuture<Void> vote(ClientPoll poll,int option){
        var client=MinecraftClient.getInstance();var session=client.getSession();long current=generation;
        var request=new JsonObject();request.addProperty("pollId",poll.id());request.addProperty("optionId",option);
        var result=new CompletableFuture<Void>();
        FeedbackService.authenticatedRequest("vote",request).whenComplete((body,failure)->client.execute(()->{
            if(current!=generation||client.getSession()!=session){result.completeExceptionally(new IllegalStateException("Your account changed. Please reopen Polls."));return;}
            if(failure==null){
                try {
                    polls=polls.stream().map(p->p.id()==poll.id()?p.recordVote(body):p).toList();revision++;
                    result.complete(null);
                }catch(RuntimeException ex){result.completeExceptionally(new java.io.IOException("Could not read the vote receipt. Refresh to check your vote.",ex));}
            }else result.completeExceptionally(failure);
            refresh();
        }));
        return result;
    }
}
