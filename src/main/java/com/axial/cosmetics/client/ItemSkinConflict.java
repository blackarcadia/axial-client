package com.axial.cosmetics.client;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.axial.axialutils.client.cosmetics.ArmorCosmeticRegistry;

/** Match the same worn-item registry as Prisonscore's Geo armour renderer. */
public final class ItemSkinConflict {
    private ItemSkinConflict(){}
    public static boolean hasSkin(ItemStack stack,EquipmentSlot slot){return ArmorCosmeticRegistry.find(stack,slot).isPresent();}
    public static boolean hidesCosmetics(PlayerEntityRenderState state,java.util.Set<com.axial.cosmetics.data.CosmeticSlot> slots){
        return slots.stream().anyMatch(slot->switch(slot){
            case HEAD -> hasSkin(state.equippedHeadStack,EquipmentSlot.HEAD);
            case CHEST,LEFT_ARM,RIGHT_ARM,BACKPACK -> hasSkin(state.equippedChestStack,EquipmentSlot.CHEST);
            case LEGS -> hasSkin(state.equippedLegsStack,EquipmentSlot.LEGS);
            case BOOTS -> hasSkin(state.equippedFeetStack,EquipmentSlot.FEET);
        });
    }
    public static boolean hidesCosmetics(PlayerEntityRenderState state){
        return hasSkin(state.equippedHeadStack,EquipmentSlot.HEAD)||hasSkin(state.equippedChestStack,EquipmentSlot.CHEST)
            ||hasSkin(state.equippedLegsStack,EquipmentSlot.LEGS)||hasSkin(state.equippedFeetStack,EquipmentSlot.FEET);
    }
}
