package com.axial.cosmetics.client;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Named local snapshots of client preferences. Account and launcher data are never included. */
final class ProfileStore {
    static final Set<String> FILES=Set.of("axialutils.json","axial-crosshair.json",
        "axial-cosmetics-performance.json","axial-cosmetics-keystrokes.json","axial-cosmetics-island-xp.json",
        "axial-cosmetics-cooldowns.json","axial-cosmetics-enchant-glint.json","axial-cosmetics-third-person-name-tags.json",
        "axial-cosmetics-item-scaler.json","axial-cosmetics-potions-hud.json","axial-cosmetics-scoreboard.json",
        "axial-cosmetics-information-hud.json","axial-cosmetics-chunk-borders.json","axial-cosmetics-menu-music.json",
        "axial-cosmetics-minimap.json");
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    record Entry(String id,String name) {}
    private static final class Saved {String id,name;Map<String,String> files=new TreeMap<>();}
    private static final class Data {int version=1;String active;List<Saved> profiles=new ArrayList<>();}
    private final Path config,file;
    ProfileStore(Path config){this.config=config;file=config.resolve("axial-profiles.json");}
    List<Entry> list() throws IOException {return read().profiles.stream().map(p->new Entry(p.id,p.name)).toList();}
    String active() throws IOException {return read().active;}
    Entry create(String name) throws IOException {
        var data=read();name=validName(data,name,null);
        var saved=new Saved();saved.id=UUID.randomUUID().toString();saved.name=name;saved.files=capture();
        data.profiles.add(saved);data.active=saved.id;write(data);return new Entry(saved.id,name);
    }
    void update(String id) throws IOException {var data=read();find(data,id).files=capture();data.active=id;write(data);}
    void rename(String id,String name) throws IOException {var data=read();find(data,id).name=validName(data,name,id);write(data);}
    void delete(String id) throws IOException {var data=read();find(data,id);data.profiles.removeIf(p->p.id.equals(id));if(id.equals(data.active))data.active=null;write(data);}
    void activate(String id,Runnable reload) throws IOException {
        var data=read();var saved=find(data,id);validateFiles(saved.files);
        // Preserve the first unsaved setup as a profile before replacing any settings.
        if(data.active==null){var original=new Saved();original.id=UUID.randomUUID().toString();
            String name="Previous setup";int n=2;while(hasName(data,name,null))name="Previous setup "+n++;
            original.name=name;original.files=capture();data.profiles.add(original);}
        var backup=new HashMap<String,String>();
        for(String name:saved.files.keySet()){var path=config.resolve(name);backup.put(name,Files.exists(path)?Files.readString(path):null);}
        try {restore(saved.files);reload.run();data.active=id;write(data);}
        catch(IOException|RuntimeException ex){
            try{restore(backup);reload.run();}catch(IOException|RuntimeException rollback){ex.addSuppressed(rollback);}
            throw new IOException("Could not load profile; previous settings were restored.",ex);
        }
    }
    private Map<String,String> capture() throws IOException {
        var result=new TreeMap<String,String>();
        for(String name:FILES){var path=config.resolve(name);if(Files.exists(path))result.put(name,Files.readString(path));}
        if(result.isEmpty())throw new IOException("No client settings to save.");validateFiles(result);return result;
    }
    private void restore(Map<String,String> files) throws IOException {
        for(var entry:files.entrySet()){var path=config.resolve(entry.getKey());if(entry.getValue()==null)Files.deleteIfExists(path);else atomic(path,entry.getValue());}
    }
    private Data read() throws IOException {
        if(!Files.exists(file))return new Data();
        try {
            var data=GSON.fromJson(Files.readString(file),Data.class);
            if(data==null||data.version!=1||data.profiles==null)throw new IllegalArgumentException();
            Set<String> ids=new HashSet<>();Set<String> names=new HashSet<>();
            for(var p:data.profiles){UUID.fromString(p.id);if(!ids.add(p.id)||!names.add(normalize(p.name)))throw new IllegalArgumentException();validateFiles(p.files);}
            if(data.active!=null&&!ids.contains(data.active))throw new IllegalArgumentException();return data;
        }catch(RuntimeException ex){throw new IOException("Could not read saved profiles.",ex);}
    }
    private void write(Data data) throws IOException {atomic(file,GSON.toJson(data));}
    private static void atomic(Path path,String content) throws IOException {
        Files.createDirectories(path.getParent());var temp=Files.createTempFile(path.getParent(),".profile-",".tmp");
        try{Files.writeString(temp,content);try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ex){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}}
        finally{Files.deleteIfExists(temp);}
    }
    private static void validateFiles(Map<String,String> files) throws IOException {
        if(files==null||files.isEmpty()||!FILES.containsAll(files.keySet()))throw new IOException("Invalid profile settings.");
        try{for(String value:files.values())if(value==null||!JsonParser.parseString(value).isJsonObject())throw new IllegalArgumentException();}
        catch(RuntimeException ex){throw new IOException("Invalid profile settings.",ex);}
    }
    private static Saved find(Data data,String id) throws IOException {return data.profiles.stream().filter(p->p.id.equals(id)).findFirst().orElseThrow(()->new IOException("Profile no longer exists."));}
    private static String normalize(String name){if(name==null||name.isBlank()||name.strip().length()>32||name.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Use a profile name of 1–32 characters.");return name.strip().toUpperCase(Locale.ROOT);}
    private static boolean hasName(Data data,String name,String except){return data.profiles.stream().anyMatch(p->!p.id.equals(except)&&normalize(p.name).equals(normalize(name)));}
    private static String validName(Data data,String name,String except){normalize(name);if(hasName(data,name,except))throw new IllegalArgumentException("A profile with that name already exists.");return name.strip();}
}
