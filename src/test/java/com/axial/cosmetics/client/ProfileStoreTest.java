package com.axial.cosmetics.client;

import java.nio.file.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ProfileStoreTest {
    @TempDir Path root;
    private void settings(String value) throws IOException {Files.writeString(root.resolve("axialutils.json"),"{\"hudX\":"+value+"}");}
    @Test void snapshotsRenameUpdateDeleteAndActiveSelectionSurviveRestart() throws Exception {
        settings("10");var store=new ProfileStore(root);var first=store.create("Mining");
        settings("20");var second=store.create("PVP");
        var fresh=new ProfileStore(root);assertEquals(second.id(),fresh.active());
        fresh.activate(first.id(),()->{});assertTrue(Files.readString(root.resolve("axialutils.json")).contains("10"));
        fresh.rename(first.id(),"Building");assertEquals("Building",fresh.list().getFirst().name());
        settings("30");fresh.update(first.id());settings("40");fresh.activate(first.id(),()->{});
        assertTrue(Files.readString(root.resolve("axialutils.json")).contains("30"));
        fresh.delete(first.id());assertNull(fresh.active());assertEquals(1,fresh.list().size());
        assertTrue(Files.exists(root.resolve("axialutils.json")));
    }
    @Test void validationNeverOverwritesProfilesOrTouchesUnrelatedFiles() throws Exception {
        settings("1");Files.writeString(root.resolve("options.txt"),"controls");
        var store=new ProfileStore(root);var entry=store.create("Mining");
        assertThrows(IllegalArgumentException.class,()->store.create(" mining "));
        assertThrows(IllegalArgumentException.class,()->store.rename(entry.id()," "));
        assertThrows(IllegalArgumentException.class,()->store.create("x".repeat(33)));
        settings("2");store.activate(entry.id(),()->{});assertEquals("controls",Files.readString(root.resolve("options.txt")));
        assertEquals(1,store.list().size());
    }
    @Test void runtimeFailureRollsBackFilesAndActivePointer() throws Exception {
        settings("1");var store=new ProfileStore(root);var first=store.create("First");
        settings("2");var second=store.create("Second");var calls=new AtomicInteger();
        assertThrows(IOException.class,()->store.activate(first.id(),()->{if(calls.incrementAndGet()==1)throw new IllegalStateException();}));
        assertEquals(2,calls.get());assertEquals(second.id(),store.active());
        assertTrue(Files.readString(root.resolve("axialutils.json")).contains("2"));
    }
    @Test void corruptOrUnsafeSnapshotCannotWriteOutsideAllowlist() throws Exception {
        settings("1");var store=new ProfileStore(root);var entry=store.create("First");
        var index=root.resolve("axial-profiles.json");var text=Files.readString(index);
        Files.writeString(index,text.replace("axialutils.json","../outside.json"));
        assertThrows(IOException.class,()->store.activate(entry.id(),()->fail("Must validate before loading")));
        assertTrue(Files.readString(root.resolve("axialutils.json")).contains("1"));
    }
    @Test void firstLoadKeepsAnUnsavedSetupAsAnotherProfile() throws Exception {
        settings("1");var store=new ProfileStore(root);var first=store.create("First");
        // Deleting the current profile keeps the running setup intact.
        settings("2");var second=store.create("Second");store.delete(second.id());settings("3");
        store.activate(first.id(),()->{});
        assertEquals(2,store.list().size());var backup=store.list().stream().filter(p->p.name().equals("Previous setup")).findFirst().orElseThrow();
        store.activate(backup.id(),()->{});assertTrue(Files.readString(root.resolve("axialutils.json")).contains("3"));
    }
}
