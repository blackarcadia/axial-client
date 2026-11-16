package com.axial.cosmetics.client;

import java.util.UUID;

/** Shared names for wardrobe cards, native wheel tooltips and the emote/NPC pickers. */
public final class EmoteCatalogNames {
    private EmoteCatalogNames() {}
    public static String label(UUID animation){var entry=WardrobeEmote.fromAnimation(animation);return entry==null?null:entry.label;}
}
