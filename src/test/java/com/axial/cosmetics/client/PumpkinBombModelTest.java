package com.axial.cosmetics.client;

import com.google.gson.JsonParser;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PumpkinBombModelTest {
    @Test
    void inventoryModelResolvesToGeometryMinecraftCanLoad() throws Exception {
        String modelId;
        try (var reader = resource("/assets/axial_cosmetics/items/pumpkinbomb.json")) {
            var model = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("model");
            assertEquals("minecraft:model", model.get("type").getAsString());
            modelId = model.get("model").getAsString();
        }
        String[] id = modelId.split(":", 2);
        String geometryPath = "/assets/" + id[0] + "/models/" + id[1] + ".json";
        try (var reader = resource(geometryPath)) {
            assertNotNull(JsonUnbakedModel.deserialize(reader).geometry());
        }
        try (var reader = resource(geometryPath)) {
            var geometry = JsonParser.parseReader(reader).getAsJsonObject();
            assertFalse(geometry.getAsJsonArray("elements").isEmpty());
            for (var texture : geometry.getAsJsonObject("textures").entrySet()) {
                String[] textureId = texture.getValue().getAsString().split(":", 2);
                assertNotNull(getClass().getResource("/assets/" + textureId[0]
                        + "/textures/" + textureId[1] + ".png"));
            }
        }
        try (var reader = resource("/assets/minecraft/items/note_block.json")) {
            var entries = JsonParser.parseReader(reader).getAsJsonObject()
                    .getAsJsonObject("model").getAsJsonArray("entries");
            var pumpkin = java.util.stream.StreamSupport.stream(entries.spliterator(), false)
                    .map(element -> element.getAsJsonObject())
                    .filter(entry -> entry.get("threshold").getAsInt() == 288)
                    .findFirst().orElseThrow();
            assertEquals(modelId, pumpkin.getAsJsonObject("model").get("model").getAsString());
        }
    }

    private InputStreamReader resource(String path) {
        var stream = getClass().getResourceAsStream(path);
        assertNotNull(stream, path);
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }
}
