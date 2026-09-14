package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountStoreTest {
    @TempDir Path root;

    @Test void listsMultipleAccountsAndIgnoresMalformedFiles() throws Exception {
        var store = new AccountStore(root);
        account("alpha.json", "Alpha", "00000000000000000000000000000001");
        account("beta.json", "Beta", "00000000-0000-0000-0000-000000000002");
        Files.writeString(root.resolve("accounts/broken.json"), "{");
        assertEquals(2, store.list().size());
        store.select("beta.json");
        assertEquals("beta.json", Files.readString(root.resolve("active-account.path")));
        assertEquals(2, store.list().size(), "Switching must retain the other account");
    }

    @Test void deduplicatesProfilesAndRemovesOnlyChosenAccount() throws Exception {
        var store = new AccountStore(root);
        account("old-name.json", "OldName", "00000000000000000000000000000001");
        account("new-name.json", "NewName", "00000000000000000000000000000001");
        account("other.json", "Other", "00000000000000000000000000000002");
        assertEquals(2, store.list().size());
        var other = store.list().stream().filter(e -> e.name().equals("Other")).findFirst().orElseThrow();
        store.select(other.fileName());
        store.remove(other);
        assertFalse(Files.exists(root.resolve("active-account.path")));
        assertEquals(1, store.list().size());
        store.remove(store.list().getFirst());
        assertTrue(store.list().isEmpty(), "Legacy aliases must not restore a removed account");
    }

    @Test void rejectsPathsOutsideAccountDirectory() {
        var store = new AccountStore(root);
        assertThrows(IllegalArgumentException.class, () -> store.file("../outside.json"));
        assertThrows(IllegalArgumentException.class, () -> store.file("temp.txt"));
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), AccountStore.parseUuid("00000000000000000000000000000001"));
    }

    private void account(String file, String name, String uuid) throws Exception {
        Files.createDirectories(root.resolve("accounts"));
        Files.writeString(root.resolve("accounts").resolve(file), "{\"minecraftProfile\":{\"name\":\"" + name + "\",\"id\":\"" + uuid + "\"}}");
    }
}
