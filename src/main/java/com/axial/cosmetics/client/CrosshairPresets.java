package com.axial.cosmetics.client;

/** Shared by the style tiles and previews; keeps all nine existing presets. */
final class CrosshairPresets {
    private CrosshairPresets() {}
    static void apply(CrosshairConfig c,CrosshairConfig.CrosshairStyle style) {
        c.style=style;
        c.length=switch(style){case DOT,SMALL_DOT->0;case CIRCLE->3;case SMALL_CROSS->2;default->4;};
        c.width=style==CrosshairConfig.CrosshairStyle.SMALL_DOT?1:style==CrosshairConfig.CrosshairStyle.CIRCLE?4:2;
        c.gap=switch(style){case CLASSIC,DOT,SMALL_DOT,PLUS,CIRCLE->0;default->2;};
        c.outlineEnabled=style!=CrosshairConfig.CrosshairStyle.DOT&&style!=CrosshairConfig.CrosshairStyle.SMALL_DOT;
    }
}
