package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import net.minecraft.nbt.NbtCompound;
import org.joml.Vector4f;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EnchantGlintConfigTest {
    @Test
    void maddyOverridesEveryPlayerColorAndStrengthSetting() {
        var data = new NbtCompound();
        data.putString("prisonscore:lootbox_data", "maddy");
        var itemOverride = ShadowArmorGlint.typeData(data);
        var original = new Vector4f(0, 0, 1, 1);
        for (int color : new int[]{0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFF000000, 0xFFFFFFFF}) {
            for (float strength : new float[]{1.0f, 2.5f, EnchantGlintConfig.MAX_STRENGTH}) {
                for (boolean customColor : new boolean[]{false, true}) {
                    var result = EnchantGlintConfig.resolveModulator(original, itemOverride,
                            strength, customColor, color);
                    assertEquals(new Vector4f(1.5f, 0, 0, -1), result);
                }
            }
        }
    }

    @Test
    void ordinaryItemsStillUsePlayerSettingsAfterAMaddyDraw() {
        var original = new Vector4f(1, 1, 1, 1);
        EnchantGlintConfig.resolveModulator(original, ShadowArmorGlint.GlintType.MADDY,
                4.0f, true, 0xFF00FF00);
        assertEquals(new Vector4f(0, 4, 0, -1),
                EnchantGlintConfig.resolveModulator(original, null, 4.0f, true, 0xFF00FF00));
        assertSame(original, EnchantGlintConfig.resolveModulator(original, null, 1.0f, false, 0));
        assertEquals(new Vector4f(2, 2, 2, 1),
                EnchantGlintConfig.resolveModulator(original, null, 2.0f, false, 0));
        assertEquals(new Vector4f(1, 1, 1, 1), original);
    }

    @Test
    void strengthCanOnlyIncreaseFromVanillaAndStopsAtMaximum() {
        assertEquals(1.0f, EnchantGlintConfig.clampStrength(-3.0f));
        assertEquals(1.0f, EnchantGlintConfig.clampStrength(0.5f));
        assertEquals(1.0f, EnchantGlintConfig.clampStrength(1.0f));
        assertEquals(2.5f, EnchantGlintConfig.clampStrength(2.5f));
        assertEquals(4.0f, EnchantGlintConfig.clampStrength(4.0f));
        assertEquals(4.0f, EnchantGlintConfig.clampStrength(100.0f));
    }

    @Test
    void nonFiniteStrengthFallsBackToVanilla() {
        assertEquals(1.0f, EnchantGlintConfig.clampStrength(Float.NaN));
        assertEquals(1.0f, EnchantGlintConfig.clampStrength(Float.POSITIVE_INFINITY));
        assertEquals(1.0f, EnchantGlintConfig.clampStrength(Float.NEGATIVE_INFINITY));
    }
}
