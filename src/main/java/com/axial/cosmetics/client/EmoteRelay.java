package com.axial.cosmetics.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.kosmx.emotes.api.proxy.AbstractNetworkInstance;
import io.github.kosmx.emotes.api.proxy.EmotesProxyManager;
import io.github.kosmx.emotes.arch.network.client.ClientNetwork;
import io.github.kosmx.emotes.common.network.EmotePacket;
import io.github.kosmx.emotes.common.network.PacketBound;
import io.github.kosmx.emotes.common.network.PacketTask;
import io.github.kosmx.emotes.main.mixinFunctions.IPlayerEntity;
import io.github.kosmx.emotes.main.network.ClientPacketManager;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import net.minecraft.client.world.ClientWorld;

/** Emotecraft proxy over WSS. Native server sync retains priority. */
public final class EmoteRelay extends AbstractNetworkInstance {
    private static final URI ENDPOINT=URI.create("wss://api.axialclient.com/v1/emotes");
    private static final EmoteRelay INSTANCE=new EmoteRelay();
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"axial-emote-relay");t.setDaemon(true);return t;});
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    private final Set<UUID> animated=new HashSet<>(); // Game thread only.
    private volatile Context context;
    private volatile Connection connection;
    private volatile boolean stopping;
    private volatile long retryAt;
    private volatile int failures;
    private int ticks;
    private Set<UUID> watched=Set.of();
    private EmoteRelay() { }

    public static void initialize() {
        EmotesProxyManager.registerProxyInstance(INSTANCE);
        ClientTickEvents.END_CLIENT_TICK.register(INSTANCE::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{
            EmotePermissionState.reset();INSTANCE.reset();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client->{INSTANCE.stopping=true;INSTANCE.reset();INSTANCE.worker.shutdownNow();});
        INSTANCE.worker.scheduleWithFixedDelay(INSTANCE::maintain,1,1,TimeUnit.SECONDS);
    }
    private void tick(MinecraftClient client) {
        if(client.world==null||client.player==null||!client.player.getUuid().equals(client.getSession().getUuidOrNull())) {
            if(context!=null)reset();return;
        }
        if(context==null||context.world!=client.world||!context.session.getUuidOrNull().equals(client.getSession().getUuidOrNull())) {
            reset();context=new Context(client.world,client.getSession(),client.getApiServices().sessionService());
            return;
        }
        if(++ticks%20!=0)return;
        Connection live=connection;
        if(live==null||!live.ready||!fallback())return;
        var visible=new HashSet<UUID>();
        client.world.getPlayers().stream().filter(p->p!=client.player).sorted(java.util.Comparator.comparingDouble(p->p.squaredDistanceTo(client.player)))
                .limit(EmoteRelayProtocol.MAX_WATCH).forEach(p->visible.add(p.getUuid()));
        if(!visible.equals(watched)||!live.watchSent) {
            watched=Set.copyOf(visible);sendWatch(live);
        }
    }
    private static boolean fallback() {
        return !ClientNetwork.INSTANCE.isActive()||ClientPacketManager.isInstanceOutdatedForStreaming(ClientNetwork.INSTANCE);
    }
    private void sendWatch(Connection live) {
        var message=new JsonObject();message.addProperty("type","watch");var ids=new JsonArray();watched.forEach(id->ids.add(id.toString()));message.add("players",ids);
        live.send(message);live.watchSent=true;
    }
    private void reset() {
        context=null;watched=Set.of();ticks=0;retryAt=0;failures=0;
        Connection old=connection;connection=null;if(old!=null)old.close();
        clearAnimations();
    }
    private void clearAnimations() {
        var client=MinecraftClient.getInstance();
        if(client.world!=null&&fallback())for(UUID id:animated) {
            var player=client.world.getPlayerByUuid(id);if(player instanceof IPlayerEntity emote)emote.stopEmote();
        }
        animated.clear();
    }
    private void maintain() {
        if(stopping)return;
        try {
            Context current=context;
            if(current==null||System.nanoTime()-current.created<TimeUnit.SECONDS.toNanos(5))return;
            Connection live=connection;
            if(live!=null) {
                if(!live.ready&&System.nanoTime()-live.created>TimeUnit.SECONDS.toNanos(25))live.failed();
                return;
            }
            if(System.nanoTime()<retryAt)return;
            live=new Connection(current);connection=live;
            Connection opening=live;
            http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(8)).buildAsync(ENDPOINT,live)
                    .whenComplete((socket,error)->{if(error!=null)opening.failed();else if(connection!=opening)socket.abort();});
        }catch(Exception ignored){/* Retry without affecting the Minecraft connection. */}
    }
    @Override public boolean isActive(){var live=connection;return live!=null&&live.ready&&live.base==context&&!stopping;}
    @Override public boolean trustReceivedPlayer(){return false;}
    @Override public boolean isServerTrackingPlayState(){return false;}
    @Override public int maxDataSize(){return EmoteRelayProtocol.MAX_PACKET;}
    @Override public void sendMessage(EmotePacket packet,UUID target) {
        Connection live=connection;
        if(!isActive()||!fallback()||(packet.data.purpose!=PacketTask.STREAM&&packet.data.purpose!=PacketTask.STOP))return;
        if(target!=null&&!watched.contains(target))return;
        var buffer=Unpooled.buffer();
        try {
            packet.write(buffer,PacketBound.CLIENT);
            if(buffer.readableBytes()>maxDataSize())return;
            byte[] bytes=new byte[buffer.readableBytes()];buffer.readBytes(bytes);
            var message=new JsonObject();message.addProperty("type","packet");message.addProperty("data",Base64.getEncoder().encodeToString(bytes));
            if(target!=null)message.addProperty("target",target.toString());live.send(message);
        }catch(Exception ignored){/* Local playback and native networking must keep working. */}
        finally{buffer.release();}
    }
    private void repeat(Connection live,UUID target) {
        var client=MinecraftClient.getInstance();
        if(connection!=live||context!=live.base||!live.ready||!fallback()||client.player==null)return;
        if(target!=null&&!watched.contains(target))return;
        if(client.player instanceof IPlayerEntity player&&player.isPlayingEmote()) {
            var emote=player.emotecraft$getEmote();
            var packet=new EmotePacket.Builder().configureToStreamEmote(emote.getCurrentAnimationInstance(),client.player.getUuid())
                    .configureEmoteTick(emote.getAnimationTicks()).setSizeLimit(maxDataSize(),false).build();
            sendMessage(packet,target);
        }
    }
    private static final class Context {
        final ClientWorld world;final Session session;final com.mojang.authlib.minecraft.MinecraftSessionService service;final long created=System.nanoTime();
        Context(ClientWorld world,Session session,com.mojang.authlib.minecraft.MinecraftSessionService service){this.world=world;this.session=session;this.service=service;}
    }
    private final class Connection implements WebSocket.Listener {
        final Context base;final long created=System.nanoTime();final StringBuilder incoming=new StringBuilder();final AtomicInteger pending=new AtomicInteger();
        volatile WebSocket socket;volatile boolean ready;boolean watchSent;boolean challengeSeen;
        CompletableFuture<?> tail=CompletableFuture.completedFuture(null);
        Connection(Context base){this.base=base;}
        @Override public void onOpen(WebSocket socket){this.socket=socket;if(connection!=this||context!=base){failed();return;}socket.request(1);}
        synchronized void send(JsonObject message) {
            WebSocket ws=socket;if(ws==null||connection!=this)return;
            if(pending.incrementAndGet()>32){pending.decrementAndGet();failed();return;}
            String text=message.toString();
            tail=tail.thenCompose(ignored->ws.sendText(text,true)).whenComplete((ignored,error)->{pending.decrementAndGet();if(error!=null)failed();});
        }
        @Override public CompletionStage<?> onText(WebSocket ws,CharSequence data,boolean last) {
            if(incoming.length()+data.length()>EmoteRelayProtocol.MAX_MESSAGE){failed();return null;}
            incoming.append(data);
            if(last) {
                String text=incoming.toString();incoming.setLength(0);
                try {receive(EmoteRelayProtocol.parse(text));}catch(Exception ex){failed();}
            }
            ws.request(1);return null;
        }
        @Override public CompletionStage<?> onBinary(WebSocket ws,ByteBuffer data,boolean last){failed();return null;}
        private void receive(JsonObject message) throws IOException {
            if(connection!=this||context!=base)return;
            String type=message.get("type").getAsString();
            if(type.equals("challenge")) {
                if(challengeSeen||message.get("version").getAsInt()!=1)throw new IOException("Invalid relay handshake");
                String serverId=message.get("serverId").getAsString();
                if(!serverId.matches("[0-9a-f]{40}"))throw new IOException("Invalid relay challenge");
                challengeSeen=true;
                worker.execute(()->{
                    if(connection!=this||context!=base)return;
                    try {
                        // The access token goes only to Mojang through authlib, never to the VPS.
                        base.service.joinServer(base.session.getUuidOrNull(),base.session.getAccessToken(),serverId);
                        if(connection!=this||context!=base)return;
                        var auth=new JsonObject();auth.addProperty("type","auth");auth.addProperty("version",1);
                        auth.addProperty("name",base.session.getUsername());auth.addProperty("uuid",base.session.getUuidOrNull().toString());send(auth);
                    }catch(Exception ignored){failed();}
                });return;
            }
            if(type.equals("ready")) {
                if(!challengeSeen||ready||message.get("version").getAsInt()!=1)throw new IOException("Invalid relay handshake");
                ready=true;failures=0;
                MinecraftClient.getInstance().execute(()->{if(connection==this&&context==base){sendWatch(this);repeat(this,null);}});return;
            }
            if(!ready)throw new IOException("Unauthenticated relay message");
            UUID author=EmoteRelayProtocol.uuid(message.get("uuid").getAsString());
            if(author.equals(base.session.getUuidOrNull()))return;
            if(type.equals("request")){MinecraftClient.getInstance().execute(()->repeat(this,author));return;}
            if(type.equals("gone")) {
                MinecraftClient.getInstance().execute(()->{
                    if(connection!=this||context!=base||!animated.remove(author)||!fallback())return;
                    var player=base.world.getPlayerByUuid(author);if(player instanceof IPlayerEntity emote)emote.stopEmote();
                });return;
            }
            if(!type.equals("packet"))throw new IOException("Unknown relay message");
            byte[] bytes=EmoteRelayProtocol.decode(message.get("data").getAsString());
            var buffer=Unpooled.wrappedBuffer(bytes);EmotePacket packet;
            try{packet=new EmotePacket(buffer,PacketBound.CLIENT);}finally{buffer.release();}
            if(packet.data.purpose!=PacketTask.STREAM&&packet.data.purpose!=PacketTask.STOP)return;
            packet.data.isForced=false;packet.data.player=author;
            MinecraftClient.getInstance().execute(()->{
                if(connection!=this||context!=base||!fallback()||!watched.contains(author)||base.world.getPlayerByUuid(author)==null)return;
                receiveMessage(packet,author);
                if(packet.data.purpose==PacketTask.STREAM)animated.add(author);else animated.remove(author);
            });
        }
        @Override public CompletionStage<?> onClose(WebSocket ws,int status,String reason){failed();return null;}
        @Override public void onError(WebSocket ws,Throwable error){failed();}
        synchronized void failed() {
            ready=false;if(socket!=null)socket.abort();
            if(connection!=this)return;connection=null;
            failures=Math.min(failures+1,6);retryAt=System.nanoTime()+TimeUnit.SECONDS.toNanos(Math.min(60,1L<<failures));
            MinecraftClient.getInstance().execute(()->{if(context==base)clearAnimations();});
        }
        void close(){ready=false;if(socket!=null)socket.abort();}
    }
}
