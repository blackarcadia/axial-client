package com.axial.cosmetics.client;

import java.util.Locale;

/** Default colors shared by the configurable HUD headings and detail lines. */
public final class HudColorDefaults {
    public static final int TITLE = 0xFFF900DF;
    public static final int SUBTITLE = 0xFFFFC917;

    private HudColorDefaults() { }

    /**
     * Returns the reset color for a HUD color-picker entry, preserving the
     * supplied value for every unrelated picker.
     */
    public static int forPickerLabel(String label, int fallback) {
        return switch (label.toUpperCase(Locale.ROOT)) {
            case "INFO TITLE", "SATCHEL TITLE", "POTIONS TITLE", "TITLE", "ARMOR" -> TITLE;
            case "CHARGE / MIN", "CHARGE/MIN", "XP / MIN", "XP/MIN", "SATCHEL INFORMATION",
                 "SATCHEL COUNT", "SATCHEL EMPTY", "CPS", "CPS COUNT", "DURABILITY", "SERVER",
                 "FACING", "COORDINATES", "PING" -> SUBTITLE;
            default -> fallback;
        };
    }
}
