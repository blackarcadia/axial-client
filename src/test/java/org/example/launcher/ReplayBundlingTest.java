package org.example.launcher;

import com.google.gson.JsonParser;
import java.nio.file.*;
import java.util.jar.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import static org.junit.jupiter.api.Assertions.*;

class ReplayBundlingTest {
    private static final String REPLAY="replaymod-1.21.11-2.6.28.jar";
    @TempDir Path directory;
    private ClassNode read(JarFile jar,String name)throws Exception {
        var node=new ClassNode();try(var in=jar.getInputStream(jar.getJarEntry(name+".class"))){new ClassReader(in).accept(node,0);}return node;
    }
    @Test void suppliedReplayJarIncludesItsDependenciesAndThePinnedViewerApi()throws Exception {
        try(var jar=new JarFile("src/main/resources/"+REPLAY)) {
            var metadata=JsonParser.parseString(new String(jar.getInputStream(jar.getJarEntry("fabric.mod.json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("replaymod",metadata.get("id").getAsString());assertEquals("1.21.11-2.6.28",metadata.get("version").getAsString());
            for(var nested:metadata.getAsJsonArray("jars"))assertNotNull(jar.getJarEntry(nested.getAsJsonObject().get("file").getAsString()));
            var viewer=read(jar,"com/replaymod/replay/gui/screen/GuiReplayViewer");
            assertTrue(viewer.methods.stream().anyMatch(m->m.name.equals("<init>")&&m.desc.equals("(Lcom/replaymod/replay/ReplayModReplay;)V")));
            assertTrue(viewer.fields.stream().anyMatch(f->f.name.equals("cancelButton")));
            var gui=read(jar,"com/replaymod/lib/de/johni0702/minecraft/gui/container/AbstractGuiScreen");
            assertTrue(gui.methods.stream().anyMatch(m->m.name.equals("toMinecraft")&&m.desc.equals("()Lnet/minecraft/class_437;")));
            var handler=read(jar,"com/replaymod/replay/handler/GuiHandler");
            assertTrue(handler.methods.stream().anyMatch(m->m.name.equals("injectIntoMainMenu")&&m.desc.equals("(Lnet/minecraft/class_437;Ljava/util/Collection;)V")));
        }
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())) {
            assertArrayEquals(Files.readAllBytes(Path.of("src/main/resources",REPLAY)),jar.getInputStream(jar.getJarEntry(REPLAY)).readAllBytes());
            var mixin=read(jar,"com/axial/cosmetics/mixin/ReplayMainMenuButtonMixin");
            assertTrue(mixin.methods.stream().anyMatch(m->m.name.equals("axial$useSubtleReplayButton")&&m.desc.equals("(Lnet/minecraft/class_437;Ljava/util/Collection;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V")));
            assertTrue(new String(jar.getInputStream(jar.getJarEntry("axial-cosmetics.mixins.json")).readAllBytes()).contains("ReplayMainMenuButtonMixin"));
        }
    }
    @Test void updaterInstallsReplayAndReplacesOldVersionsWithoutTouchingRecordings()throws Exception {
        var mods=Files.createDirectories(directory.resolve("mods"));var recordings=Files.createDirectories(directory.resolve("replay_recordings"));
        Files.writeString(mods.resolve("replaymod-old.jar"),"old");Files.writeString(recordings.resolve("my-replay.mcpr"),"recording");
        byte[] replay=Files.readAllBytes(Path.of("src/main/resources",REPLAY));var bundle=directory.resolve("bundle.jar");
        try(var out=new JarOutputStream(Files.newOutputStream(bundle))) {
            out.putNextEntry(new JarEntry("axial-cosmetics.jar"));out.write(new byte[]{1});out.closeEntry();
            out.putNextEntry(new JarEntry(REPLAY));out.write(replay);out.closeEntry();
        }
        try(var jar=new JarFile(bundle.toFile())){assertTrue(GitHubReleaseUpdater.installPackagedJarsFromBundle(jar,mods).contains(REPLAY));}
        assertFalse(Files.exists(mods.resolve("replaymod-old.jar")));assertArrayEquals(replay,Files.readAllBytes(mods.resolve(REPLAY)));
        assertEquals("recording",Files.readString(recordings.resolve("my-replay.mcpr")));
    }
}
