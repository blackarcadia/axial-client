package org.example.launcher;

import java.nio.file.Path;

class FileLayout {
    final Path root;

    FileLayout(Path root) {
        this.root = root;
    }

    Path versionsDir() {
        return root.resolve("versions");
    }

    Path versionDir(String versionId) {
        return versionsDir().resolve(versionId);
    }

    Path versionJson(String versionId) {
        return versionDir(versionId).resolve(versionId + ".json");
    }

    Path clientJar(String versionId) {
        return versionDir(versionId).resolve(versionId + ".jar");
    }

    Path librariesDir() {
        return root.resolve("libraries");
    }

    Path assetsDir() {
        return root.resolve("assets");
    }

    Path assetIndex(String id) {
        return assetsDir().resolve("indexes").resolve(id + ".json");
    }

    Path assetObject(String hash) {
        String sub = hash.substring(0, 2);
        return assetsDir().resolve("objects").resolve(sub).resolve(hash);
    }

    Path nativesDir(String versionId) {
        return versionDir(versionId).resolve("natives");
    }

    Path modsDir() {
        return root.resolve("mods");
    }

    Path resourcePacksDir() {
        return root.resolve("resourcepacks");
    }

    Path optionsFile() {
        return root.resolve("options.txt");
    }

    Path resourcePackDir(String name) {
        return resourcePacksDir().resolve(name);
    }

    Path configDir() {
        return root.resolve("config");
    }
}
