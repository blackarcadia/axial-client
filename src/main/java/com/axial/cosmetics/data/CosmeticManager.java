package com.axial.cosmetics.data;

import com.axial.cosmetics.AxialCosmetics;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CosmeticManager {
    private static final Gson GSON = new Gson();
    private final Map<UUID, List<CosmeticTexture>> byUuid = new HashMap<>();
    private final Map<String, List<CosmeticTexture>> byName = new HashMap<>();
    private final Set<String> enabled = new HashSet<>();
    private boolean initialized = false;
    private boolean hasEnabledFile = false;

    public boolean isInitialized() {
        return initialized;
    }

    public void reload(MinecraftClient client) {
        Path cfgDir = FabricLoader.getInstance().getConfigDir();
        Path cfg = cfgDir.resolve("axial_cosmetics.json");
        Path textureDir = cfgDir.resolve("axial_cosmetics/textures");
        Path enabledFile = cfgDir.resolve("axial_cosmetics_enabled.json");
        try {
            Files.createDirectories(textureDir);
            if (Files.notExists(cfg)) {
                writeSampleConfig(cfg, textureDir);
            }
            loadEnabled(enabledFile);
            loadFromFile(client, cfg, textureDir);
            saveEnabled(enabledFile);
            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFromFile(MinecraftClient client, Path cfg, Path textureDir) throws IOException {
        byUuid.clear();
        byName.clear();
        try (Reader reader = Files.newBufferedReader(cfg)) {
            JsonArray root = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement el : root) {
                try {
                    JsonObject obj = el.getAsJsonObject();
                    String id = obj.get("id").getAsString();
                    Path texturePath = textureDir.resolve(obj.get("texture").getAsString());
                    Set<CosmeticSlot> slots = EnumSet.noneOf(CosmeticSlot.class);
                    for (JsonElement s : obj.getAsJsonArray("slots")) {
                        slots.add(CosmeticSlot.valueOf(s.getAsString().toUpperCase(Locale.ROOT)));
                    }
                    String modelId = obj.has("model") ? obj.get("model").getAsString() : null;
                    List<UUID> uuids = new ArrayList<>();
                    for (JsonElement u : obj.getAsJsonArray("uuids")) {
                        uuids.add(UUID.fromString(u.getAsString()));
                    }
                    List<String> names = new ArrayList<>();
                    for (JsonElement n : obj.getAsJsonArray("names")) {
                        names.add(n.getAsString());
                    }

                    Identifier texId = registerTexture(client, id, texturePath);
                    CosmeticTexture texture = new CosmeticTexture(new CosmeticDefinition(id, slots, texturePath, modelId, uuids, names), texId);
                    if (!hasEnabledFile) enabled.add(id); // default-on first time
                    for (UUID uuid : uuids) {
                        byUuid.computeIfAbsent(uuid, k -> new ArrayList<>()).add(texture);
                    }
                    for (String name : names) {
                        byName.computeIfAbsent(name.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(texture);
                    }
                } catch (Exception e) {
                    System.err.println("[AxialCosmetics] Skipping invalid cosmetic entry: " + e.getMessage());
                }
            }
        }
    }

    private Identifier registerTexture(MinecraftClient client, String id, Path texturePath) throws IOException {
        if (!Files.exists(texturePath)) {
            throw new IOException("Texture missing for cosmetic " + id + " at " + texturePath);
        }
        try (var in = Files.newInputStream(texturePath)) {
            NativeImage image = NativeImage.read(in);
            NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "axial_cosmetics/" + id, image);
            Identifier dynId = Identifier.of(AxialCosmetics.MOD_ID, "cosmetics/" + id.toLowerCase(Locale.ROOT));
            client.getTextureManager().registerTexture(dynId, tex);
            return dynId;
        }
    }

    public List<CosmeticTexture> get(UUID uuid, String name) {
        List<CosmeticTexture> result = new ArrayList<>();
        if (uuid != null) result.addAll(byUuid.getOrDefault(uuid, List.of()));
        if (name != null) result.addAll(byName.getOrDefault(name.toLowerCase(Locale.ROOT), List.of()));
        result.removeIf(ct -> !enabled.contains(ct.definition().id()));
        return result;
    }

    public void warmTextures(UUID uuid) {
        // no-op for now; textures stay registered
    }

    public void setEnabled(String id, boolean value, MinecraftClient client) {
        if (value) enabled.add(id); else enabled.remove(id);
        Path enabledFile = FabricLoader.getInstance().getConfigDir().resolve("axial_cosmetics_enabled.json");
        try { saveEnabled(enabledFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public boolean isEnabled(String id) {
        return enabled.contains(id);
    }

    private void loadEnabled(Path file) {
        enabled.clear();
        hasEnabledFile = Files.exists(file);
        if (!hasEnabledFile) return;
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonArray arr = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement el : arr) enabled.add(el.getAsString());
        } catch (Exception ignored) {}
    }

    private void saveEnabled(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        for (String id : enabled) arr.add(id);
        Files.writeString(file, arr.toString());
    }

    private void writeSampleConfig(Path cfg, Path textureDir) throws IOException {
        String sample = """
            [
              {
                "id": "dev_headset",
                "texture": "dev_headset.png",
                "slots": ["head"],
                "uuids": [],
                "names": ["SubXil"]
              },
              {
                "id": "hasmatmask",
                "texture": "hasmathelmet.png",
                "model": "axial_cosmetics:item/hasmatmask",
                "slots": ["head"],
                "uuids": [],
                "names": ["SubXil"]
              }
            ]
            """;
        Files.writeString(cfg, sample.stripIndent());
        Path placeholder = textureDir.resolve("dev_headset.png");
        if (Files.notExists(placeholder)) {
            byte[] png = PlaceholderTextures.HEADSET_PNG;
            Files.write(placeholder, png);
        }
        Path hasmat = textureDir.resolve("hasmathelmet.png");
        if (Files.notExists(hasmat)) {
            Files.write(hasmat, PlaceholderTextures.HEADSET_PNG);
        }
    }
}
