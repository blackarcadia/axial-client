package com.axial.cosmetics.data;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CosmeticDefinition {
    private final String id;
    private final Set<CosmeticSlot> slots;
    private final Path texturePath;
    private final String modelId; // Optional baked model identifier (namespace:path)
    private final List<UUID> uuids;
    private final List<String> names;

    public CosmeticDefinition(String id, Set<CosmeticSlot> slots, Path texturePath, String modelId, List<UUID> uuids, List<String> names) {
        this.id = id;
        this.slots = EnumSet.copyOf(slots);
        this.texturePath = texturePath;
        this.modelId = modelId;
        this.uuids = uuids;
        this.names = names;
    }

    public String id() {
        return id;
    }

    public Set<CosmeticSlot> slots() {
        return slots;
    }

    public Path texturePath() {
        return texturePath;
    }

    public String modelId() {
        return modelId;
    }

    public boolean matches(UUID uuid, String name) {
        if (uuid != null && uuids.contains(uuid)) return true;
        if (name != null) {
            for (String n : names) {
                if (n.equalsIgnoreCase(name)) return true;
            }
        }
        return false;
    }
}
