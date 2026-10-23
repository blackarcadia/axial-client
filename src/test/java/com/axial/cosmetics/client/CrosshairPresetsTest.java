package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrosshairPresetsTest {
    @Test void allNineStyleTilesRetainPresetValuesWithoutChangingUserColorsOrSize() {
        for(var style:CrosshairConfig.CrosshairStyle.values()) {
            var c=new CrosshairConfig();c.color=0xFF123456;c.outlineColor=0xFF654321;c.size=1.7f;c.dynamicEnabled=true;
            CrosshairPresets.apply(c,style);
            assertEquals(style,c.style);assertEquals(0xFF123456,c.color);assertEquals(0xFF654321,c.outlineColor);
            assertEquals(1.7f,c.size);assertTrue(c.dynamicEnabled);
            assertEquals(style!=CrosshairConfig.CrosshairStyle.DOT&&style!=CrosshairConfig.CrosshairStyle.SMALL_DOT,c.outlineEnabled);
        }
        var dot=new CrosshairConfig();CrosshairPresets.apply(dot,CrosshairConfig.CrosshairStyle.SMALL_DOT);
        assertEquals(1,dot.width);assertEquals(0,dot.length);assertEquals(0,dot.gap);
    }
}
