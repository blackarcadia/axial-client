package com.axial.worldmap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/** Weighted round-robin selection without increasing the shared chunk-loading budget. */
final class RenderScheduler {
    private RenderScheduler() { }
    static <K,V> V next(LinkedHashMap<K,V> jobs,Predicate<V> ready,Predicate<V> focused,boolean preferFocused) {
        Map.Entry<K,V> selected=null;
        for(var entry:jobs.entrySet())if(ready.test(entry.getValue())&&(!preferFocused||focused.test(entry.getValue()))){selected=entry;break;}
        if(selected==null)for(var entry:jobs.entrySet())if(ready.test(entry.getValue())){selected=entry;break;}
        if(selected==null)return null;
        K key=selected.getKey();V value=selected.getValue();
        jobs.remove(key);jobs.put(key,value);return value;
    }
}
