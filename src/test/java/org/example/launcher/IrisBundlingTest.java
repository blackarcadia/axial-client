package org.example.launcher;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import static org.junit.jupiter.api.Assertions.*;

class IrisBundlingTest {
    private static final String IRIS="iris-fabric-1.10.8+mc1.21.11.jar";
    @TempDir Path directory;
    @Test void suppliedIrisAndBundledSodiumDeclareCompatibleVersions()throws Exception{
        var iris=metadata(IRIS);var sodium=metadata("sodium-fabric-0.8.13+mc1.21.11.jar");
        assertEquals("iris",iris.get("id").getAsString());assertEquals("1.10.8+mc1.21.11",iris.get("version").getAsString());
        assertEquals("0.8.13+mc1.21.11",sodium.get("version").getAsString());
        assertEquals("0.8.x",iris.getAsJsonObject("depends").getAsJsonArray("sodium").get(0).getAsString());
    }
    @Test void updateReplacesOlderIrisAndLeavesShaderPreferencesIntact()throws Exception{
        Path mods=Files.createDirectories(directory.resolve("mods"));
        Files.writeString(mods.resolve("iris-fabric-older.jar"),"old iris");
        Path preferences=Files.createDirectories(directory.resolve("config")).resolve("iris.properties");
        Files.writeString(preferences,"enableShaders=true\nshaderPack=chosen.zip\n");
        Path pack=Files.createDirectories(directory.resolve("shaderpacks")).resolve("chosen.zip");Files.writeString(pack,"user shader pack");
        Path bundle=directory.resolve("bundle.jar");byte[] iris;
        try(var input=getClass().getResourceAsStream("/"+IRIS)){assertNotNull(input);iris=input.readAllBytes();}
        try(var output=new JarOutputStream(Files.newOutputStream(bundle))){
            output.putNextEntry(new JarEntry("axial-cosmetics.jar"));output.write(new byte[]{1});output.closeEntry();
            output.putNextEntry(new JarEntry(IRIS));output.write(iris);output.closeEntry();
        }
        try(var jar=new JarFile(bundle.toFile())){assertTrue(GitHubReleaseUpdater.installPackagedJarsFromBundle(jar,mods).contains(IRIS));}
        assertFalse(Files.exists(mods.resolve("iris-fabric-older.jar")));assertArrayEquals(iris,Files.readAllBytes(mods.resolve(IRIS)));
        assertTrue(Files.readString(preferences).contains("chosen.zip"));assertEquals("user shader pack",Files.readString(pack));
    }
    private com.google.gson.JsonObject metadata(String resource)throws Exception{
        byte[] metadata=null;Set<String> entries=new HashSet<>();
        try(var raw=getClass().getResourceAsStream("/"+resource)){
            assertNotNull(raw,resource);try(var jar=new JarInputStream(raw)){
                java.util.zip.ZipEntry entry;while((entry=jar.getNextEntry())!=null){entries.add(entry.getName());if(entry.getName().equals("fabric.mod.json"))metadata=jar.readAllBytes();}
            }
        }
        assertNotNull(metadata);var json=JsonParser.parseString(new String(metadata,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        for(var nested:json.getAsJsonArray("jars"))assertTrue(entries.contains(nested.getAsJsonObject().get("file").getAsString()),"Missing nested dependency");
        return json;
    }
}
