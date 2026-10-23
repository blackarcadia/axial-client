package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class WaypointStoreTest {
    @TempDir Path dir;
    @Test void legacyFilesKeepNamesColoursCoordinatesAndTheGlobalToggle() throws Exception {
        Path p=dir.resolve("waypoints.json");
        Files.writeString(p,"""
            {"enabled":false,"waypoints":[
            {"name":"Home","world":"server:test","dimension":"minecraft:overworld","x":-40,"y":64,"z":81,"color":-65408},
            {"name":"Mine","world":"server:test","dimension":"minecraft:overworld","x":-40,"y":64,"z":81,"color":-16711936}]}
            """);
        var store=new WaypointStore(p);var entries=store.entries();
        assertFalse(store.enabled());assertEquals(2,entries.size());
        assertEquals("Home",entries.getFirst().name());assertEquals(-40,entries.getFirst().x());
        assertEquals(-65408,entries.getFirst().color());assertFalse(entries.getFirst().hidden());
        assertNotEquals(entries.getFirst().id(),entries.getLast().id());
        assertEquals(entries,new WaypointStore(p).entries());
    }
    @Test void editingOneOfTwoCoLocatedMarkersDoesNotChangeOrDeleteTheOther() {
        var store=new WaypointStore(dir.resolve("waypoints.json"));
        var a=new WaypointConfig.Entry("Home","server:test","minecraft:overworld",10,64,20,0xFFFF0000);
        var b=new WaypointConfig.Entry("Portal","server:test","minecraft:overworld",10,64,20,0xFF00FF00);
        store.add(a);store.add(b);
        store.update(b.id(),e->new WaypointConfig.Entry(e.id(),"Nether Portal",e.world(),e.dimension(),80,64,20,e.color(),true));
        assertEquals(a,store.entries().getFirst());
        var saved=new WaypointStore(dir.resolve("waypoints.json"));
        assertEquals("Nether Portal",saved.entries().getLast().name());assertTrue(saved.entries().getLast().hidden());
        store.delete(a.id());assertEquals(b.id(),store.entries().getFirst().id());
    }
    @Test void malformedFilesArePreservedBeforeReplacement() throws Exception {
        Path p=dir.resolve("waypoints.json");Files.writeString(p,"{bad json");
        var store=new WaypointStore(p);assertTrue(store.entries().isEmpty());
        try(var paths=Files.list(dir)) {
            var backup=paths.filter(f->f.getFileName().toString().contains(".backup-")).findFirst().orElseThrow();
            assertEquals("{bad json",Files.readString(backup));
        }
        store.add(new WaypointConfig.Entry("New","server:test","minecraft:overworld",1,2,3,0xFFFFFFFF));
        assertEquals("New",new WaypointStore(p).entries().getFirst().name());
    }
}
