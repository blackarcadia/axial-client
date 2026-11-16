package com.axial.cosmetics.client;

import io.github.kosmx.emotes.main.EmoteHolder;
import java.util.*;

/** Catalog IDs are also the VPS grant IDs; animation UUIDs stay stable on the native wheel. */
enum WardrobeEmote {
    GANGNAM_STYLE("gangnam_style","Gangnam Style","00039df2-3f23-ec84-0007-5d66adfcfb6a"),
    RAT_DANCE("rat_dance","Rat Dance","1fac5d45-0000-0000-0000-000000000000"),
    DEATH("death","Death","7146ae20-0520-415e-beb4-9379f848f590"),
    FEAR("fear","Fear","000380e3-6fec-6184-0007-2855df429a6a"),
    KEEP_YOUR_HEAD_OFF("keep_your_head_off","Keep Your Head Off","00038d24-8f03-5b11-0007-3a20c4136d51"),
    SPOOKY_DANCE("spooky_dance","Spooky Dance","f9cd0cf2-0000-0000-0000-000000000000");
    final String id,file,label;final UUID animation;
    WardrobeEmote(String file,String label,String animation){this.file=file;this.id="emote_"+file;this.label=label;this.animation=UUID.fromString(animation);}
    static WardrobeEmote fromId(String id){for(var e:values())if(e.id.equals(id))return e;return null;}
    static WardrobeEmote fromAnimation(UUID id){for(var e:values())if(e.animation.equals(id))return e;return null;}
    boolean ownedBy(UUID account){return CosmeticAssignments.owns(account,id,false);}
    EmoteHolder holder(){return EmoteHolder.getEmoteFromUuid(animation);}
}
