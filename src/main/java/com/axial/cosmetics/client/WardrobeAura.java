package com.axial.cosmetics.client;

import java.util.List;
import java.util.UUID;
import net.minecraft.util.Identifier;

/** Server-granted aura catalogue. There are no implicit owner/admin grants. */
enum WardrobeAura {
    HEXWIND("halloween_hexwind","Hexwind","HALLOWEEN AURA");
    final String id,label,badge;
    static final Identifier PREVIEW=Identifier.of("axial_cosmetics","textures/gui/hexwind_preview.png");
    WardrobeAura(String id,String label,String badge){this.id=id;this.label=label;this.badge=badge;}
    boolean ownedBy(UUID uuid){return uuid!=null&&CosmeticAssignments.owns(uuid,id,false);}
    static List<WardrobeAura> owned(UUID uuid){return java.util.Arrays.stream(values()).filter(a->a.ownedBy(uuid)).toList();}
    static WardrobeAura defaultFor(UUID uuid){return owned(uuid).stream().findFirst().orElse(null);}
    static WardrobeAura fromId(String id){return java.util.Arrays.stream(values()).filter(a->a.id.equals(id)).findFirst().orElse(null);}
}
