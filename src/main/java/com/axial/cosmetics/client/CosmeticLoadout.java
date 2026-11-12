package com.axial.cosmetics.client;

import com.axial.cosmetics.data.CosmeticSlot;
import java.util.*;
import java.util.function.Function;

/** Items may occupy multiple body parts; replacing one never removes unrelated slots. */
final class CosmeticLoadout {
    private CosmeticLoadout() {}
    static <T> List<T> compatible(Collection<T> items,Function<T,Set<CosmeticSlot>> slots){
        var used=EnumSet.noneOf(CosmeticSlot.class);var result=new ArrayList<T>();
        for(T item:items)if(item!=null&&Collections.disjoint(used,slots.apply(item))){result.add(item);used.addAll(slots.apply(item));}
        return List.copyOf(result);
    }
    static <T> List<T> toggle(Collection<T> items,T selected,Function<T,Set<CosmeticSlot>> slots){
        var result=new ArrayList<>(items);
        if(!result.remove(selected)){
            result.removeIf(item->!Collections.disjoint(slots.apply(item),slots.apply(selected)));
            result.add(selected);
        }
        return compatible(result,slots);
    }
}
