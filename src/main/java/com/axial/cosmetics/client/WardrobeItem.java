package com.axial.cosmetics.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One presentation type for independently equipped cape and aura slots. */
record WardrobeItem(WardrobeCape cape,WardrobeAura aura) {
    static WardrobeItem of(WardrobeCape cape){return cape==null?null:new WardrobeItem(cape,null);}
    static WardrobeItem of(WardrobeAura aura){return aura==null?null:new WardrobeItem(null,aura);}
    String label(){return cape!=null?cape.label:aura.label;}
    String badge(){return cape!=null?cape.badge:aura.badge;}
    boolean ownedBy(UUID uuid){return cape!=null?cape.ownedBy(uuid):aura.ownedBy(uuid);}
    boolean equipped(){return cape!=null?WardrobePreferences.current()==cape:WardrobePreferences.currentAura()==aura;}
    void toggle()throws java.io.IOException{if(cape!=null)WardrobePreferences.toggle(cape);else WardrobePreferences.toggle(aura);}
    static List<WardrobeItem> owned(UUID uuid,WardrobeCategory category){
        var items=new ArrayList<WardrobeItem>();
        if(category.includesCapes())WardrobeCape.owned(uuid).forEach(c->items.add(of(c)));
        if(category.includesAuras())WardrobeAura.owned(uuid).forEach(a->items.add(of(a)));
        return List.copyOf(items);
    }
}
