package com.axial.cosmetics.client;

import java.util.*;
import net.minecraft.util.Identifier;

/** Independently equipped, server-granted model cosmetics. */
enum WardrobeAccessory {
    SWIM_BANDS("swim_bands","Swim Bands","ARM BANDS");
    final String id,label,badge;
    WardrobeAccessory(String id,String label,String badge){this.id=id;this.label=label;this.badge=badge;}
    boolean ownedBy(UUID uuid){return uuid!=null&&CosmeticAssignments.owns(uuid,id,false);}
    Identifier preview(){return Identifier.of("axial_cosmetics","textures/gui/swim_bands_preview.png");}
    static WardrobeAccessory fromId(String id){return Arrays.stream(values()).filter(a->a.id.equals(id)).findFirst().orElse(null);}
    static List<WardrobeAccessory> owned(UUID uuid){return Arrays.stream(values()).filter(a->a.ownedBy(uuid)).toList();}
}
