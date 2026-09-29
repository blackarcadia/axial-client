package com.axial.cosmetics.client;

import com.axial.cosmetics.mixin.RenderLayerAccessor;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.ItemTags;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import software.bernie.geckolib.constant.dataticket.DataTicket;

import java.util.IdentityHashMap;
import java.util.Map;

public final class ShadowArmorGlint {
    public static final float STRENGTH = 1.8f;
    public static final DataTicket<GlintType> GEO_TYPE = DataTicket.create("axial_special_set_glint", GlintType.class);
    private static final String SET_KEY = "prisonscore:special-set";
    private static final String LOOTBOX_KEY = "prisonscore:lootbox_data";
    // Separate layer identities keep deferred/batched draws isolated from ordinary glint.
    private static final Map<RenderLayer, Map<GlintType, RenderLayer>> VARIANTS = new IdentityHashMap<>();
    private static final Map<RenderLayer, RenderLayer> ORIGINALS = new IdentityHashMap<>();
    private static final Map<RenderLayer, GlintType> TYPES = new IdentityHashMap<>();

    private ShadowArmorGlint() {}

    public enum GlintType {
        SHADOW,
        PROSPECTOR,
        MADDY;

        public Vector4fc modulator() {
            // Negative alpha selects recolouring in glint.fsh, not transparency.
            return switch (this) {
                case SHADOW -> new Vector4f(STRENGTH, STRENGTH, STRENGTH, -1.0f);
                case PROSPECTOR -> new Vector4f(0.5f * STRENGTH, STRENGTH, 0.0f, -1.0f);
                case MADDY -> new Vector4f(1.5f, 0.0f, 0.0f, -1.0f);
            };
        }
    }

    public static boolean matches(ItemStack stack) {
        return type(stack) != null;
    }

    public static GlintType type(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        GlintType type = data == null ? null : typeData(data.copyNbt());
        // Lootbox glint applies to any base item, including unenchanted items.
        if (type == null || type == GlintType.MADDY) return type;
        if (!(stack.isIn(ItemTags.HEAD_ARMOR) || stack.isIn(ItemTags.CHEST_ARMOR)
                || stack.isIn(ItemTags.LEG_ARMOR) || stack.isIn(ItemTags.FOOT_ARMOR))) return null;
        return type;
    }

    public static boolean matchesData(NbtCompound data) {
        return typeData(data) != null;
    }

    public static GlintType typeData(NbtCompound data) {
        String lootbox = data.getString(LOOTBOX_KEY, "");
        if (lootbox.isEmpty()) {
            lootbox = data.getCompoundOrEmpty("PublicBukkitValues").getString(LOOTBOX_KEY, "");
        }
        if ("maddy".equals(lootbox)) return GlintType.MADDY;
        String set = data.getString(SET_KEY, "");
        if (set.isEmpty()) {
            set = data.getCompoundOrEmpty("PublicBukkitValues").getString(SET_KEY, "");
        }
        return switch (set) {
            case "shadow" -> GlintType.SHADOW;
            case "prospector" -> GlintType.PROSPECTOR;
            default -> null;
        };
    }

    public static RenderLayer variant(RenderLayer original) {
        return variant(original, GlintType.SHADOW);
    }

    public static RenderLayer variant(RenderLayer original, GlintType type) {
        if (isSpecial(original)) return original;
        return VARIANTS.computeIfAbsent(original, ignored -> new java.util.EnumMap<>(GlintType.class))
                .computeIfAbsent(type, glintType -> {
                    RenderLayer variant = RenderLayerAccessor.axial$create("axial_" + glintType.name().toLowerCase()
                                    + "_" + TYPES.size(),
                            ((RenderLayerAccessor) original).axial$getRenderSetup());
                    ORIGINALS.put(variant, original);
                    TYPES.put(variant, glintType);
                    return variant;
                });
    }

    public static boolean isShadow(RenderLayer layer) {
        return type(layer) == GlintType.SHADOW;
    }

    public static boolean isSpecial(RenderLayer layer) {
        return TYPES.containsKey(layer);
    }

    public static GlintType type(RenderLayer layer) {
        return TYPES.get(layer);
    }

    public static RenderLayer original(RenderLayer layer) {
        return ORIGINALS.getOrDefault(layer, layer);
    }

    public static RenderLayer armorLayer() {
        return variant(RenderLayers.armorEntityGlint());
    }

    public static RenderLayer armorLayer(GlintType type) {
        return variant(RenderLayers.armorEntityGlint(), type);
    }
}
