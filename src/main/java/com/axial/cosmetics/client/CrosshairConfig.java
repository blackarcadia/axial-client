package com.axial.cosmetics.client;

public final class CrosshairConfig {
    public boolean enabled = true;
    public CrosshairStyle style = CrosshairStyle.LUNAR;
    public int color = 0xFFFFFFFF;
    public float size = 1.0f;
    public float length = 4.0f;
    public float width = 2.0f;
    public float gap = 2.0f;
    public boolean outlineEnabled = true;
    public boolean dynamicEnabled = false;
    public int outlineColor = 0xFF000000;

    public enum CrosshairStyle {
        CLASSIC,
        LUNAR,
        DOT,
        SMALL_DOT,
        PLUS,
        T,
        X,
        CIRCLE,
        SMALL_CROSS
    }
}
