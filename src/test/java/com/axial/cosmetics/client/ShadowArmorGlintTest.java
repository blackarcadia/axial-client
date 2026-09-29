package com.axial.cosmetics.client;

import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShadowArmorGlintTest {
    @Test
    void recognizesMaddyLootboxInDirectAndPaperData() {
        NbtCompound direct = new NbtCompound();
        direct.putString("prisonscore:lootbox_data", "maddy");
        assertEquals(ShadowArmorGlint.GlintType.MADDY, ShadowArmorGlint.typeData(direct));

        NbtCompound paper = new NbtCompound();
        paper.put("PublicBukkitValues", direct);
        assertEquals(ShadowArmorGlint.GlintType.MADDY, ShadowArmorGlint.typeData(paper));

        direct.putString("prisonscore:special-set", "shadow");
        assertEquals(ShadowArmorGlint.GlintType.MADDY, ShadowArmorGlint.typeData(direct));
    }

    @Test
    void rejectsOtherLootboxesCaseChangesAndWrongTypes() {
        NbtCompound data = new NbtCompound();
        for (String value : new String[]{"", "other", "Maddy", "maddy "}) {
            data.putString("prisonscore:lootbox_data", value);
            assertNull(ShadowArmorGlint.typeData(data));
        }
        data.putInt("prisonscore:lootbox_data", 1);
        assertNull(ShadowArmorGlint.typeData(data));
        NbtCompound paper = new NbtCompound();
        paper.put("PublicBukkitValues", data);
        assertNull(ShadowArmorGlint.typeData(paper));
    }

    @Test
    void maddyUsesRedAtOneAndAHalfStrengthWithoutChangingArmorColors() {
        var red = ShadowArmorGlint.GlintType.MADDY.modulator();
        assertEquals(1.5f, red.x());
        assertEquals(0.0f, red.y());
        assertEquals(0.0f, red.z());
        assertEquals(-1.0f, red.w());
        var shadow = ShadowArmorGlint.GlintType.SHADOW.modulator();
        assertEquals(1.8f, shadow.x());
        assertEquals(1.8f, shadow.y());
        assertEquals(1.8f, shadow.z());
        var prospector = ShadowArmorGlint.GlintType.PROSPECTOR.modulator();
        assertEquals(0.9f, prospector.x());
        assertEquals(1.8f, prospector.y());
        assertEquals(0.0f, prospector.z());
    }

    @Test
    void recognizesDirectClientDataAndPaperPersistentData() {
        NbtCompound direct = new NbtCompound();
        direct.putString("prisonscore:special-set", "shadow");
        assertTrue(ShadowArmorGlint.matchesData(direct));

        NbtCompound paper = new NbtCompound();
        paper.put("PublicBukkitValues", direct);
        assertTrue(ShadowArmorGlint.matchesData(paper));

        NbtCompound prospector = new NbtCompound();
        prospector.putString("prisonscore:special-set", "prospector");
        assertEquals(ShadowArmorGlint.GlintType.PROSPECTOR, ShadowArmorGlint.typeData(prospector));
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
