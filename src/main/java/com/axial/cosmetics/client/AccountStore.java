package com.axial.cosmetics.client;

import com.google.gson.JsonParser;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import org.example.launcher.ClientPaths;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class AccountStore {
    private final Path directory;
    private final Path pointer;

    public AccountStore(Path root) {
        directory = root.resolve("accounts");
        pointer = root.resolve("active-account.path");
    }

    public static AccountStore shared() { return new AccountStore(ClientPaths.appRoot()); }

    public record Entry(String name, UUID uuid, String fileName) {}

    public List<Entry> list() throws IOException {
        if (!Files.isDirectory(directory)) return List.of();
        Map<UUID, Entry> accounts = new LinkedHashMap<>();
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                try {
                    var profile = JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonObject("minecraftProfile");
                    UUID uuid = parseUuid(profile.get("id").getAsString());
                    accounts.put(uuid, new Entry(profile.get("name").getAsString(), uuid, file.getFileName().toString()));
                } catch (RuntimeException ignored) {
                    // An incomplete or invalid account must not hide the remaining accounts.
                }
            }
        }
        return accounts.values().stream().sorted(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public Path file(String name) {
        Path result = directory.resolve(name).normalize();
        if (!result.getParent().equals(directory) || !name.endsWith(".json")) throw new IllegalArgumentException("Invalid account file");
        return result;
    }

    public String save(JavaAuthManager auth) throws IOException {
        var profile = auth.getMinecraftProfile().getUpToDate();
        UUID uuid = profile.getId();
        String name = list().stream().filter(e -> e.uuid().equals(uuid)).map(Entry::fileName)
                .findFirst().orElse(uuid + ".json");
        write(file(name), JavaAuthManager.toJson(auth).toString());
        return name;
    }

    public void select(String name) throws IOException { file(name); write(pointer, name); }

    public void logout(UUID uuid) throws IOException {
        for (Entry entry : list()) {
            if (entry.uuid().equals(uuid)) remove(entry);
        }
        Files.deleteIfExists(pointer);
    }

    public void remove(Entry entry) throws IOException {
        // Remove legacy aliases too, so a renamed account cannot reappear.
        try (var files = Files.list(directory)) {
            for (Path candidate : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                UUID uuid;
                try {
                    var profile = JsonParser.parseString(Files.readString(candidate)).getAsJsonObject().getAsJsonObject("minecraftProfile");
                    uuid = parseUuid(profile.get("id").getAsString());
                } catch (RuntimeException ignored) { continue; }
                if (!entry.uuid().equals(uuid)) continue;
                Files.deleteIfExists(candidate);
                if (Files.exists(pointer) && Files.readString(pointer).trim().equals(candidate.getFileName().toString())) Files.delete(pointer);
            }
        }
    }

    static void write(Path target, String value) throws IOException {
        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), ".account-", ".tmp");
        try {
            Files.writeString(temp, value);
            try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }

    public static UUID parseUuid(String value) {
        if (value.matches("[a-fA-F0-9]{32}")) value = value.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");
        return UUID.fromString(value);
    }
}
