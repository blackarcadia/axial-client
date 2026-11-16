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
    SPOOKY_DANCE("spooky_dance","Spooky Dance","f9cd0cf2-0000-0000-0000-000000000000"),
    ODDBALL("oddball","Oddball","000388b0-8671-9ce6-0007-303ccd326310"),
    SAY_SO("say_so","Say So","0003eacb-7dce-e4b5-0007-e29e8da00f3d"),
    DYLAN_DANCE("dylan_dance","Dylan Dance","000387bd-7768-77ce-0007-3628b37345c8"),
    RASPUTIN("rasputin","Rasputin","0003a918-45bf-27b0-0007-6b1a77780bee"),
    SAD_CAT("sad_cat","Sad Cat","0003867f-738b-126e-0007-321725311da8"),
    THE_PHITTY("the_phitty","The Phitty","000384ce-e689-4b0f-0007-2c3f26c29ecb"),
    SLICK_BACK("slick_back","Slick Back","0003b9ae-d5d9-64fa-0007-8729b515ec4c"),
    DEFAULT_DANCE("default_dance","Default Dance","c0e099c5-0000-0000-0000-000000000000"),
    ODD_LOOP("odd_loop","Odd Loop","0003bc02-5c8b-cec3-0007-8dcc9b23a2e7"),
    TAKE_THE_L("take_the_l","Take The L","d37c7498-0000-0000-0000-000000000000"),
    CARAMEL("caramel","Caramel","ac90254c-c1b8-47ca-bd20-b42f6575cd6f"),
    TWERK("twerk","Twerk","0003863f-f4bb-dcf8-0007-2eb2a604b5c6"),
    BREAKDANCE("breakdance","Breakdance","6f792aad-0589-47d1-900d-60d52337d77b");
    final String id,file,label;final UUID animation;
    WardrobeEmote(String file,String label,String animation){this.file=file;this.id="emote_"+file;this.label=label;this.animation=UUID.fromString(animation);}
    static WardrobeEmote fromId(String id){for(var e:values())if(e.id.equals(id))return e;return null;}
    static WardrobeEmote fromAnimation(UUID id){for(var e:values())if(e.animation.equals(id))return e;return null;}
    boolean ownedBy(UUID account){return CosmeticAssignments.owns(account,id,false);}
    EmoteHolder holder(){return EmoteHolder.getEmoteFromUuid(animation);}
}
