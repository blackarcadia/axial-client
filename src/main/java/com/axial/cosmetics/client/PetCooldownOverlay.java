package com.axial.cosmetics.client;

import com.axial.cosmetics.mixin.ItemCooldownEntryAccessor;
import com.axial.cosmetics.mixin.ItemCooldownManagerAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.*;

/** Visible items only: no inventory scans, background polling or server requests. */
public final class PetCooldownOverlay {
    private record CachedPet(PetCooldown.Pet pet,Identifier group){}
    private static final Map<NbtComponent,CachedPet> ITEMS=new IdentityHashMap<>();
    private static final PetCooldownClock CLOCK=new PetCooldownClock();
    private static final StyleSpriteSource.Font FONT=new StyleSpriteSource.Font(UiTypography.font(.75f,true));
    private PetCooldownOverlay(){}
    public static void reset(){ITEMS.clear();CLOCK.reset();}
    public static void render(DrawContext context,ItemStack stack,int x,int y){
        if(!CooldownsConfig.petsVisible()||!stack.isOf(Items.PLAYER_HEAD))return;
        var client=MinecraftClient.getInstance();if(client.player==null)return;
        var data=stack.get(DataComponentTypes.CUSTOM_DATA);if(data==null)return;
        CachedPet cached;
        if(ITEMS.containsKey(data))cached=ITEMS.get(data);
        else{
            var pet=PetCooldown.read(data.copyNbt());
            cached=pet==null?null:new CachedPet(pet,Identifier.of("prisonscore","pet_"+pet.instance().replace("-","")));
            if(ITEMS.size()>=512)ITEMS.clear();ITEMS.put(data,cached);
        }
        if(cached==null)return;
        var pet=cached.pet();long now=System.currentTimeMillis();
        var manager=(ItemCooldownManagerAccessor)client.player.getItemCooldownManager();
        Object entry=manager.axial$entries().get(cached.group());
        Long nativeRemaining=entry instanceof ItemCooldownEntryAccessor active
                ?Math.max(0L,(long)active.axial$endTick()-manager.axial$tick())*50L:null;
        long remaining=CLOCK.remaining(pet,now,nativeRemaining);
        String label=PetCooldown.label(remaining);if(label.isEmpty())return;
        Text text=Text.literal(label).styled(s->s.withFont(FONT));
        int width=client.textRenderer.getWidth(text);
        float scale=Math.min(1f,14f/Math.max(1,width));
        context.getMatrices().pushMatrix();
        try{
            context.getMatrices().translate(x+8f,y+8f);context.getMatrices().scale(scale,scale);
            int left=-width/2,top=-4;
            for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(dx!=0||dy!=0)context.drawText(client.textRenderer,text,left+dx,top+dy,0xFF000000,false);
            context.drawText(client.textRenderer,text,left,top,0xFFFF3030,false);
        }finally{context.getMatrices().popMatrix();}
    }
}
