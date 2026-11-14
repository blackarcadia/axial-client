package com.axial.cosmetics.client;

import com.google.gson.JsonParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.axial.axialutils.client.cosmetics.ArmorCosmeticRegistry;

/** Suppress the cape only while visible cosmetic geometry occupies the player's back. */
public final class CapeObstruction {
    private record Key(Identifier model,Set<String> bones) {}
    private static final Map<Key,Boolean> ITEM_CACHE=new HashMap<>();
    private static final Map<WardrobeAccessory,Boolean> WARDROBE_CACHE=new EnumMap<>(WardrobeAccessory.class);
    private CapeObstruction() {}
    public static void initialize(){
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener(){
            @Override public Identifier getFabricId(){return Identifier.of("axial_cosmetics","cape_obstruction");}
            @Override public void reload(ResourceManager manager){ITEM_CACHE.clear();}
        });
    }
    public static boolean hidesCape(PlayerEntityRenderState state){
        if(state instanceof WardrobePreviewState preview)return wardrobeBlocks(preview.accessories);
        var client=MinecraftClient.getInstance();
        for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
            var stack=switch(slot){case HEAD->state.equippedHeadStack;case CHEST->state.equippedChestStack;case LEGS->state.equippedLegsStack;default->state.equippedFeetStack;};
            var found=ArmorCosmeticRegistry.find(stack,slot);if(found.isEmpty())continue;
            var definition=found.get();
            Set<String> visible=new HashSet<>();
            if(!definition.renderMode().name().equals("FULL_MODEL"))for(var segment:definition.segments())visible.add(switch(segment.name()){
                case "HEAD"->"armorHead";case "CHEST"->"armorBody";
                case "LEFT_ARM"->"armorLeftArm";case "RIGHT_ARM"->"armorRightArm";
                case "LEFT_LEG"->"armorLeftLeg";case "RIGHT_LEG"->"armorRightLeg";
                case "LEFT_FOOT"->"armorLeftBoot";case "RIGHT_FOOT"->"armorRightBoot";
                default->segment.name();
            });
            var key=new Key(definition.modelResource(),Set.copyOf(visible));
            if(ITEM_CACHE.computeIfAbsent(key,k->{
                try(var in=client.getResourceManager().getResource(k.model()).orElseThrow().getInputStream();var reader=new InputStreamReader(in,StandardCharsets.UTF_8)){
                    return CapeObstructionGeometry.intersectsBack(JsonParser.parseReader(reader).getAsJsonObject(),k.bones());
                }catch(Exception ignored){return false;}
            }))return true;
        }
        if(client.world==null)return false;var entity=client.world.getEntityById(state.id);if(entity==null)return false;
        return wardrobeBlocks(WardrobePreferences.accessoriesFor(entity.getUuid()).stream().filter(item->!ItemSkinConflict.hidesCosmetics(state,item.slots)).toList());
    }
    static boolean wardrobeBlocks(List<WardrobeAccessory> items){
        for(var item:items){
            if(item.slots.stream().noneMatch(slot->slot==com.axial.cosmetics.data.CosmeticSlot.CHEST||slot==com.axial.cosmetics.data.CosmeticSlot.BACKPACK||slot==com.axial.cosmetics.data.CosmeticSlot.LEGS||slot==com.axial.cosmetics.data.CosmeticSlot.HEAD))continue;
            if(WARDROBE_CACHE.computeIfAbsent(item,key->{
                try(var in=CapeObstruction.class.getResourceAsStream("/assets/axial_cosmetics/geo/"+key.id+".geo.json")){
                    if(in==null)return false;
                    try(var reader=new InputStreamReader(in,StandardCharsets.UTF_8)){return CapeObstructionGeometry.intersectsBack(JsonParser.parseReader(reader).getAsJsonObject(),Set.of());}
                }catch(Exception ignored){return false;}
            }))return true;
        }
        return false;
    }
}
