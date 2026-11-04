package com.axial.cosmetics.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One presentation type for independently equipped cape, aura and outfit slots. */
record WardrobeItem(WardrobeCape cape,WardrobeAura aura,WardrobeAccessory accessory) {
    static WardrobeItem of(WardrobeCape cape){return cape==null?null:new WardrobeItem(cape,null,null);}
    static WardrobeItem of(WardrobeAura aura){return aura==null?null:new WardrobeItem(null,aura,null);}
    static WardrobeItem of(WardrobeAccessory item){return item==null?null:new WardrobeItem(null,null,item);}
    String label(){return cape!=null?cape.label:aura!=null?aura.label:accessory.label;}
    String badge(){return cape!=null?cape.badge:aura!=null?aura.badge:accessory.badge;}
    boolean ownedBy(UUID uuid){return cape!=null?cape.ownedBy(uuid):aura!=null?aura.ownedBy(uuid):accessory.ownedBy(uuid);}
    boolean equipped(){return cape!=null?WardrobePreferences.current()==cape:aura!=null?WardrobePreferences.currentAura()==aura:WardrobePreferences.currentAccessory()==accessory;}
    void toggle()throws java.io.IOException{if(cape!=null)WardrobePreferences.toggle(cape);else if(aura!=null)WardrobePreferences.toggle(aura);else WardrobePreferences.toggle(accessory);}
    static List<WardrobeItem> owned(UUID uuid,WardrobeCategory category){
        var items=new ArrayList<WardrobeItem>();
        if(category.includesCapes())WardrobeCape.owned(uuid).forEach(c->items.add(of(c)));
        if(category.includesAuras())WardrobeAura.owned(uuid).forEach(a->items.add(of(a)));
        if(category==WardrobeCategory.ALL||category==WardrobeCategory.OUTFITS)WardrobeAccessory.owned(uuid).forEach(a->items.add(of(a)));
        return List.copyOf(items);
    }
}
