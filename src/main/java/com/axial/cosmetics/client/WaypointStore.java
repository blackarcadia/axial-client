package com.axial.cosmetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.UnaryOperator;

/** Disk-backed waypoint identities; migrates the original coordinate-only records. */
final class WaypointStore {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private Data data;
    WaypointStore(Path path) { this.path=path; }
    private Data data() {
        if(data!=null)return data;
        if(!Files.exists(path))return data=new Data();
        try {
            Data loaded=GSON.fromJson(Files.readString(path),Data.class);
            if(loaded==null||loaded.waypoints==null)throw new JsonParseException("Missing waypoints");
            Set<String> ids=new HashSet<>();boolean migrated=false;
            List<WaypointConfig.Entry> clean=new ArrayList<>();
            for(var e:loaded.waypoints) {
                if(e==null||e.world()==null||e.dimension()==null)continue;
                String id=e.id();
                if(id==null||id.isBlank()||!ids.add(id)){id=UUID.randomUUID().toString();ids.add(id);migrated=true;}
                clean.add(new WaypointConfig.Entry(id,e.name()==null?"Waypoint":e.name(),e.world(),e.dimension(),e.x(),e.y(),e.z(),e.color()|0xFF000000,e.hidden()));
            }
            loaded.waypoints=clean;data=loaded;
            if(migrated)save();
            return data;
        } catch(IOException|JsonParseException ex) {
            // Preserve the unreadable original before a new config can replace it.
            try { Files.copy(path,path.resolveSibling(path.getFileName()+".backup-"+System.currentTimeMillis())); }
            catch(IOException backup) { throw new IllegalStateException("Could not back up unreadable waypoints",backup); }
            org.slf4j.LoggerFactory.getLogger(WaypointStore.class).warn("Backed up unreadable waypoints",ex);
            return data=new Data();
        }
    }
    List<WaypointConfig.Entry> entries(){return List.copyOf(data().waypoints);}
    boolean enabled(){return data().enabled;}
    void enabled(boolean enabled){data().enabled=enabled;save();}
    void add(WaypointConfig.Entry entry){data().waypoints.add(entry);save();}
    WaypointConfig.Entry update(String id,UnaryOperator<WaypointConfig.Entry> change) {
        for(int i=0;i<data().waypoints.size();i++)if(Objects.equals(id,data.waypoints.get(i).id())) {
            var updated=change.apply(data.waypoints.get(i));data.waypoints.set(i,updated);save();return updated;
        }
        return null;
    }
    void delete(String id){if(data().waypoints.removeIf(e->Objects.equals(id,e.id())))save();}
    private void save() {
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Path temp=Files.createTempFile(path.toAbsolutePath().getParent(),"waypoints-",".tmp");
            try {
                Files.writeString(temp,GSON.toJson(data));
                try { Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
                catch(AtomicMoveNotSupportedException ex){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}
            } finally { Files.deleteIfExists(temp); }
        } catch(IOException ex){throw new IllegalStateException("Could not save waypoints",ex);}
    }
    private static final class Data { boolean enabled=true;List<WaypointConfig.Entry> waypoints=new ArrayList<>(); }
}
