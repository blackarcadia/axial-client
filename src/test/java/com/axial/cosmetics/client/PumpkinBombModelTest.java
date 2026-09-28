package com.axial.cosmetics.client;

import com.google.gson.JsonParser;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.item.ItemDisplayContext;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PumpkinBombModelTest {
    @Test
    void placedItemDisplayKeepsFullModelScale() throws Exception {
        var transform = resolvedTransform("axial_cosmetics:block/custom/pumpkinbomb", ItemDisplayContext.FIXED);
        assertEquals(1.0F, transform.scale().x());
        assertEquals(1.0F, transform.scale().y());
        assertEquals(1.0F, transform.scale().z());
    }

    @Test
    void inventoryAndHandScalesStayAsAuthored() throws Exception {
        var gui = resolvedTransform("axial_cosmetics:block/custom/pumpkinbomb", ItemDisplayContext.GUI);
        var hand = resolvedTransform("axial_cosmetics:block/custom/pumpkinbomb", ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
        assertEquals(0.8F, gui.scale().x());
        assertEquals(0.5F, hand.scale().x());
    }

    private Transformation resolvedTransform(String modelId, ItemDisplayContext context) throws Exception {
        String[] id = modelId.split(":", 2);
        try (var reader = resource("/assets/" + id[0] + "/models/" + id[1] + ".json")) {
            var model = JsonUnbakedModel.deserialize(reader);
            var transform = model.transformations() == null ? Transformation.IDENTITY
                    : model.transformations().getTransformation(context);
            return transform != Transformation.IDENTITY || model.parent() == null
                    ? transform : resolvedTransform(model.parent().toString(), context);
        }
    }

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
        for (String baseItem : new String[]{"note_block", "barrier"}) {
            try (var reader = resource("/assets/minecraft/items/" + baseItem + ".json")) {
                var entries = JsonParser.parseReader(reader).getAsJsonObject()
                        .getAsJsonObject("model").getAsJsonArray("entries");
                var pumpkin = java.util.stream.StreamSupport.stream(entries.spliterator(), false)
                        .map(element -> element.getAsJsonObject())
                        .filter(entry -> entry.get("threshold").getAsInt() == 288)
                        .findFirst().orElseThrow();
                assertEquals(modelId, pumpkin.getAsJsonObject("model").get("model").getAsString());
            }
        }
    }

    private InputStreamReader resource(String path) {
        var stream = getClass().getResourceAsStream(path);
        assertNotNull(stream, path);
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }
}
