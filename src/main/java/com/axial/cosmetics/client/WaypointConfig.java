package com.axial.cosmetics.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.math.BlockPos;
import java.util.*;

public final class WaypointConfig {
    private WaypointConfig() { }
    private static final class Holder {
        static final WaypointStore STORE=new WaypointStore(FabricLoader.getInstance().getConfigDir().resolve("axial-cosmetics-waypoints.json"));
    }
    public static void create(String name,String world,String dimension,BlockPos position,int color) {
        Holder.STORE.add(new Entry(UUID.randomUUID().toString(),validName(name),world,dimension,position.getX(),position.getY(),position.getZ(),color|0xFF000000,false));
    }
    public static List<Entry> waypoints(){return Holder.STORE.entries();}
    public static List<Entry> waypointsFor(MinecraftClient client){String world=worldId(client);return waypoints().stream().filter(e->world.equals(e.world())).toList();}
    public static List<Entry> visibleFor(MinecraftClient client) {
        String dimension=client.world==null?"":client.world.getRegistryKey().getValue().toString();
        return waypointsFor(client).stream().filter(e->!e.hidden()&&dimension.equals(e.dimension())).toList();
    }
    public static String worldId(MinecraftClient client) {
        ServerInfo server=client.getCurrentServerEntry();
        if(server!=null)return "server:"+server.address.toLowerCase(Locale.ROOT);
        if(client.getServer()!=null)return "singleplayer:"+client.getServer().getSaveProperties().getLevelName();
        return "unknown";
    }
    public static boolean enabled(){return Holder.STORE.enabled();}
    public static void setEnabled(boolean enabled){Holder.STORE.enabled(enabled);}
    public static void rename(Entry old,String name){if(!name.isBlank())update(old,name,old.x(),old.y(),old.z(),old.color());}
    public static void delete(Entry old){Holder.STORE.delete(old.id());}
    public static Entry setColor(Entry old,int color){return Holder.STORE.update(old.id(),e->new Entry(e.id(),e.name(),e.world(),e.dimension(),e.x(),e.y(),e.z(),color|0xFF000000,e.hidden()));}
    public static Entry setHidden(Entry old,boolean hidden){return Holder.STORE.update(old.id(),e->new Entry(e.id(),e.name(),e.world(),e.dimension(),e.x(),e.y(),e.z(),e.color(),hidden));}
    public static Entry update(Entry old,String name,int x,int y,int z,int color) {
        return Holder.STORE.update(old.id(),e->new Entry(e.id(),validName(name),e.world(),e.dimension(),x,y,z,color|0xFF000000,e.hidden()));
    }
    private static String validName(String name){String value=name.strip();if(value.isEmpty())throw new IllegalArgumentException("Enter a waypoint name");return value.substring(0,Math.min(48,value.length()));}
    public record Entry(String id,String name,String world,String dimension,int x,int y,int z,int color,boolean hidden) {
        public Entry(String name,String world,String dimension,int x,int y,int z,int color){this(UUID.randomUUID().toString(),name,world,dimension,x,y,z,color,false);}
    }
}
