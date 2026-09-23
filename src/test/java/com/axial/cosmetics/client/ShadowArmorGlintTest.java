package com.axial.cosmetics.client;

import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShadowArmorGlintTest {
    @Test
    void recognizesDirectClientDataAndPaperPersistentData() {
        NbtCompound direct = new NbtCompound();
        direct.putString("prisonscore:special-set", "shadow");
        assertTrue(ShadowArmorGlint.matchesData(direct));

        NbtCompound paper = new NbtCompound();
        paper.put("PublicBukkitValues", direct);
        assertTrue(ShadowArmorGlint.matchesData(paper));
    }

    @Test
    void rejectsOtherSetsMissingKeysAndWrongTypes() {
        NbtCompound data = new NbtCompound();
        assertFalse(ShadowArmorGlint.matchesData(data));
        data.putString("prisonscore:special-set", "greed");
        assertFalse(ShadowArmorGlint.matchesData(data));
        data.putString("prisonscore:special-set", "SHADOW");
        assertFalse(ShadowArmorGlint.matchesData(data));
        data.putInt("prisonscore:special-set", 1);
        data.putString("PublicBukkitValues", "shadow");
        assertFalse(ShadowArmorGlint.matchesData(data));
    }
}
