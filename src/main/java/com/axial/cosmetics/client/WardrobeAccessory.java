package com.axial.cosmetics.client;

import java.util.*;
import com.axial.cosmetics.data.CosmeticSlot;
import net.minecraft.util.Identifier;

/** Independently equipped, server-granted model cosmetics. */
enum WardrobeAccessory {
    SWIM_BANDS("swim_bands","Swim Bands","ARM BANDS",CosmeticSlot.LEFT_ARM,CosmeticSlot.RIGHT_ARM),
    WHITE_KICKS("white_kicks","White Kicks","SNEAKERS",500,CosmeticSlot.BOOTS),
    WHITE_SNEAKERS("white_sneakers","White Sneakers","SNEAKERS",CosmeticSlot.BOOTS),
    BLACK_SNEAKERS("black_sneakers","Black Sneakers","SNEAKERS",CosmeticSlot.BOOTS),
    RIBCAGE("ribcage","Ribcage","CHEST",CosmeticSlot.CHEST);
    final String id,label,badge;
    final Set<CosmeticSlot> slots;
    final int previewSize;
    WardrobeAccessory(String id,String label,String badge,CosmeticSlot... slots){this(id,label,badge,128,slots);}
    WardrobeAccessory(String id,String label,String badge,int previewSize,CosmeticSlot... slots){this.id=id;this.label=label;this.badge=badge;this.previewSize=previewSize;this.slots=Set.copyOf(Arrays.asList(slots));}
    boolean ownedBy(UUID uuid){return uuid!=null&&CosmeticAssignments.owns(uuid,id,false);}
    Identifier preview(){return Identifier.of("axial_cosmetics","textures/gui/"+id+"_preview.png");}
    static WardrobeAccessory fromId(String id){return Arrays.stream(values()).filter(a->a.id.equals(id)).findFirst().orElse(null);}
    static List<WardrobeAccessory> owned(UUID uuid){return Arrays.stream(values()).filter(a->a.ownedBy(uuid)).toList();}
}
