package com.axial.cosmetics.client;

import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

/** Isolated histories for nearby entitled players; cleared across worlds and account changes. */
public final class PixelStarsTrail {
    private static final Map<UUID,PixelStarsEmitter> EMITTERS=new HashMap<>();
    private static Object world;
    private static UUID account;
    private static long tick;
    private PixelStarsTrail() {}
    public static void tick(MinecraftClient client){
        var currentAccount=client.getSession().getUuidOrNull();
        if(world!=client.world||!Objects.equals(account,currentAccount)){EMITTERS.clear();world=client.world;account=currentAccount;tick=0;}
        if(client.world==null||client.player==null){EMITTERS.clear();return;}
        tick++;
        var keep=new HashSet<UUID>();
        var nearby=client.world.getPlayers().stream().filter(p->p.squaredDistanceTo(client.player)<=64*64)
                .sorted(Comparator.comparingDouble(p->p.squaredDistanceTo(client.player))).limit(24).toList();
        for(var player:nearby){
            var id=player.getUuid();
            if(player.isInvisible()||player.isSpectator()||!PlayerProfiles.isActualPlayer(id)||WardrobePreferences.auraFor(id)!=WardrobeAura.PIXEL_STARS)continue;
            keep.add(id);EMITTERS.computeIfAbsent(id,PixelStarsEmitter::new).tick(tick,player.getX(),player.getY(),player.getZ());
        }
        EMITTERS.keySet().retainAll(keep);
    }
    static List<HexwindGeometry.Quad> frame(UUID id,PlayerEntityRenderState state,float partial,boolean detailed){
        var emitter=EMITTERS.get(id);if(emitter==null)return List.of();
        return PixelStarsGeometry.render(emitter.stars(),tick+Math.clamp(partial,0,1),state.x,state.y,state.z,state.bodyYaw,detailed);
    }
}
