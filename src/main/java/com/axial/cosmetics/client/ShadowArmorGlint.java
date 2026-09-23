package com.axial.cosmetics.client;

import com.axial.cosmetics.mixin.RenderLayerAccessor;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.ItemTags;
import software.bernie.geckolib.constant.dataticket.DataTicket;

import java.util.IdentityHashMap;
import java.util.Map;

public final class ShadowArmorGlint {
    public static final float STRENGTH = 1.8f;
    public static final DataTicket<Boolean> GEO_SHADOW = DataTicket.create("axial_shadow_armor", Boolean.class);
    private static final String SET_KEY = "prisonscore:special-set";
    // Separate layer identities keep deferred/batched draws isolated from ordinary glint.
    private static final Map<RenderLayer, RenderLayer> VARIANTS = new IdentityHashMap<>();
    private static final Map<RenderLayer, RenderLayer> ORIGINALS = new IdentityHashMap<>();

    private ShadowArmorGlint() {}

    public static boolean matches(ItemStack stack) {
        if (!(stack.isIn(ItemTags.HEAD_ARMOR) || stack.isIn(ItemTags.CHEST_ARMOR)
                || stack.isIn(ItemTags.LEG_ARMOR) || stack.isIn(ItemTags.FOOT_ARMOR))) return false;
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null && matchesData(data.copyNbt());
    }

    public static boolean matchesData(NbtCompound data) {
        return "shadow".equals(data.getString(SET_KEY, ""))
                || "shadow".equals(data.getCompoundOrEmpty("PublicBukkitValues").getString(SET_KEY, ""));
    }

    public static RenderLayer variant(RenderLayer original) {
        if (isShadow(original)) return original;
        return VARIANTS.computeIfAbsent(original, layer -> {
            RenderLayer variant = RenderLayerAccessor.axial$create("axial_shadow_" + VARIANTS.size(),
                    ((RenderLayerAccessor) layer).axial$getRenderSetup());
            ORIGINALS.put(variant, layer);
            return variant;
        });
    }

    public static boolean isShadow(RenderLayer layer) {
        return ORIGINALS.containsKey(layer);
    }

    public static RenderLayer original(RenderLayer layer) {
        return ORIGINALS.getOrDefault(layer, layer);
    }

    public static RenderLayer armorLayer() {
        return variant(RenderLayers.armorEntityGlint());
    }
}
