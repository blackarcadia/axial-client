package com.axial.worldmap;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.WorldLoadEvent;
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
    private int initialPackets,loadedChunksPerTick;
    private long renderBudgetNanos;
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
        // Let other plugins finish restoring their worlds and borders before planning coverage.
        getServer().getScheduler().runTask(this,()->{
            if(getConfig().getBoolean("auto-render",true))for(World world:getServer().getWorlds())if(eligible(world))ensure(world,getConfig().getBoolean("refresh-on-startup",true));
        });
        getLogger().info("Full-world maps enabled on "+MapProtocol.CHANNEL+". /axialmap status");
    }
    private void settings() {
        resolution=Math.clamp(getConfig().getInt("resolution",512),32,512);
        chunksPerTick=Math.clamp(getConfig().getInt("chunks-per-tick",1),1,8);
        maxInFlight=Math.clamp(getConfig().getInt("max-in-flight",2),1,8);
        messagesPerTick=Math.clamp(getConfig().getInt("messages-per-tick",2),1,16);
        initialPackets=Math.clamp(getConfig().getInt("initial-transfer-packets-per-tick",12),1,32);
        loadedChunksPerTick=Math.clamp(getConfig().getInt("loaded-chunks-per-tick",64),1,256);
        renderBudgetNanos=(long)(Math.clamp(getConfig().getDouble("render-time-budget-ms",2),.25,10)*1_000_000);
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
        if(!force&&existing!=null&&existing.complete()&&System.currentTimeMillis()-existing.renderedAt()<refreshMillis)return;
        Job job=new Job(world.getUID(),geometry);jobs.put(job.world,job);
        String worldName=world.getName();
        final boolean readCache=existing==null;
        final WorldMap previous=existing;
        Set<Long> loadedChunks=new HashSet<>();
        for(Chunk chunk:world.getLoadedChunks())loadedChunks.add(chunkKey(chunk.getX(),chunk.getZ()));
        var focus=world.getPlayers().isEmpty()?world.getSpawnLocation():world.getPlayers().getFirst().getLocation();
        int focusX=focus.getBlockX()>>4,focusZ=focus.getBlockZ()>>4;
        worker.execute(()->{
            WorldMap cached=previous;
            if(readCache)try{cached=cache.read(job.world,geometry);}catch(Exception ex){getLogger().warning("Rebuilding unreadable map cache for "+worldName+": "+ex.getMessage());}
            WorldMap checkpoint=null;
            if(readCache)try{checkpoint=cache.readCheckpoint(job.world,geometry);}catch(Exception ex){getLogger().warning("Ignoring unreadable render checkpoint for "+worldName+": "+ex.getMessage());}
            WorldMap loaded=cached!=null&&cached.complete()?cached:null;
            WorldMap resume=checkpoint;
            // Only finished snapshots are visible; staging progress is kept in a separate file.
            if(loaded!=null||resume!=null)complete(()->{
                if(jobs.get(job.world)!=job)return;
                World live=Bukkit.getWorld(job.world);if(live==null||!matches(live,geometry))return;
                if(loaded!=null){maps.put(job.world,loaded);sendToSubscribers(job.world);}
                WorldMap seed=resume!=null?resume:loaded;
                System.arraycopy(seed.colors(),0,job.colors,0,job.colors.length);
                if(resume!=null)job.sampled.or(resume.sampled());
            });
            boolean fresh=resume==null&&!force&&loaded!=null&&System.currentTimeMillis()-loaded.renderedAt()<refreshMillis;
            List<MapGeometry.ChunkSamples> all=fresh?List.of():geometry.plan();
            List<MapGeometry.ChunkSamples> plan=new ArrayList<>();
            for(var chunk:all)if(resume==null||!resume.hasChunk(chunk))plan.add(chunk);
            plan.sort(Comparator.<MapGeometry.ChunkSamples>comparingInt(chunk->loadedChunks.contains(chunkKey(chunk.x(),chunk.z()))?0:1)
                    .thenComparingLong(chunk->{long dx=(long)chunk.x()-focusX,dz=(long)chunk.z()-focusZ;return dx*dx+dz*dz;}));
            complete(()->{
                if(jobs.get(job.world)!=job)return;
                World live=Bukkit.getWorld(job.world);if(live==null||!matches(live,geometry)){jobs.remove(job.world);return;}
                if(fresh) {
                    jobs.remove(job.world);sendToSubscribers(job.world);
                    return;
                }
                job.tasks=new ArrayDeque<>(plan);job.total=all.size();job.done=all.size()-plan.size();job.ready=true;
                if(plan.isEmpty()){finishMap(job);return;}
                getLogger().info("Rendering "+worldName+": "+geometry.width()+"x"+geometry.height()+", "+job.total+" sampled chunks");
            });
        });
    }
    private void complete(Runnable callback){if(!stopping)completions.add(callback);}
    private void tick() {
        tick++;
        long deadline=System.nanoTime()+renderBudgetNanos;
        for(int i=0;i<64&&System.nanoTime()<deadline;i++){var callback=completions.poll();if(callback==null)break;callback.run();}
        sendPackets();
        for(Job job:jobs.values())if(job.ready&&job.fullScan&&job.done>0&&tick-job.lastCheckpoint>=600)checkpoint(job);
        if(paused||Bukkit.getTPS()[0]<minTps)return;
        for(var entry:new ArrayList<>(dirty.entrySet())) {
            if(jobs.containsKey(entry.getKey())||tick-dirtySince.getOrDefault(entry.getKey(),tick)<40)continue;
            var map=maps.get(entry.getKey());var world=Bukkit.getWorld(entry.getKey());
            if(map==null||world==null||!eligible(world)){dirty.remove(entry.getKey());continue;}
            // A failed/paused initial render must resume its remaining coverage first.
            if(!map.complete()){ensure(world,false);continue;}
            Job update=new Job(entry.getKey(),map.geometry());System.arraycopy(map.colors(),0,update.colors,0,update.colors.length);
            update.fullScan=false;
            for(long key:entry.getValue()){var samples=map.geometry().samplesForChunk((int)(key>>32),(int)key);if(samples.indices().length>0)update.tasks.add(samples);}
            dirty.remove(entry.getKey());dirtySince.remove(entry.getKey());
            if(!update.tasks.isEmpty()){update.total=update.tasks.size();update.ready=true;jobs.put(entry.getKey(),update);}
        }
        // Rotate ready jobs; three out of four ticks prefer worlds with players.
        Job active=RenderScheduler.next(jobs,j->j.ready&&!j.tasks.isEmpty(),j->{
            World world=Bukkit.getWorld(j.world);return world!=null&&!world.getPlayers().isEmpty();
        },tick%4!=0);
        if(active!=null)load(active,deadline);
        if(tick%1200==0)for(World world:getServer().getWorlds())if(eligible(world))ensure(world,false);
    }

    private static long chunkKey(int x,int z){return ((long)x<<32)|(z&0xffffffffL);}
    private void load(Job active,long deadline) {
        World world=Bukkit.getWorld(active.world);
        if(world==null||!matches(world,active.geometry)){jobs.remove(active.world);return;}
        // Loaded terrain needs only its sampled surface columns, not a full chunk snapshot.
        for(int i=0;i<loadedChunksPerTick&&jobs.get(active.world)==active&&!active.tasks.isEmpty()&&System.nanoTime()<deadline;i++) {
            var sample=active.tasks.peekFirst();if(!world.isChunkLoaded(sample.x(),sample.z()))break;
            active.tasks.removeFirst();active.inFlight++;globalInFlight++;
            try{apply(active,sample,render(world,sample,active.geometry));finished(active);}
            catch(Exception ex){fail(active,ex);return;}
        }
        for(int i=0;i<chunksPerTick&&jobs.get(active.world)==active&&globalInFlight<maxInFlight&&!active.tasks.isEmpty();i++) {
            var sample=active.tasks.removeFirst();active.inFlight++;globalInFlight++;
            try {
            world.getChunkAtAsync(sample.x(),sample.z(),generate,false).whenComplete((chunk,failure)->complete(()->{
                if(jobs.get(active.world)!=active){finished(active);return;}
                if(failure!=null){fail(active,failure);return;}
                if(chunk==null){active.missing++;markSampled(active,sample);finished(active);return;}
                try {
                    if(!world.isChunkLoaded(sample.x(),sample.z())) {
                        active.inFlight--;globalInFlight--;active.tasks.addFirst(sample);return;
                    }
                    apply(active,sample,render(world,sample,active.geometry));finished(active);
                } catch(Exception ex){fail(active,ex);}
            }));
            } catch(Exception ex){fail(active,ex);}
        }
    }
    private static void apply(Job job,MapGeometry.ChunkSamples sample,int[] colors) {
        for(int n=0;n<colors.length;n++)job.colors[sample.indices()[n]]=colors[n];markSampled(job,sample);
    }
    private static int[] render(World world,MapGeometry.ChunkSamples task,MapGeometry geometry) {
        int[] colors=new int[task.indices().length];
        int bottom=world.getMinHeight(),top=world.getMaxHeight()-1;
        for(int i=0;i<colors.length;i++) {
            int x=geometry.x(task.indices()[i]),z=geometry.z(task.indices()[i]);
            int y=Math.min(top,world.getHighestBlockYAt(x,z));
            if(y<bottom){colors[i]=UNKNOWN;continue;}
            var data=world.getBlockAt(x,y,z).getBlockData();
            while(y>bottom&&(data.getMaterial().isAir()||data.getMaterial()==Material.BARRIER||data.getMaterial()==Material.STRUCTURE_VOID)){data=world.getBlockAt(x,--y,z).getBlockData();}
            int rgb=data.getMapColor().asRGB();
            int north=(z&15)>0?world.getHighestBlockYAt(x,z-1):y;
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
        finishMap(job);
    }
    private static void markSampled(Job job,MapGeometry.ChunkSamples chunk){for(int index:chunk.indices())job.sampled.set(index);}
    private void finishMap(Job job) {
        job.ready=false; // Keep the job registered until the durable snapshot is committed.
        var previous=maps.get(job.world);
        long version=Math.max(System.currentTimeMillis(),previous==null?0:previous.renderedAt()+1);
        var map=new WorldMap(job.world,job.geometry,version,job.colors);
        worker.execute(()->{
            try {
                cache.write(map);
                complete(()->{
                    if(jobs.get(job.world)!=job)return;
                    World world=Bukkit.getWorld(job.world);
                    if(world==null||!matches(world,job.geometry)){jobs.remove(job.world);return;}
                    maps.put(job.world,map);jobs.remove(job.world);
                    getLogger().info("Full map cached for "+world.getName()+" ("+job.done+" sampled chunks; "+job.missing+" unavailable)");
                    sendToSubscribers(job.world);
                });
            } catch(Exception ex){complete(()->{if(jobs.get(job.world)==job)jobs.remove(job.world);getLogger().log(Level.WARNING,"Could not commit full map cache; previous snapshot retained",ex);});}
        });
    }
    private void checkpoint(Job job) {
        if(!job.fullScan)return;
        job.lastCheckpoint=tick;
        var map=new WorldMap(job.world,job.geometry,System.currentTimeMillis(),job.colors.clone(),job.sampled);
        worker.execute(()->{try{cache.write(map);}catch(Exception ex){getLogger().log(Level.WARNING,"Could not checkpoint map render",ex);}});
    }
    @Override public void onPluginMessageReceived(String channel,Player player,byte[] data) {
        if(!MapProtocol.CHANNEL.equals(channel)||!MapProtocol.isRequest(data)||!player.hasPermission("axialworldmap.view"))return;
        long now=System.currentTimeMillis();if(now-requests.getOrDefault(player.getUniqueId(),0L)<2_000)return;
        requests.put(player.getUniqueId(),now);subscribers.add(player.getUniqueId());
        // A new client world has no previous stripes, even if this map is unchanged.
        if(MapProtocol.requestsFullMap(data)){sent.remove(player.getUniqueId());transfers.remove(player.getUniqueId());}
        World world=player.getWorld();
        if(!eligible(world)) {
            status(player,world.getWorldBorder().getSize()>maxDiameter?"World border exceeds the full-map limit":"Full map disabled for this world",0,0);
            return;
        }
        // Requests only deliver prepared data. Startup and maintenance own terrain scans.
        WorldMap map=maps.get(world.getUID());
        if(map!=null&&matches(world,map.geometry())) {
            status(player,"Loading cached full-world map",0,0);
            send(player,map);
        }
        else {
            Job job=jobs.get(world.getUID());
            status(player,paused?"Full-map rendering paused":job==null?"Full map unavailable; server render needs attention":job.ready?"Rendering full-world map":"Preparing full-world map",job==null?0:job.done,job==null?0:job.total);
        }
    }
    private void status(Player player,String message,int done,int total) {
        try{player.sendPluginMessage(this,MapProtocol.CHANNEL,MapProtocol.status(message,done,total));}
        catch(Exception ex){getLogger().log(Level.FINE,"Could not send map status",ex);}
    }
    private void sendToSubscribers(UUID world) {
        var map=maps.get(world);if(map==null)return;
        for(UUID id:subscribers){Player player=Bukkit.getPlayer(id);if(player!=null&&player.isOnline()&&player.getWorld().getUID().equals(world)&&matches(player.getWorld(),map.geometry())) {
            status(player,"Full map ready",0,0);
            send(player,map);
        }}
    }
    private void send(Player player,WorldMap map) {
        UUID id=player.getUniqueId();if(transfers.containsKey(id)||!player.hasPermission("axialworldmap.view"))return;
        WorldMap old=sent.get(id);if(old==map)return;
        Deque<Integer> stripes=TransferPlan.stripes(map,null);
        if(stripes.isEmpty()){sent.put(id,map);return;}
        transfers.put(id,new Transfer(map,stripes,old==null));
    }
    private void sendPackets() {
        int budget=MapRefreshPolicy.packetBudget(messagesPerTick,initialPackets,transfers.values().stream().anyMatch(Transfer::initial));
        for(int n=0;n<budget&&!transfers.isEmpty();n++) {
            var iterator=transfers.entrySet().iterator();var item=iterator.next();UUID id=item.getKey();Transfer transfer=item.getValue();iterator.remove();
            Player player=Bukkit.getPlayer(id);
            if(player==null||!player.isOnline()||!player.getWorld().getUID().equals(transfer.map.world())||!player.hasPermission("axialworldmap.view")){sent.remove(id);continue;}
            try{player.sendPluginMessage(this,MapProtocol.CHANNEL,MapProtocol.completeSnapshot(transfer.map,transfer.rows.removeFirst()));}
            catch(Exception ex){getLogger().log(Level.WARNING,"Map transfer failed",ex);sent.remove(id);continue;}
            if(transfer.rows.isEmpty()) {
                sent.put(id,transfer.map);
                WorldMap latest=maps.get(transfer.map.world());
                if(latest!=null&&latest!=transfer.map)send(player,latest);
            } else transfers.put(id,transfer);
        }
    }
    private void clearPlayer(Player player){UUID id=player.getUniqueId();transfers.remove(id);sent.remove(id);requests.remove(id);}
    private void arriving(Player player) {
        clearPlayer(player);UUID expected=player.getWorld().getUID();
        getServer().getScheduler().runTaskLater(this,()->{
            if(!player.isOnline()||!player.getWorld().getUID().equals(expected)||!player.hasPermission("axialworldmap.view")
                    ||!player.getListeningPluginChannels().contains(MapProtocol.CHANNEL))return;
            World world=player.getWorld();if(!eligible(world))return;
            UUID id=player.getUniqueId();subscribers.add(id);
            WorldMap map=maps.get(expected);
            if(map!=null&&matches(world,map.geometry()))send(player,map);
        },1L);
    }
    @EventHandler public void join(PlayerJoinEvent event){arriving(event.getPlayer());}
    @EventHandler public void changeWorld(PlayerChangedWorldEvent event){arriving(event.getPlayer());}
    @EventHandler public void respawn(PlayerRespawnEvent event){arriving(event.getPlayer());}
    @EventHandler public void quit(PlayerQuitEvent event){clearPlayer(event.getPlayer());subscribers.remove(event.getPlayer().getUniqueId());}
    @EventHandler(ignoreCancelled=true) public void unload(WorldUnloadEvent event){UUID id=event.getWorld().getUID();Job job=jobs.remove(id);if(job!=null&&job.ready&&job.done>0)checkpoint(job);maps.remove(id);dirty.remove(id);dirtySince.remove(id);}
    @EventHandler public void loaded(WorldLoadEvent event){
        UUID id=event.getWorld().getUID();
        getServer().getScheduler().runTask(this,()->{
            World world=Bukkit.getWorld(id);
            if(world!=null&&getConfig().getBoolean("auto-render",true)&&eligible(world))ensure(world,getConfig().getBoolean("refresh-on-startup",true));
        });
    }
    private void dirty(org.bukkit.block.Block block) {
        UUID id=block.getWorld().getUID();if(!maps.containsKey(id)&&!jobs.containsKey(id))return;
        var chunks=dirty.computeIfAbsent(id,k->new LinkedHashSet<>());
        if(chunks.size()<4096)chunks.add(((long)(block.getX()>>4)<<32)|((block.getZ()>>4)&0xFFFFFFFFL));
        dirtySince.putIfAbsent(id,tick);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void breakBlock(BlockBreakEvent e){dirty(e.getBlock());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void placeBlock(BlockPlaceEvent e){dirty(e.getBlock());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void explode(BlockExplodeEvent e){e.blockList().forEach(this::dirty);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void entityExplode(EntityExplodeEvent e){e.blockList().forEach(this::dirty);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void extend(BlockPistonExtendEvent e){e.getBlocks().forEach(block->{dirty(block);dirty(block.getRelative(e.getDirection()));});}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void retract(BlockPistonRetractEvent e){e.getBlocks().forEach(block->{dirty(block);dirty(block.getRelative(e.getDirection()));dirty(block.getRelative(e.getDirection().getOppositeFace()));});}

    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!sender.hasPermission("axialworldmap.admin")){sender.sendMessage("You do not have permission.");return true;}
        String action=args.length==0?"status":args[0].toLowerCase(Locale.ROOT);
        if(action.equals("pause")){paused=true;sender.sendMessage("World map rendering paused.");return true;}
        if(action.equals("resume")){paused=false;sender.sendMessage("World map rendering resumed.");return true;}
        if(action.equals("reload")){reloadConfig();settings();sender.sendMessage("World map settings reloaded. Cached terrain is retained.");return true;}
        if(action.equals("status")) {
            long completed=maps.values().stream().filter(WorldMap::complete).count();
            sender.sendMessage("AxialWorldMap: "+completed+" complete maps, "+(maps.size()-completed)+" partial maps, "+jobs.size()+" jobs"+(paused?" (paused)":""));
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
        stopping=true;getServer().getScheduler().cancelTasks(this);
        if(worker!=null)for(Job job:jobs.values())if(job.ready&&job.done>0)checkpoint(job);
        jobs.clear();transfers.clear();completions.clear();
        if(worker!=null){worker.shutdown();try{if(!worker.awaitTermination(5,TimeUnit.SECONDS))worker.shutdownNow();}catch(InterruptedException ex){Thread.currentThread().interrupt();worker.shutdownNow();}}
    }
    private static final class Job {
        final UUID world;final MapGeometry geometry;final int[] colors;
        final BitSet sampled=new BitSet();long lastCheckpoint;
        Deque<MapGeometry.ChunkSamples> tasks=new ArrayDeque<>();int total,done,inFlight,missing;boolean ready,fullScan=true;
        Job(UUID world,MapGeometry geometry){this.world=world;this.geometry=geometry;colors=new int[geometry.width()*geometry.height()];Arrays.fill(colors,UNKNOWN);}
    }
    private record Transfer(WorldMap map,Deque<Integer> rows,boolean initial) { }
}
