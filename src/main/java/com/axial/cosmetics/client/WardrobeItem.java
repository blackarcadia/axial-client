package com.axial.cosmetics.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One presentation type for independently equipped cape, aura and outfit slots. */
record WardrobeItem(WardrobeCape cape,WardrobeAura aura,WardrobeAccessory accessory,WardrobeEmote emote) {
    WardrobeItem(WardrobeCape cape,WardrobeAura aura,WardrobeAccessory accessory){this(cape,aura,accessory,null);}
    static WardrobeItem of(WardrobeEmote e){return e==null?null:new WardrobeItem(null,null,null,e);}
    static WardrobeItem of(WardrobeCape cape){return cape==null?null:new WardrobeItem(cape,null,null);}
    static WardrobeItem of(WardrobeAura aura){return aura==null?null:new WardrobeItem(null,aura,null);}
    static WardrobeItem of(WardrobeAccessory item){return item==null?null:new WardrobeItem(null,null,item);}
    static WardrobeItem fromId(String id){
        var cape=WardrobeCape.fromId(id);if(cape!=null)return of(cape);
        var aura=WardrobeAura.fromId(id);if(aura!=null)return of(aura);
        var emote=WardrobeEmote.fromId(id);return emote!=null?of(emote):of(WardrobeAccessory.fromId(id));
    }
    String label(){return cape!=null?cape.label:aura!=null?aura.label:emote!=null?emote.label:accessory.label;}
    String badge(){return cape!=null?cape.badge:aura!=null?aura.badge:emote!=null?"EMOTE":accessory.badge;}
    boolean ownedBy(UUID uuid){return cape!=null?cape.ownedBy(uuid):aura!=null?aura.ownedBy(uuid):emote!=null?emote.ownedBy(uuid):accessory.ownedBy(uuid);}
    boolean equipped(){return cape!=null?WardrobePreferences.current()==cape:aura!=null?WardrobePreferences.currentAura()==aura:emote==null&&WardrobePreferences.currentAccessories().contains(accessory);}
    void toggle()throws java.io.IOException{if(emote!=null)return;if(cape!=null)WardrobePreferences.toggle(cape);else if(aura!=null)WardrobePreferences.toggle(aura);else WardrobePreferences.toggle(accessory);}
    static List<WardrobeItem> owned(UUID uuid,WardrobeCategory category){
        var items=new ArrayList<WardrobeItem>();
        if(category.includesCapes())WardrobeCape.owned(uuid).forEach(c->items.add(of(c)));
        if(category.includesAuras())WardrobeAura.owned(uuid).forEach(a->items.add(of(a)));
        WardrobeAccessory.owned(uuid).stream().filter(a->category==WardrobeCategory.ALL
                || (a.slots.contains(com.axial.cosmetics.data.CosmeticSlot.PET)?category==WardrobeCategory.PETS:category==WardrobeCategory.OUTFITS)).forEach(a->items.add(of(a)));
        if(category==WardrobeCategory.ALL||category==WardrobeCategory.EMOTES)for(var e:WardrobeEmote.values())if(e.ownedBy(uuid))items.add(of(e));
        return List.copyOf(items);
    }
}
