package com.axial.cosmetics.client;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CapeObstructionTest {
    private boolean resource(String path,Set<String> parts) throws Exception {
        try(var in=getClass().getResourceAsStream("/assets/axial_cosmetics/"+path)){
            assertNotNull(in);try(var reader=new InputStreamReader(in,StandardCharsets.UTF_8)){return CapeObstructionGeometry.intersectsBack(JsonParser.parseReader(reader).getAsJsonObject(),parts);}
        }
    }
    private JsonObject model(String bones){return JsonParser.parseString("{\"minecraft:geometry\":[{\"bones\":["+bones+"]}]}").getAsJsonObject();}
    @Test void bundledBackPiecesBlockButFrontOnlyJewelleryAndShoesDoNot() throws Exception {
        assertTrue(resource("geo/ribcage.geo.json",Set.of()));
        assertTrue(resource("geckolib/models/broodmother.geo.json",Set.of("armorBody")));
        assertTrue(resource("geckolib/models/chainbelt.geo.json",Set.of()));
        assertTrue(resource("geckolib/models/shocktherapy.geo.json",Set.of()));
        assertFalse(resource("geckolib/models/diamondnecklace.geo.json",Set.of("armorBody")));
        for(String id:List.of("white_kicks","white_sneakers","black_sneakers"))assertFalse(resource("geo/"+id+".geo.json",Set.of()));
        assertFalse(resource("geckolib/models/knightsboots.geo.json",Set.of("armorLeftBoot","armorRightBoot")));
    }
    @Test void cubeAndParentRotationsAndInflationAffectCollision() {
        String cube="{\"origin\":[-1,15,-5],\"size\":[2,4,1],\"pivot\":[0,18,0] ROTATION}";
        var front=model("{\"name\":\"armorBody\",\"pivot\":[0,18,0],\"cubes\":["+cube.replace(" ROTATION","")+"]}");
        assertFalse(CapeObstructionGeometry.intersectsBack(front,Set.of()));
        var rotated=model("{\"name\":\"armorBody\",\"pivot\":[0,18,0],\"cubes\":["+cube.replace(" ROTATION",",\"rotation\":[0,180,0]")+"]}");
        assertTrue(CapeObstructionGeometry.intersectsBack(rotated,Set.of()));
        var parent=model("{\"name\":\"bipedBody\",\"pivot\":[0,18,0],\"rotation\":[0,180,0]},{\"name\":\"armorBody\",\"parent\":\"bipedBody\",\"pivot\":[0,18,0],\"cubes\":["+cube.replace(" ROTATION","")+"]}");
        assertTrue(CapeObstructionGeometry.intersectsBack(parent,Set.of("armorBody")));
        var flush=model("{\"name\":\"armorBody\",\"cubes\":[{\"origin\":[-1,15,1.8],\"size\":[2,4,.2]}]}");
        assertFalse(CapeObstructionGeometry.intersectsBack(flush,Set.of()));
        flush.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones").get(0).getAsJsonObject().getAsJsonArray("cubes").get(0).getAsJsonObject().addProperty("inflate",.3);
        assertTrue(CapeObstructionGeometry.intersectsBack(flush,Set.of()));
    }
    @Test void onlyVisibleSegmentsCount() {
        var m=model("{\"name\":\"armorBody\"},{\"name\":\"armorHead\",\"cubes\":[{\"origin\":[-1,20,3],\"size\":[2,3,1]}]}");
        assertTrue(CapeObstructionGeometry.intersectsBack(m,Set.of()));
        assertFalse(CapeObstructionGeometry.intersectsBack(m,Set.of("armorBody")));
        assertTrue(CapeObstructionGeometry.intersectsBack(m,Set.of("armorHead")));
    }
    @Test void wardrobeLoadoutKeepsCapeSelectionAndReturnsVisibilityWhenBackItemIsRemoved() {
        assertFalse(CapeObstruction.wardrobeBlocks(List.of()));
        assertFalse(CapeObstruction.wardrobeBlocks(List.of(WardrobeAccessory.SWIM_BANDS,WardrobeAccessory.BLACK_SNEAKERS)));
        assertTrue(CapeObstruction.wardrobeBlocks(List.of(WardrobeAccessory.RIBCAGE,WardrobeAccessory.BLACK_SNEAKERS)));
        assertFalse(CapeObstruction.wardrobeBlocks(List.of(WardrobeAccessory.BLACK_SNEAKERS)));
    }
}
