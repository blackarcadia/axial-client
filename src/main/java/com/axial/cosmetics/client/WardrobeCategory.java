package com.axial.cosmetics.client;

enum WardrobeCategory {
    ALL("All cosmetics", "grid", "Search cosmetics..."),
    CAPES("Capes", "shirt", "Search capes..."),
    AURAS("Auras", "grid", "Search auras..."),
    HEADWEAR("Headwear", "account", "Search headwear..."),
    OUTFITS("Bodywear", "shirt", "Search bodywear..."),
    PETS("Pets", "account", "Search pets..."),
    EMOTES("Emotes", "replay", "Search emotes..."),
    BACKPACKS("Backpacks", "backpack", "Search backpacks...");

    final String label, icon, search;
    WardrobeCategory(String label, String icon, String search) {
        this.label=label;this.icon=icon;this.search=search;
    }
    boolean includesAuras() {return this==ALL||this==AURAS;}
    boolean includesCapes() {return this==ALL||this==CAPES;}
}
