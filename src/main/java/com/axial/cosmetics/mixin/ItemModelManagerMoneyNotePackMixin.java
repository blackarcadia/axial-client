package com.axial.cosmetics.mixin;

import net.minecraft.client.item.ItemModelManager;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemModelManager.class)
public abstract class ItemModelManagerMoneyNotePackMixin {
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
