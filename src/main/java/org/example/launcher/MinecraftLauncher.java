package org.example.launcher;

import com.google.gson.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MinecraftLauncher {
    public interface Logger {
        void info(String msg);
    }

    private final Logger logger;
    private static final String VERSION_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest.json";
    private static final String TOGGLE_MOD_URL = "https://cdn.modrinth.com/data/gejCNKwT/versions/731Py1cq/togglesneakhotkey-1.0.2.jar";
    private static final String TOGGLE_MOD_FILE = "togglesneakhotkey-1.0.2.jar";
    private static final String XAERO_MINIMAP_URL = "https://cdn.modrinth.com/data/1bokaNcj/versions/avSqR3vF/xaerominimap-fabric-1.21.11-25.3.10.jar";
    private static final String XAERO_MINIMAP_FILE = "xaerominimap-fabric-1.21.11-25.3.10.jar";
    private static final String FABRIC_API_URL = "https://edge.forgecdn.net/files/7422/501/fabric-api-0.141.1+1.21.11.jar";
    private static final String FABRIC_API_FILE = "fabric-api-0.141.1+1.21.11.jar";
    private static final String MOD_MENU_URL = "https://cdn.modrinth.com/data/mOgUt4GM/versions/fP9olSIC/modmenu-17.0.0-alpha.1.jar";
    private static final String MOD_MENU_FILE = "modmenu-17.0.0-alpha.1.jar";
    private static final String SODIUM_FILE = "sodium-fabric-0.8.13+mc1.21.11.jar";
    private static final String GECKOLIB_FILE = "geckolib-fabric-1.21.11-5.4.5.jar";
    private static final String AXIAL_COSMETICS_FILE = "axial-cosmetics.jar";
    private static final String AXIAL_UTILS_FILE = "axialutils-1.0-SNAPSHOT.jar";
    private static final String AXIAL_PACK_NAME = "axial_pack";
    private static final String SIMPLE_MENU_URL = null;
    private static final String SIMPLE_MENU_FILE = "simplemenu-1.21.11-2.1.jar";
    private static final String COLLECTIVE_URL = null;
    private static final String COLLECTIVE_FILE = "collective-1.21.11-8.13.jar";

    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new GsonBuilder().create();

    public MinecraftLauncher() {
        this(msg -> System.out.println(msg));
    }

    public MinecraftLauncher(Logger logger) {
        this.logger = logger;
    }

    public void ensureInstalled(LaunchRequest request) throws IOException {
        FileLayout layout = new FileLayout(request.getGameDir());
        Files.createDirectories(layout.versionsDir());
        Files.createDirectories(layout.librariesDir());
        Files.createDirectories(layout.assetsDir());

        VersionManifest manifest = loadVersionManifest();
        JsonObject versionJson;
        VersionRef ref = manifest.find(request.getVersionId()).orElse(null);
        if (ref != null) {
            versionJson = fetchVersionJson(layout, ref);
        } else {
            versionJson = fetchFabricProfile(layout, request.getVersionId(), "1.21.11", "0.18.4");
        }

        if (versionJson.has("inheritsFrom")) {
            String base = versionJson.get("inheritsFrom").getAsString();
            ensureVanilla(base, layout, manifest);
        }

        downloadClient(layout, request.getVersionId(), versionJson);
        downloadLibraries(layout, versionJson);
        downloadAssets(layout, versionJson);
        downloadToggleSprintMod(layout);
        downloadXaeroMinimap(layout);
        downloadFabricApi(layout);
        downloadModMenu(layout);
        installSodium(layout);
        removeStaticBgMod(layout);
        removeSimpleMenu(layout);
        removeCollective(layout);
        installAxialPack(layout);
        installGeckoLib(layout);
        installAxialUtils(layout);
        installAxialCosmetics(layout);
        logger.info("Installation check complete.");
    }

    public void launch(LaunchRequest request) throws IOException, InterruptedException {
        launch(request, System.out);
    }

    public void launch(LaunchRequest request, java.io.PrintStream log) throws IOException, InterruptedException {
        Process process = start(request, log);
        int code = process.waitFor();
        if (code != 0) {
            throw new IllegalStateException("Minecraft exited with code " + code);
        }
    }

    public Process start(LaunchRequest request, java.io.PrintStream log) throws IOException {
        FileLayout layout = new FileLayout(request.getGameDir());
        JsonObject versionJson = readJson(layout.versionJson(request.getVersionId()));
        if (versionJson.has("inheritsFrom")) {
            JsonObject base = readJson(layout.versionJson(versionJson.get("inheritsFrom").getAsString()));
            versionJson = mergeInherited(base, versionJson);
        }

        logger.info("Libraries in merged version: " + versionJson.getAsJsonArray("libraries").size());

        Map<String, Boolean> features = Map.of("is_quick_play_enabled", false);

        List<String> jvmArgs = buildJvmArgs(layout, request, versionJson, features);
        List<String> gameArgs = buildGameArgs(layout, request, versionJson, features);

        String javaCmd = System.getProperty("java.home") + "/bin/java";

        List<String> command = new ArrayList<>();
        command.add(javaCmd);
        command.addAll(jvmArgs);
        command.add(versionJson.get("mainClass").getAsString());
        command.addAll(gameArgs);

        logger.info("Classpath entries:");
        String[] cpParts = buildClasspath(layout, versionJson, request).split(System.getProperty("path.separator"));
        for (String c : cpParts) logger.info("  " + c);
        logger.info("Command line:");
        for (String c : command) {
            logger.info("  " + c);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(layout.versionDir(request.getVersionId()).toFile());
        pb.redirectErrorStream(true);

        log.println("Launching Minecraft " + request.getVersionId());
        Process p;
        try {
            p = pb.start();
        } catch (IOException e) {
            throw e;
        }
        Thread pipe = new Thread(() -> {
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    log.println(line);
                }
            } catch (IOException ignored) {
            }
        }, "mc-log-pipe");
        pipe.setDaemon(true);
        pipe.start();
        return p;
    }

    private VersionManifest loadVersionManifest() throws IOException {
        Request req = new Request.Builder().url(VERSION_MANIFEST_URL).build();
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) throw new IOException("Version manifest request failed: " + resp.code());
            JsonObject obj = gson.fromJson(resp.body().charStream(), JsonObject.class);
            return new VersionManifest(obj);
        }
    }

    private JsonObject fetchVersionJson(FileLayout layout, VersionRef ref) throws IOException {
        Path target = layout.versionJson(ref.id());
        if (Files.exists(target)) {
            return readJson(target);
        }

        Files.createDirectories(target.getParent());
        downloadTo(ref.url(), target);
        return readJson(target);
    }

    private JsonObject fetchFabricProfile(FileLayout layout, String versionId, String mcVersion, String loaderVersion) throws IOException {
        Path target = layout.versionJson(versionId);
        if (Files.exists(target)) return readJson(target);
        Files.createDirectories(target.getParent());
        String url = "https://meta.fabricmc.net/v2/versions/loader/" + mcVersion + "/" + loaderVersion + "/profile/json";
        downloadTo(url, target);
        return readJson(target);
    }

    private void ensureVanilla(String baseVersion, FileLayout layout, VersionManifest manifest) throws IOException {
        VersionRef base = manifest.find(baseVersion)
                .orElseThrow(() -> new IllegalArgumentException("Base version not found: " + baseVersion));
        JsonObject baseJson = fetchVersionJson(layout, base);
        downloadClient(layout, baseVersion, baseJson);
        downloadLibraries(layout, baseJson);
        downloadAssets(layout, baseJson);
    }

    private void downloadClient(FileLayout layout, String versionId, JsonObject versionJson) throws IOException {
        Path target = layout.clientJar(versionId);
        if (Files.exists(target)) return;

        JsonObject downloads = versionJson.getAsJsonObject("downloads");
        if (downloads == null) {
            if (versionJson.has("inheritsFrom")) {
                Path inherited = layout.clientJar(versionJson.get("inheritsFrom").getAsString());
                if (!Files.exists(inherited)) {
                    throw new IOException("Inherited client jar missing: " + inherited);
                }
                Files.createDirectories(target.getParent());
                Files.copy(inherited, target);
                return;
            }
            throw new IOException("No downloads section for version " + versionId);
        }
        JsonObject client = downloads.getAsJsonObject("client");
        if (client == null && versionJson.has("inheritsFrom")) {
            // use inherited client jar
            Path inherited = layout.clientJar(versionJson.get("inheritsFrom").getAsString());
            if (!Files.exists(inherited)) {
                throw new IOException("Inherited client jar missing: " + inherited);
            }
            Files.createDirectories(target.getParent());
            Files.copy(inherited, target);
            return;
        }
        downloadTo(client.get("url").getAsString(), target);
    }

    private void downloadLibraries(FileLayout layout, JsonObject versionJson) throws IOException {
        JsonArray libs = versionJson.getAsJsonArray("libraries");
        for (JsonElement el : libs) {
            JsonObject lib = el.getAsJsonObject();
            if (!RuleEvaluator.isAllowed(lib, Collections.emptyMap())) continue;

            JsonObject downloads = null;
            JsonElement downloadsEl = lib.get("downloads");
            if (downloadsEl != null && downloadsEl.isJsonObject()) {
                downloads = downloadsEl.getAsJsonObject();
            }
            if (downloads == null) {
                // Fabric profile omits downloads; synthesize Maven path from "name" and optional "url"
                downloads = new JsonObject();
                String coords = lib.get("name").getAsString();
                String[] parts = coords.split(":");
                if (parts.length < 3) {
                    logger.info("Skipping library with invalid coords: " + coords);
                    continue;
                }
                String path = parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + ".jar";
                String base = lib.has("url") ? lib.get("url").getAsString() : "https://libraries.minecraft.net/";
                JsonObject artifact = new JsonObject();
                artifact.addProperty("path", path);
                artifact.addProperty("url", base + path);
                downloads.add("artifact", artifact);
                lib.add("downloads", downloads);
            }
            JsonObject artifact = downloads.getAsJsonObject("artifact");
            if (artifact != null) {
                downloadArtifact(layout.librariesDir(), artifact);
            }

            JsonObject classifiers = downloads.getAsJsonObject("classifiers");
            if (classifiers != null) {
                JsonObject classifierArtifact = selectNative(classifiers);
                if (classifierArtifact != null) {
                    Path archive = downloadArtifact(layout.librariesDir(), classifierArtifact);
                    extractNatives(archive, layout.nativesDir(versionJson.get("id").getAsString()), lib);
                }
            }
        }
    }

    private void downloadAssets(FileLayout layout, JsonObject versionJson) throws IOException {
        JsonObject assetIndex = versionJson.getAsJsonObject("assetIndex");
        if (assetIndex == null && versionJson.has("inheritsFrom")) {
            Path basePath = layout.versionJson(versionJson.get("inheritsFrom").getAsString());
            JsonObject baseJson = readJson(basePath);
            assetIndex = baseJson.getAsJsonObject("assetIndex");
        }
        if (assetIndex == null) {
            throw new IOException("No assetIndex found for version " + versionJson.get("id").getAsString());
        }
        String indexId = assetIndex.get("id").getAsString();
        Path indexPath = layout.assetIndex(indexId);
        if (!Files.exists(indexPath)) {
            Files.createDirectories(indexPath.getParent());
            downloadTo(assetIndex.get("url").getAsString(), indexPath);
        }

        JsonObject indexJson = readJson(indexPath);
        JsonObject objects = indexJson.getAsJsonObject("objects");
        for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
            JsonObject obj = entry.getValue().getAsJsonObject();
            String hash = obj.get("hash").getAsString();
            Path objPath = layout.assetObject(hash);
            if (Files.exists(objPath)) continue;

            Files.createDirectories(objPath.getParent());
            String url = "https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash;
            downloadTo(url, objPath);
        }
    }

    private List<String> buildJvmArgs(FileLayout layout, LaunchRequest request, JsonObject versionJson, Map<String, Boolean> features) throws IOException {
        List<String> args = new ArrayList<>();
        JsonObject arguments = versionJson.getAsJsonObject("arguments");
        for (String token : ArgumentParser.collect(arguments.getAsJsonArray("jvm"), features)) {
            String replaced = replaceTokens(token, layout, request, versionJson);
            if ("-cp".equals(replaced) || "${classpath}".equals(replaced)) {
                continue; // we build classpath ourselves
            }
            args.add(replaced);
        }

        String classpath = buildClasspath(layout, versionJson, request);
        args.add("-cp");
        args.add(classpath);
        args.add("-Djava.library.path=" + layout.nativesDir(request.getVersionId()).toAbsolutePath());
        return args;
    }

    private List<String> buildGameArgs(FileLayout layout, LaunchRequest request, JsonObject versionJson, Map<String, Boolean> features) {
        List<String> raw = new ArrayList<>();
        JsonObject arguments = versionJson.getAsJsonObject("arguments");
        for (String token : ArgumentParser.collect(arguments.getAsJsonArray("game"), features)) {
            raw.add(replaceTokens(token, layout, request, versionJson));
        }

        List<String> pruned = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            String t = raw.get(i);
            if ("--demo".equals(t)) continue;
            if (t.startsWith("--quickPlay")) {
                // drop flag and its value if present
                if (i + 1 < raw.size() && !raw.get(i + 1).startsWith("--")) i++;
                continue;
            }
            if (t.contains("${quickPlay")) continue;
            pruned.add(t);
        }
        return pruned;
    }

    private String replaceTokens(String token, FileLayout layout, LaunchRequest request, JsonObject versionJson) {
        Map<String, String> values = new HashMap<>();
        values.put("auth_player_name", request.getPlayerName());
        values.put("version_name", request.getVersionId());
        values.put("game_directory", layout.root.resolve("").toAbsolutePath().toString());
        values.put("assets_root", layout.assetsDir().toAbsolutePath().toString());
        values.put("assets_index_name", versionJson.getAsJsonObject("assetIndex").get("id").getAsString());
        values.put("auth_uuid", request.getPlayerUuid().toString().replace("-", ""));
        values.put("auth_access_token", request.getAccessToken());
        values.put("clientid", "AxialLauncher");
        values.put("auth_xuid", request.getXuid());
        values.put("user_type", "msa");
        values.put("version_type", versionJson.get("type").getAsString());
        values.put("natives_directory", layout.nativesDir(request.getVersionId()).toAbsolutePath().toString());
        values.put("launcher_name", "AxialLauncher");
        values.put("launcher_version", "1");
        values.put("classpath_separator", System.getProperty("path.separator"));
        values.put("library_directory", layout.librariesDir().toAbsolutePath().toString());
        values.put("game_assets", layout.assetsDir().toAbsolutePath().toString());
        values.put("resolution_width", "854");
        values.put("resolution_height", "480");
        values.put("quickPlayPath", "");
        values.put("quickPlaySingleplayer", "");
        values.put("quickPlayMultiplayer", "");
        values.put("quickPlayRealms", "");

        String result = token;
        for (Map.Entry<String, String> e : values.entrySet()) {
            result = result.replace("${" + e.getKey() + "}", e.getValue());
        }
        return result;
    }

    private String buildClasspath(FileLayout layout, JsonObject versionJson, LaunchRequest request) {
        List<String> entries = new ArrayList<>();
        JsonArray mergedLibs = new JsonArray();
        if (versionJson.has("inheritsFrom")) {
            try {
                JsonObject base = readJson(layout.versionJson(versionJson.get("inheritsFrom").getAsString()));
                JsonArray baseLibs = base.getAsJsonArray("libraries");
                if (baseLibs != null) baseLibs.forEach(mergedLibs::add);
            } catch (IOException e) {
                logger.info("Failed to load base libraries for classpath merge: " + e.getMessage());
            }
        }
        JsonArray ownLibs = versionJson.getAsJsonArray("libraries");
        if (ownLibs != null) ownLibs.forEach(mergedLibs::add);

        for (JsonElement el : mergedLibs) {
            JsonObject lib = el.getAsJsonObject();
            if (!RuleEvaluator.isAllowed(lib, Collections.emptyMap())) continue;

            JsonObject downloads = null;
            JsonElement dlEl = lib.get("downloads");
            if (dlEl != null && dlEl.isJsonObject()) {
                downloads = dlEl.getAsJsonObject();
            }
            if (downloads == null) {
                String coords = lib.get("name").getAsString();
                String[] parts = coords.split(":");
                if (parts.length < 3) continue;
                String path = parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + ".jar";
                String base = lib.has("url") ? lib.get("url").getAsString() : "https://libraries.minecraft.net/";
                downloads = new JsonObject();
                JsonObject artifact = new JsonObject();
                artifact.addProperty("path", path);
                artifact.addProperty("url", base + path);
                downloads.add("artifact", artifact);
            }
            JsonObject artifact = downloads.getAsJsonObject("artifact");
            if (artifact != null) {
                entries.add(layout.librariesDir().resolve(artifact.get("path").getAsString()).toAbsolutePath().toString());
            }
        }
        // Safety: ensure jopt-simple is present for Main.main resolution
        boolean hasJopt = entries.stream().anyMatch(p -> p.contains("jopt-simple"));
        if (!hasJopt) {
            Path joptPath = layout.librariesDir().resolve("net/sf/jopt-simple/jopt-simple/5.0.4/jopt-simple-5.0.4.jar");
            if (Files.exists(joptPath)) {
                entries.add(joptPath.toAbsolutePath().toString());
            }
        }
        // Safety: ensure mojang logging (LogUtils) present
        boolean hasLogUtils = entries.stream().anyMatch(p -> p.contains("com/mojang/logging"));
        if (!hasLogUtils) {
            Path logUtils = layout.librariesDir().resolve("com/mojang/logging/logging/1.6.11/logging-1.6.11.jar");
            if (Files.exists(logUtils)) {
                entries.add(logUtils.toAbsolutePath().toString());
            }
        }

        entries.add(layout.clientJar(request.getVersionId()).toAbsolutePath().toString());
        logger.info("Classpath library count: " + entries.size());
        return String.join(System.getProperty("path.separator"), entries);
    }

    private void downloadToggleSprintMod(FileLayout layout) throws IOException {
        Files.createDirectories(layout.modsDir());
        // Remove legacy toggle-fix jars to avoid duplicate functionality
        try (var stream = Files.list(layout.modsDir())) {
            for (Path p : stream.toList()) {
                String name = p.getFileName().toString();
                if (name.startsWith("toggle-fix-") && name.endsWith(".jar")) {
                    Files.deleteIfExists(p);
                }
            }
        }
        Path target = layout.modsDir().resolve(TOGGLE_MOD_FILE);
        if (Files.exists(target)) {
            return;
        }
        logger.info("Fetching Toggle Sprint mod...");
        downloadTo(TOGGLE_MOD_URL, target);
    }

    private void downloadXaeroMinimap(FileLayout layout) throws IOException {
        Files.createDirectories(layout.modsDir());
        Path target = layout.modsDir().resolve(XAERO_MINIMAP_FILE);
        if (Files.exists(target)) {
            return;
        }
        logger.info("Fetching Xaero's Minimap...");
        downloadTo(XAERO_MINIMAP_URL, target);
    }

    private void downloadFabricApi(FileLayout layout) throws IOException {
        Files.createDirectories(layout.modsDir());
        Path target = layout.modsDir().resolve(FABRIC_API_FILE);
        if (Files.exists(target)) return;
        logger.info("Fetching Fabric API...");
        downloadTo(FABRIC_API_URL, target);
    }

    private void downloadModMenu(FileLayout layout) throws IOException {
        Files.createDirectories(layout.modsDir());
        Path target = layout.modsDir().resolve(MOD_MENU_FILE);
        if (Files.exists(target)) return;
        logger.info("Fetching Mod Menu...");
        downloadTo(MOD_MENU_URL, target);
    }

    private void downloadSimpleMenu(FileLayout layout) throws IOException {
        // no-op (removed)
    }

    private void downloadCollective(FileLayout layout) throws IOException {
        // no-op (removed)
    }

    private void removeSimpleMenu(FileLayout layout) throws IOException {
        Path target = layout.modsDir().resolve(SIMPLE_MENU_FILE);
        Files.deleteIfExists(target);
    }

    private void removeCollective(FileLayout layout) throws IOException {
        Path target = layout.modsDir().resolve(COLLECTIVE_FILE);
        Files.deleteIfExists(target);
    }

    private void installAxialCosmetics(FileLayout layout) throws IOException {
        Path mods = layout.modsDir();
        Files.createDirectories(mods);
        // Remove older axial-cosmetics jars to avoid version conflicts
        try (var stream = Files.list(mods)) {
            stream.filter(p -> p.getFileName().toString().startsWith("axial-cosmetics-") && p.toString().endsWith(".jar"))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        }

        Path target = mods.resolve(AXIAL_COSMETICS_FILE);

        try (InputStream in = MinecraftLauncher.class.getResourceAsStream("/" + AXIAL_COSMETICS_FILE)) {
            if (in == null) {
                logger.info("Axial cosmetics jar not packaged; skipping install.");
                return;
            }
            Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            logger.info("Installed Axial cosmetics mod: " + AXIAL_COSMETICS_FILE);
        }
    }

    private void installGeckoLib(FileLayout layout) throws IOException {
        Path mods = layout.modsDir();
        Files.createDirectories(mods);
        try (var stream = Files.list(mods)) {
            stream.filter(p -> p.getFileName().toString().startsWith("geckolib-fabric-") && p.getFileName().toString().endsWith(".jar"))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        }

        Path target = mods.resolve(GECKOLIB_FILE);
        if (Files.exists(target)) {
            return;
        }

        try (InputStream in = MinecraftLauncher.class.getResourceAsStream("/" + GECKOLIB_FILE)) {
            if (in == null) {
                logger.info("GeckoLib jar not packaged; skipping install.");
                return;
            }
            Files.copy(in, target);
            logger.info("Installed GeckoLib mod: " + GECKOLIB_FILE);
        }
    }

    private void installSodium(FileLayout layout) throws IOException {
        Path mods = layout.modsDir();
        Files.createDirectories(mods);
        try (var stream = Files.list(mods)) {
            stream.filter(p -> {
                        String name = p.getFileName().toString();
                        return name.equals("sodium.jar")
                                || name.startsWith("sodium-fabric-")
                                || name.startsWith("sodium-extra-")
                                || name.startsWith("reeses-sodium-options-");
                    })
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        }

        Path target = mods.resolve(SODIUM_FILE);
        try (InputStream in = MinecraftLauncher.class.getResourceAsStream("/" + SODIUM_FILE)) {
            if (in == null) {
                logger.info("Sodium jar not packaged; skipping install.");
                return;
            }
            Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            logger.info("Installed Sodium mod: " + SODIUM_FILE);
        }
    }

    private void installAxialUtils(FileLayout layout) throws IOException {
        Path mods = layout.modsDir();
        Files.createDirectories(mods);
        try (var stream = Files.list(mods)) {
            stream.filter(p -> p.getFileName().toString().startsWith("axialutils-") && p.getFileName().toString().endsWith(".jar"))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        }

        Path target = mods.resolve(AXIAL_UTILS_FILE);
        try (InputStream in = MinecraftLauncher.class.getResourceAsStream("/" + AXIAL_UTILS_FILE)) {
            if (in == null) {
                logger.info("AxialUtils jar not packaged; skipping install.");
                return;
            }
            Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            logger.info("Installed AxialUtils mod: " + AXIAL_UTILS_FILE);
        }
    }

    private void removeStaticBgMod(FileLayout layout) throws IOException {
        Files.createDirectories(layout.modsDir());
        try (var stream = Files.list(layout.modsDir())) {
            for (Path p : stream.toList()) {
                String name = p.getFileName().toString();
                if (name.startsWith("staticbgmod-") && name.endsWith(".jar")) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }

    private void installAxialPack(FileLayout layout) throws IOException {
        Path packDir = layout.resourcePackDir(AXIAL_PACK_NAME);
        // Clean up any old custom packs we created
        deleteRecursive(packDir);
        deleteRecursive(layout.resourcePackDir("axial_panorama"));
        // Stop here if user requested removal only
        logger.info("axial_pack removed.");
        return;
    }

    private void deleteRecursive(Path p) throws IOException {
        if (Files.notExists(p)) return;
        if (Files.isDirectory(p)) {
            try (var stream = Files.list(p)) {
                for (Path child : stream.toList()) {
                    deleteRecursive(child);
                }
            }
        }
        Files.deleteIfExists(p);
    }

    private void ensureResourcePacksEnabled(Path optionsFile, List<String> packIds) throws IOException {
        List<String> lines = Files.exists(optionsFile) ? Files.readAllLines(optionsFile) : new ArrayList<>();
        int idx = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("resourcePacks:")) {
                idx = i;
                break;
            }
        }
        if (idx == -1) {
            lines.add("resourcePacks:[\"vanilla\"]");
            idx = lines.size() - 1;
        }
        String line = lines.get(idx);
        int colon = line.indexOf(':');
        String rest = colon >= 0 ? line.substring(colon + 1).trim() : "[]";
        if (!rest.startsWith("[")) rest = "[]";
        String content = rest.substring(1, rest.endsWith("]") ? rest.length() - 1 : rest.length());
        List<String> current = new ArrayList<>();
        if (!content.isBlank()) {
            for (String s : content.split(",")) {
                String t = s.trim().replace("\"", "");
                if (!t.isBlank()) current.add(t);
            }
        }
        // drop any custom packs we previously generated
        current.removeIf(s -> s.contains("axial_panorama") || s.contains("axial_pack"));
        // prepend our fresh pack
        String packId = "file/" + packIds.get(0);
        current.add(0, packId);
        if (!current.contains("vanilla")) current.add("vanilla");

        String newLine = "resourcePacks:[\"" + String.join("\",\"", current) + "\"]";
        lines.set(idx, newLine);
        Files.write(optionsFile, lines);
    }

    private void setPanoramaSpeed(Path optionsFile) throws IOException {
        List<String> lines = Files.exists(optionsFile) ? Files.readAllLines(optionsFile) : new ArrayList<>();
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("panoramaScrollSpeed:")) {
                lines.set(i, "panoramaScrollSpeed:0.0");
                found = true;
                break;
            }
        }
        if (!found) lines.add("panoramaScrollSpeed:0.0");
        Files.write(optionsFile, lines);
    }

    private void writeSimpleMenuConfig(FileLayout layout, String packName) throws IOException {
        Path cfg = layout.configDir().resolve("simplemenu.json5");
        Files.createDirectories(cfg.getParent());
        String background = packName + ":textures/gui/title/background.png";
        List<String> lines = new ArrayList<>();
        lines.add("{");
        lines.add("  \"useCustomMenuBackground\": true,");
        lines.add("  \"customMenuBackgroundResourceLocation\": \"" + background + "\",");
        lines.add("  \"menuBackgroundMovementSpeedModifier\": 0.0,");
        lines.add("  \"menuBackgroundRotate\": false,");
        lines.add("  \"menuBackgroundZoom\": 1.0");
        lines.add("}");
        Files.write(cfg, lines);
    }

    private void writeTransparentPng(Path target) throws IOException {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Files.createDirectories(target.getParent());
        try (var out = Files.newOutputStream(target)) {
            javax.imageio.ImageIO.write(img, "png", out);
        }
    }

    private Path downloadArtifact(Path baseDir, JsonObject artifact) throws IOException {
        String path = artifact.get("path").getAsString();
        Path target = baseDir.resolve(path);
        if (Files.exists(target)) return target;

        Files.createDirectories(target.getParent());
        downloadTo(artifact.get("url").getAsString(), target);
        return target;
    }

    private void downloadTo(String url, Path target) throws IOException {
        Request req = new Request.Builder().url(url).build();
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Download failed: " + url + " -> " + resp.code());
            }
            try (InputStream in = resp.body().byteStream();
                 OutputStream out = Files.newOutputStream(target)) {
                in.transferTo(out);
            }
        }
        logger.info("Fetched " + url);
    }

    private JsonObject readJson(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return gson.fromJson(new java.io.InputStreamReader(in), JsonObject.class);
        }
    }

    private JsonObject mergeInherited(JsonObject base, JsonObject child) {
        JsonObject merged = gson.fromJson(base, JsonObject.class);
        JsonArray baseLibs = base.getAsJsonArray("libraries");
        JsonArray childLibs = child.getAsJsonArray("libraries");
        logger.info("Merging inheritance: base libs=" + (baseLibs == null ? 0 : baseLibs.size()) + " child libs=" + (childLibs == null ? 0 : childLibs.size()));
        merged.remove("libraries");
        merged.add("libraries", mergeArrays(baseLibs, childLibs));

        JsonObject baseArgs = base.getAsJsonObject("arguments");
        JsonObject childArgs = child.getAsJsonObject("arguments");
        JsonObject combinedArgs = new JsonObject();
        combinedArgs.add("jvm", mergeArrays(baseArgs.getAsJsonArray("jvm"), childArgs.getAsJsonArray("jvm")));
        combinedArgs.add("game", mergeArrays(baseArgs.getAsJsonArray("game"), childArgs.getAsJsonArray("game")));
        merged.add("arguments", combinedArgs);

        if (child.has("mainClass")) merged.addProperty("mainClass", child.get("mainClass").getAsString());
        merged.remove("inheritsFrom");
        return merged;
    }

    private JsonArray mergeArrays(JsonArray a, JsonArray b) {
        JsonArray out = new JsonArray();
        if (a != null) a.forEach(out::add);
        if (b != null) b.forEach(out::add);
        return out;
    }

    private JsonObject selectNative(JsonObject classifiers) {
        String osKey = OS.current().classifierKey();
        if (classifiers.has(osKey)) return classifiers.getAsJsonObject(osKey);

        // fallback: try without arch suffix
        String generic = OS.current().genericClassifierKey();
        if (generic != null && classifiers.has(generic)) return classifiers.getAsJsonObject(generic);
        return null;
    }

    private void extractNatives(Path archive, Path targetDir, JsonObject library) throws IOException {
        Files.createDirectories(targetDir);
        Set<String> excludes = new HashSet<>();
        JsonObject extract = library.getAsJsonObject("extract");
        if (extract != null && extract.has("exclude")) {
            JsonArray arr = extract.getAsJsonArray("exclude");
            for (JsonElement el : arr) excludes.add(el.getAsString());
        }

        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory()) continue;
                if (excludes.stream().anyMatch(name::startsWith)) continue;

                Path outPath = targetDir.resolve(name.substring(name.lastIndexOf('/') + 1));
                try (OutputStream out = Files.newOutputStream(outPath)) {
                    zin.transferTo(out);
                }
            }
        }
    }

    private static class VersionManifest {
        private final List<VersionRef> versions;

        VersionManifest(JsonObject obj) {
            JsonArray arr = obj.getAsJsonArray("versions");
            List<VersionRef> list = new ArrayList<>();
            for (JsonElement el : arr) {
                JsonObject v = el.getAsJsonObject();
                list.add(new VersionRef(
                        v.get("id").getAsString(),
                        v.get("type").getAsString(),
                        v.get("url").getAsString(),
                        v.get("releaseTime").getAsString()
                ));
            }
            this.versions = List.copyOf(list);
        }

        Optional<VersionRef> find(String id) {
            return versions.stream().filter(v -> v.id().equals(id)).findFirst();
        }
    }

    private record VersionRef(String id, String type, String url, String releaseTime) {
    }

    private static class ArgumentParser {
        static List<String> collect(JsonArray arr, Map<String, Boolean> features) {
            List<String> result = new ArrayList<>();
            if (arr == null) return result;

            for (JsonElement el : arr) {
                if (el.isJsonPrimitive()) {
                    result.add(el.getAsString());
                } else {
                    JsonObject obj = el.getAsJsonObject();
                    if (RuleEvaluator.isAllowed(obj, features)) {
                        JsonElement value = obj.get("value");
                        if (value.isJsonArray()) {
                            for (JsonElement v : value.getAsJsonArray()) {
                                result.add(v.getAsString());
                            }
                        } else {
                            result.add(value.getAsString());
                        }
                    }
                }
            }
            return result;
        }
    }

    private static class RuleEvaluator {
        static boolean isAllowed(JsonObject obj, Map<String, Boolean> features) {
            JsonArray rules = obj.getAsJsonArray("rules");
            if (rules == null) return true;

            Boolean allow = null;
            for (JsonElement ruleEl : rules) {
                JsonObject rule = ruleEl.getAsJsonObject();
                String action = rule.get("action").getAsString();
                JsonObject os = rule.getAsJsonObject("os");
                JsonObject featureReq = rule.getAsJsonObject("features");

                boolean osOk = os == null || OS.current().matches(os);
                boolean featuresOk = featureReq == null || featureReq.entrySet().stream()
                        .allMatch(e -> features.getOrDefault(e.getKey(), false) == e.getValue().getAsBoolean());

                if (osOk && featuresOk) {
                    allow = "allow".equals(action);
                }
            }
            return allow == null || allow;
        }
    }

    private record OS(String name, String arch) {
        static OS current() {
            String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);

            if (osName.contains("win")) return new OS("windows", arch.contains("aarch64") || arch.contains("arm") ? "arm64" : "x64");
            if (osName.contains("mac") || osName.contains("darwin")) return new OS("macos", arch.contains("aarch64") || arch.contains("arm") ? "arm64" : "x64");
            return new OS("linux", arch.contains("aarch64") || arch.contains("arm") ? "arm64" : "x64");
        }

        boolean matches(JsonObject osRule) {
            if (osRule.has("name")) {
                if (!osRule.get("name").getAsString().equalsIgnoreCase(name)) return false;
            }
            if (osRule.has("arch")) {
                String target = osRule.get("arch").getAsString().toLowerCase(Locale.ROOT);
                if (!arch.contains(target)) return false;
            }
            return true;
        }

        String classifierKey() {
            String suffix = arch.equals("arm64") ? "-arm64" : "";
            String base = switch (name) {
                case "windows" -> "natives-windows";
                case "macos" -> "natives-macos";
                default -> "natives-linux";
            };
            return base + suffix;
        }

        String genericClassifierKey() {
            return switch (name) {
                case "windows" -> "natives-windows";
                case "macos" -> "natives-osx";
                default -> "natives-linux";
            };
        }
    }
}
