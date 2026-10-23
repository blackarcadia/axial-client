package com.axial.cosmetics.client;

import java.util.Locale;
import java.util.function.BooleanSupplier;

/** The displayed state and actions share the existing mod's configuration. */
record ModMenuEntry(String title, String description, Category category, String icon,
                    BooleanSupplier enabled, Runnable toggle, Runnable configure) {
    enum Category {
        ALL("All mods", "grid"), HUD("HUD", "monitor"), MINING("Mining", "pickaxe"),
        VISUALS("Visuals", "eye"), WORLD("World", "globe");
        final String label, icon;
        Category(String label, String icon) { this.label = label; this.icon = icon; }
    }

    boolean isEnabled() { return enabled != null && enabled.getAsBoolean(); }
    boolean matches(Category filter, String query, boolean enabledOnly) {
        return (filter == Category.ALL || category == filter)
                && (!enabledOnly || isEnabled())
                && (title + " " + description).toLowerCase(Locale.ROOT).contains(query.strip().toLowerCase(Locale.ROOT));
    }
}
