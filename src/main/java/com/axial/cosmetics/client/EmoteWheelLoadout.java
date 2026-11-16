package com.axial.cosmetics.client;

import java.util.UUID;
import io.github.kosmx.emotes.PlatformTools;
import io.github.kosmx.emotes.server.config.Serializer;

/** Edits the same native pages used by Emotecraft, with rollback if persistence fails. */
final class EmoteWheelLoadout {
    static com.axial.cosmetics.client.ModMenuLayout.Rect slotBounds(com.axial.cosmetics.client.ModMenuLayout.Rect panel,int slot) {
        int[] columns={1,2,2,2,1,0,0,0},rows={2,2,1,0,0,0,1,2};
        int cellW=(panel.width()-24)/3,cellH=(panel.height()-142)/3;
        return new com.axial.cosmetics.client.ModMenuLayout.Rect(panel.x()+12+columns[slot]*cellW+3,
                panel.y()+80+rows[slot]*cellH+(cellH-20)/2,cellW-6,20);
    }
    static UUID[][] copy(UUID[][] pages){var result=pages.clone();for(int i=0;i<result.length;i++)result[i]=pages[i].clone();return result;}
    static void put(UUID[][] pages,int page,int slot,UUID emote){
        if(page<0||page>=pages.length||slot<0||slot>=pages[page].length)throw new IllegalArgumentException("Invalid wheel position");
        pages[page][slot]=emote;
    }
    static void assign(WardrobeEmote emote,int page,int slot) {
        if(!emote.ownedBy(net.minecraft.client.MinecraftClient.getInstance().getSession().getUuidOrNull()))throw new IllegalStateException("This emote is not unlocked");
        if(emote.holder()==null)throw new IllegalStateException("Emote is still downloading. Try again shortly.");
        update(page,slot,emote.animation);
    }
    static void clear(int page,int slot){update(page,slot,null);}
    private static void update(int page,int slot,UUID id) {
        var config=PlatformTools.getConfig();var old=config.fastMenuEmotes;var next=copy(old);put(next,page,slot,id);config.fastMenuEmotes=next;
        if(!Serializer.INSTANCE.saveConfig()){config.fastMenuEmotes=old;throw new IllegalStateException("Could not save your emote wheel");}
    }
    static int pages(){return PlatformTools.getConfig().fastMenuEmotes.length;}
    static UUID at(int page,int slot){return PlatformTools.getConfig().fastMenuEmotes[page][slot];}
}
