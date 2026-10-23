package com.axial.worldmap;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

/** Paper 1.21.11 plugin; all live-world operations and network writes stay on the server thread. */
public final class AxialWorldMapPlugin extends JavaPlugin implements Listener,PluginMessageListener,TabCompleter {
    private static final int UNKNOWN=0;
    private final Map<UUID,WorldMap> maps=new HashMap<>(),sent=new HashMap<>();
    private final LinkedHashMap<UUID,Job> jobs=new LinkedHashMap<>();
    private final LinkedHashMap<UUID,Transfer> transfers=new LinkedHashMap<>();
    private final Map<UUID,Long> requests=new HashMap<>(),dirtySince=new HashMap<>();
    private final Map<UUID,Set<Long>> dirty=new HashMap<>();
    private final Set<UUID> subscribers=new HashSet<>();
    private final Queue<Runnable> completions=new ConcurrentLinkedQueue<>();
    private ExecutorService worker;
    private MapCache cache;
    private volatile boolean stopping;
    private boolean paused;
    private long tick;
    private int resolution,chunksPerTick,maxInFlight,messagesPerTick,globalInFlight;
    private double maxDiameter,minTps;
    private long refreshMillis;
    private boolean generate;
    private Set<String> worlds;

    @Override public void onEnable() {
        saveDefaultConfig();settings();cache=new MapCache(getDataFolder().toPath().resolve("maps"));
        worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"axial-world-map");t.setDaemon(true);return t;});
        getServer().getPluginManager().registerEvents(this,this);
        getServer().getMessenger().registerIncomingPluginChannel(this,MapProtocol.CHANNEL,this);
        getServer().getMessenger().registerOutgoingPluginChannel(this,MapProtocol.CHANNEL);
        Objects.requireNonNull(getCommand("axialmap")).setTabCompleter(this);
        getServer().getScheduler().runTaskTimer(this,this::tick,1,1);
        if(getConfig().getBoolean("auto-render",true))for(World world:getServer().getWorlds())if(eligible(world))ensure(world,false);
        getLogger().info("Full-world maps enabled on "+MapProtocol.CHANNEL+". /axialmap status");
    }
    private void settings() {
        resolution=Math.clamp(getConfig().getInt("resolution",512),32,512);
        chunksPerTick=Math.clamp(getConfig().getInt("chunks-per-tick",1),1,8);
        maxInFlight=Math.clamp(getConfig().getInt("max-in-flight",2),1,8);
        messagesPerTick=Math.clamp(getConfig().getInt("messages-per-tick",2),1,16);
        maxDiameter=Math.clamp(getConfig().getDouble("max-border-diameter",16384),1,60_000_000);
        minTps=Math.clamp(getConfig().getDouble("pause-below-tps",18.5),1,20);
        refreshMillis=Math.max(1,getConfig().getLong("refresh-minutes",60))*60_000L;
        generate=getConfig().getBoolean("generate-missing-chunks",true);
        worlds=new HashSet<>(getConfig().getStringList("worlds"));
    }
    private boolean eligible(World world){return (worlds.isEmpty()||worlds.contains(world.getName()))&&world.getWorldBorder().getSize()<=maxDiameter;}
    private MapGeometry geometry(World world){var b=world.getWorldBorder();return MapGeometry.forBorder(b.getCenter().getX(),b.getCenter().getZ(),b.getSize(),resolution);}
    private boolean matches(World world,MapGeometry geometry) {
        try{return eligible(world)&&geometry.equals(geometry(world));}catch(IllegalArgumentException ex){return false;}
    }
    private void ensure(World world,boolean force) {
        if(stopping||!eligible(world)||jobs.containsKey(world.getUID()))return;
        MapGeometry geometry;
        try{geometry=geometry(world);}catch(IllegalArgumentException ex){getLogger().warning(world.getName()+": "+ex.getMessage());return;}
        WorldMap existing=maps.get(world.getUID());
        if(existing!=null&&!existing.geometry().equals(geometry)){maps.remove(world.getUID());existing=null;}
        if(!force&&existing!=null&&System.currentTimeMillis()-existing.renderedAt()<refreshMillis)return;
        Job job=new Job(world.getUID(),geometry);jobs.put(job.world,job);
        String worldName=world.getName();
        final boolean readCache=!force&&existing==null;
        worker.execute(()->{
            WorldMap cached=null;
            if(readCache)try{cached=cache.read(job.world,geometry);}catch(Exception ex){getLogger().warning("Rebuilding unreadable map cache for "+worldName+": "+ex.getMessage());}
            WorldMap loaded=cached;
            boolean fresh=loaded!=null&&System.currentTimeMillis()-loaded.renderedAt()<refreshMillis;
            List<MapGeometry.ChunkSamples> plan=fresh?List.of():geometry.plan();
            complete(()->{
                if(jobs.get(job.world)!=job)return;
                World live=Bukkit.getWorld(job.world);if(live==null||!matches(live,geometry)){jobs.remove(job.world);return;}
                if(loaded!=null)maps.put(job.world,loaded);
                if(fresh){jobs.remove(job.world);sendToSubscribers(job.world);return;}
                job.tasks=new ArrayDeque<>(plan);job.total=plan.size();job.ready=true;
                getLogger().info("Rendering "+worldName+": "+geometry.width()+"x"+geometry.height()+", "+job.total+" sampled chunks");
            });
        });
    }
    private void complete(Runnable callback){if(!stopping)completions.add(callback);}
    private void tick() {
        tick++;
        for(int i=0;i<64;i++){var callback=completions.poll();if(callback==null)break;callback.run();}
        sendPackets();
        if(paused||Bukkit.getTPS()[0]<minTps)return;
        for(var entry:new ArrayList<>(dirty.entrySet())) {
            if(jobs.containsKey(entry.getKey())||tick-dirtySince.getOrDefault(entry.getKey(),tick)<40)continue;
            var map=maps.get(entry.getKey());var world=Bukkit.getWorld(entry.getKey());
            if(map==null||world==null||!eligible(world)){dirty.remove(entry.getKey());continue;}
            Job update=new Job(entry.getKey(),map.geometry());System.arraycopy(map.colors(),0,update.colors,0,update.colors.length);
            for(long key:entry.getValue()){var samples=map.geometry().samplesForChunk((int)(key>>32),(int)key);if(samples.indices().length>0)update.tasks.add(samples);}
            dirty.remove(entry.getKey());dirtySince.remove(entry.getKey());
            if(!update.tasks.isEmpty()){update.total=update.tasks.size();update.ready=true;jobs.put(entry.getKey(),update);}
        }
        // One world at a time; the global in-flight bound survives cancellation/reload.
        Job active=jobs.values().stream().filter(j->j.ready).findFirst().orElse(null);
        if(active==null)return;
        World world=Bukkit.getWorld(active.world);
        if(world==null||!matches(world,active.geometry)){jobs.remove(active.world);return;}
        for(int i=0;i<chunksPerTick&&jobs.get(active.world)==active&&globalInFlight<maxInFlight&&!active.tasks.isEmpty();i++) {
            var sample=active.tasks.removeFirst();active.inFlight++;globalInFlight++;
            try {
            world.getChunkAtAsync(sample.x(),sample.z(),generate,false).whenComplete((chunk,failure)->complete(()->{
                if(jobs.get(active.world)!=active){finished(active);return;}
                if(failure!=null){fail(active,failure);return;}
                if(chunk==null){active.missing++;finished(active);return;}
                try {
                    ChunkSnapshot snapshot=chunk.getChunkSnapshot(true,true,false);
                    int bottom=world.getMinHeight(),top=world.getMaxHeight()-1;
                    worker.execute(()->{
                        try {
                            int[] colors=render(snapshot,sample,active.geometry,bottom,top);
                            complete(()->{if(jobs.get(active.world)!=active){finished(active);return;}for(int n=0;n<colors.length;n++)active.colors[sample.indices()[n]]=colors[n];finished(active);});
                        } catch(Exception ex){complete(()->fail(active,ex));}
                    });
                } catch(Exception ex){fail(active,ex);}
            }));
            } catch(Exception ex){fail(active,ex);}
        }
    }
    private static int[] render(ChunkSnapshot snapshot,MapGeometry.ChunkSamples task,MapGeometry geometry,int bottom,int top) {
        int[] colors=new int[task.indices().length];
        for(int i=0;i<colors.length;i++) {
            int x=geometry.x(task.indices()[i])&15,z=geometry.z(task.indices()[i])&15;
            int y=Math.min(top,snapshot.getHighestBlockYAt(x,z));
            if(y<bottom){colors[i]=UNKNOWN;continue;}
            var data=snapshot.getBlockData(x,y,z);
            while(y>bottom&&(data.getMaterial().isAir()||data.getMaterial()==Material.BARRIER||data.getMaterial()==Material.STRUCTURE_VOID)){data=snapshot.getBlockData(x,--y,z);}
            int rgb=data.getMapColor().asRGB();
            int north=z>0?snapshot.getHighestBlockYAt(x,z-1):y;
            double shade=y>north?1.08:y<north?.82:1;
            int r=Math.min(255,(int)(((rgb>>16)&255)*shade)),g=Math.min(255,(int)(((rgb>>8)&255)*shade)),b=Math.min(255,(int)((rgb&255)*shade));
            colors[i]=0xFF000000|(r<<16)|(g<<8)|b;
        }
        return colors;
    }
    private void fail(Job job,Throwable error) {
        globalInFlight=Math.max(0,globalInFlight-1);job.inFlight--;
        if(jobs.get(job.world)!=job)return;
        jobs.remove(job.world);getLogger().log(Level.WARNING,"Map render stopped for "+job.world+"; retry with /axialmap render",error);
    }
    private void finished(Job job) {
        globalInFlight=Math.max(0,globalInFlight-1);job.inFlight--;
        if(jobs.get(job.world)!=job)return;
        job.done++;
        if(job.done<job.total)return;
        jobs.remove(job.world);
        var map=new WorldMap(job.world,job.geometry,System.currentTimeMillis(),job.colors);maps.put(job.world,map);
        worker.execute(()->{try{cache.write(map);}catch(Exception ex){getLogger().log(Level.WARNING,"Could not save map cache",ex);}});
        getLogger().info("Map ready for "+job.world+" ("+job.done+" sampled chunks; "+job.missing+" unavailable)");
        sendToSubscribers(job.world);
    }
    @Override public void onPluginMessageReceived(String channel,Player player,byte[] data) {
        if(!MapProtocol.CHANNEL.equals(channel)||data.length!=1||data[0]!=0||!player.hasPermission("axialworldmap.view"))return;
        long now=System.currentTimeMillis();if(now-requests.getOrDefault(player.getUniqueId(),0L)<2_000)return;
        requests.put(player.getUniqueId(),now);subscribers.add(player.getUniqueId());
        World world=player.getWorld();if(!eligible(world))return;
        ensure(world,false);WorldMap map=maps.get(world.getUID());if(map!=null&&matches(world,map.geometry()))send(player,map);
    }
    private void sendToSubscribers(UUID world) {
        var map=maps.get(world);if(map==null)return;
        for(UUID id:subscribers){Player player=Bukkit.getPlayer(id);if(player!=null&&player.isOnline()&&player.getWorld().getUID().equals(world)&&matches(player.getWorld(),map.geometry()))send(player,map);}
    }
    private void send(Player player,WorldMap map) {
        UUID id=player.getUniqueId();if(transfers.containsKey(id)||!player.hasPermission("axialworldmap.view"))return;
        WorldMap old=sent.get(id);if(old==map)return;
        Deque<Integer> stripes=TransferPlan.stripes(map,old);
        if(stripes.isEmpty()){sent.put(id,map);return;}
        transfers.put(id,new Transfer(map,stripes));
    }
    private void sendPackets() {
        for(int n=0;n<messagesPerTick&&!transfers.isEmpty();n++) {
            var iterator=transfers.entrySet().iterator();var item=iterator.next();UUID id=item.getKey();Transfer transfer=item.getValue();iterator.remove();
            Player player=Bukkit.getPlayer(id);
            if(player==null||!player.isOnline()||!player.getWorld().getUID().equals(transfer.map.world())||!player.hasPermission("axialworldmap.view")){sent.remove(id);continue;}
            try{player.sendPluginMessage(this,MapProtocol.CHANNEL,MapProtocol.snapshot(transfer.map,transfer.rows.removeFirst()));}
            catch(Exception ex){getLogger().log(Level.WARNING,"Map transfer failed",ex);sent.remove(id);continue;}
            if(transfer.rows.isEmpty())sent.put(id,transfer.map);else transfers.put(id,transfer);
        }
    }
    private void clearPlayer(Player player){UUID id=player.getUniqueId();transfers.remove(id);sent.remove(id);requests.remove(id);}
    @EventHandler public void changeWorld(PlayerChangedWorldEvent event){clearPlayer(event.getPlayer());}
    @EventHandler public void respawn(PlayerRespawnEvent event){clearPlayer(event.getPlayer());}
    @EventHandler public void quit(PlayerQuitEvent event){clearPlayer(event.getPlayer());subscribers.remove(event.getPlayer().getUniqueId());}
    @EventHandler(ignoreCancelled=true) public void unload(WorldUnloadEvent event){UUID id=event.getWorld().getUID();jobs.remove(id);maps.remove(id);dirty.remove(id);dirtySince.remove(id);}
    private void dirty(org.bukkit.block.Block block) {
        UUID id=block.getWorld().getUID();if(!maps.containsKey(id))return;
        var chunks=dirty.computeIfAbsent(id,k->new LinkedHashSet<>());
        if(chunks.size()<4096)chunks.add(((long)(block.getX()>>4)<<32)|((block.getZ()>>4)&0xFFFFFFFFL));
        dirtySince.putIfAbsent(id,tick);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void breakBlock(BlockBreakEvent e){dirty(e.getBlock());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void placeBlock(BlockPlaceEvent e){dirty(e.getBlock());}

    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!sender.hasPermission("axialworldmap.admin")){sender.sendMessage("You do not have permission.");return true;}
        String action=args.length==0?"status":args[0].toLowerCase(Locale.ROOT);
        if(action.equals("pause")){paused=true;sender.sendMessage("World map rendering paused.");return true;}
        if(action.equals("resume")){paused=false;sender.sendMessage("World map rendering resumed.");return true;}
        if(action.equals("reload")){reloadConfig();settings();sender.sendMessage("World map settings reloaded. Cached terrain is retained.");return true;}
        if(action.equals("status")) {
            sender.sendMessage("AxialWorldMap: "+maps.size()+" cached worlds, "+jobs.size()+" jobs"+(paused?" (paused)":""));
            for(var job:jobs.values()){World w=Bukkit.getWorld(job.world);sender.sendMessage((w==null?job.world:w.getName())+": "+(job.ready?job.done+"/"+job.total+" chunks, "+job.inFlight+" in flight":"loading cache / preparing"));}
            return true;
        }
        if(action.equals("render")) {
            World world=args.length>1?Bukkit.getWorld(args[1]):sender instanceof Player p?p.getWorld():null;
            if(world==null){sender.sendMessage("Usage: /axialmap render <loaded-world>");return true;}
            if(!eligible(world)){sender.sendMessage("World excluded or border too large. Set its border within "+(int)maxDiameter+" blocks, or adjust config.yml.");return true;}
            if(jobs.containsKey(world.getUID())){sender.sendMessage("A render is already queued. /axialmap status");return true;}
            ensure(world,true);sender.sendMessage("Map render queued for "+world.getName()+". /axialmap status");return true;
        }
        return false;
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
        if(!sender.hasPermission("axialworldmap.admin"))return List.of();
        if(args.length==1)return List.of("status","render","pause","resume","reload").stream().filter(v->v.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        if(args.length==2&&args[0].equalsIgnoreCase("render"))return getServer().getWorlds().stream().map(World::getName).filter(v->v.startsWith(args[1])).toList();
        return List.of();
    }
    @Override public void onDisable() {
        stopping=true;getServer().getScheduler().cancelTasks(this);jobs.clear();transfers.clear();completions.clear();
        if(worker!=null){worker.shutdown();try{if(!worker.awaitTermination(5,TimeUnit.SECONDS))worker.shutdownNow();}catch(InterruptedException ex){Thread.currentThread().interrupt();worker.shutdownNow();}}
    }
    private static final class Job {
        final UUID world;final MapGeometry geometry;final int[] colors;
        Deque<MapGeometry.ChunkSamples> tasks=new ArrayDeque<>();int total,done,inFlight,missing;boolean ready;
        Job(UUID world,MapGeometry geometry){this.world=world;this.geometry=geometry;colors=new int[geometry.width()*geometry.height()];Arrays.fill(colors,UNKNOWN);}
    }
    private record Transfer(WorldMap map,Deque<Integer> rows) { }
}
