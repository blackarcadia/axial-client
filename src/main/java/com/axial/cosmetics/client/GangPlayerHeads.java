package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import java.util.*;
import java.util.concurrent.*;

/** Only visible heads request skins; bounded background work never blocks rendering. */
final class GangPlayerHeads {
    private static final UUID UNKNOWN=new UUID(0,0);
    private static final ThreadPoolExecutor LOOKUPS=new ThreadPoolExecutor(2,2,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(64),r->{Thread t=new Thread(r,"axial-gang-skins");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    private static final Map<UUID,SkinTextures> skins=new LinkedHashMap<>(256,.75f,true);
    private static final Set<UUID> requested=new HashSet<>();
    private static final Map<UUID,Long> retryAt=new HashMap<>();
    private static long generation;
    private GangPlayerHeads(){}
    static void reset(){generation++;LOOKUPS.getQueue().clear();skins.clear();requested.clear();retryAt.clear();}
    static void draw(DrawContext c,UUID id,int x,int y,int size){
        var client=MinecraftClient.getInstance();UUID uuid=id==null?UNKNOWN:id;
        SkinTextures skin=null;
        if(client.getNetworkHandler()!=null){var player=client.getNetworkHandler().getPlayerListEntry(uuid);if(player!=null)skin=player.getSkinTextures();}
        if(skin==null){skin=skins.get(uuid);if(skin==null){skin=DefaultSkinHelper.getSkinTextures(uuid);cache(uuid,skin);}request(uuid,client);}
        var texture=skin.body().texturePath();
        c.drawTexture(RenderPipelines.GUI_TEXTURED,texture,x,y,8,8,size,size,8,8,64,64);
        c.drawTexture(RenderPipelines.GUI_TEXTURED,texture,x,y,40,8,size,size,8,8,64,64);
    }
    private static void cache(UUID uuid,SkinTextures skin){
        if(skins.size()>=256&&!skins.containsKey(uuid)){UUID oldest=skins.keySet().iterator().next();skins.remove(oldest);requested.remove(oldest);retryAt.remove(oldest);}
        skins.put(uuid,skin);
    }
    private static void request(UUID uuid,MinecraftClient client){
        if(uuid.equals(UNKNOWN)||System.currentTimeMillis()<retryAt.getOrDefault(uuid,0L)||!requested.add(uuid))return;long stamp=generation;
        try{
            LOOKUPS.execute(()->{
                try{
                    var profile=client.getApiServices().sessionService().fetchProfile(uuid,true);
                    if(profile==null)return;
                    client.execute(()->{
                        if(stamp!=generation)return;
                        client.getSkinProvider().fetchSkinTextures(profile.profile()).thenAccept(result->client.execute(()->{if(stamp==generation)result.ifPresent(skin->cache(uuid,skin));}));
                    });
                }catch(Exception ignored){/* Native default head remains available when the skin service is offline. */}
            });
        }catch(RejectedExecutionException ex){requested.remove(uuid);retryAt.put(uuid,System.currentTimeMillis()+5000);}
    }
}
