package com.axial.cosmetics.mixin;

import net.minecraft.client.item.ItemModelManager;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemModelManager.class)
public abstract class ItemModelManagerCustomItemMixin {
    @Unique
    private static final Identifier KROKS_MODEL = Identifier.of("axial_cosmetics", "kroks");
    @Unique
    private static final Identifier SOULCOLLECTOR_MODEL =
            Identifier.of("axial_cosmetics", "soulcollector");
    @Unique
    private static final Identifier LAMPSHADE_MODEL = Identifier.of("axial_cosmetics", "lampshade");
    @Unique
    private static final Identifier POWERBOX_MODEL = Identifier.of("axial_cosmetics", "powerbox");
    @Unique
    private static final Identifier SHOCK_THERAPY_MODEL = Identifier.of("axial_cosmetics", "shock_therapy");
    @Unique
    private static final String MONEY_NOTE_PACK_KEY = "PrisonsCore:money-note-pack";
    @Unique
    private static final String MONEY_NOTE_PACK_VALUE = "MONEY_NOTE_PACK";
    @Unique
    private static final Identifier MONEY_NOTE_PACK_MODEL = Identifier.of("minecraft", "money_note_pack");

    @Redirect(
            method = {
                    "update",
                    "hasHandAnimationOnSwap",
                    "getSwapAnimationScale"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;get(Lnet/minecraft/component/ComponentType;)Ljava/lang/Object;"
            )
    )
    @SuppressWarnings("unchecked")
    private Object axial_cosmetics$redirectItemModelLookup(ItemStack stack, ComponentType<?> type) {
        if (type == DataComponentTypes.ITEM_MODEL && axial_cosmetics$isMoneyNotePack(stack)) {
            return MONEY_NOTE_PACK_MODEL;
        }
        if (type == DataComponentTypes.ITEM_MODEL) {
            CustomModelDataComponent modelData = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
            if (modelData != null && Float.valueOf(247.0F).equals(modelData.getFloat(0))) {
                return KROKS_MODEL;
            }
            if (modelData != null && Float.valueOf(278.0F).equals(modelData.getFloat(0))) {
                return SOULCOLLECTOR_MODEL;
            }
            if (modelData != null && Float.valueOf(248.0F).equals(modelData.getFloat(0))) {
                return LAMPSHADE_MODEL;
            }
            if (modelData != null && Float.valueOf(249.0F).equals(modelData.getFloat(0))) {
                return POWERBOX_MODEL;
            }
            if (modelData != null && Float.valueOf(277.0F).equals(modelData.getFloat(0))) {
                return SHOCK_THERAPY_MODEL;
            }
        }

        return stack.get((ComponentType<Object>) type);
    }

    @Unique
    private static boolean axial_cosmetics$isMoneyNotePack(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return false;
        }

        NbtCompound nbt = customData.copyNbt();
        return MONEY_NOTE_PACK_VALUE.equals(nbt.getString(MONEY_NOTE_PACK_KEY));
    }
}
