package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EnchantGlintConfigTest {
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
