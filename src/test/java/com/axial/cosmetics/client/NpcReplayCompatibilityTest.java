package com.axial.cosmetics.client;

import java.nio.file.*;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class NpcReplayCompatibilityTest {
    ClassNode read(JarFile jar,String name)throws Exception {
        var node=new ClassNode();try(var input=jar.getInputStream(jar.getJarEntry(name+".class"))){new ClassReader(input).accept(node,0);}return node;
    }
    @Test void pinnedRecordingQueueAndReplayClockMatchTheIntegration()throws Exception {
        try(var jar=new JarFile("src/main/resources/replaymod-1.21.11-2.6.28.jar")) {
            var listener=read(jar,"com/replaymod/recording/packet/PacketListener");
            assertTrue(listener.fields.stream().anyMatch(f->f.name.equals("replayFile")));
            assertTrue(listener.fields.stream().anyMatch(f->f.name.equals("saveService")&&f.desc.equals("Ljava/util/concurrent/ExecutorService;")));
            assertTrue(listener.methods.stream().anyMatch(m->m.name.equals("getCurrentDuration")&&m.desc.equals("()J")));
            var sender=read(jar,"com/replaymod/replay/ReplaySender");
            assertTrue(sender.methods.stream().anyMatch(m->m.name.equals("currentTimeStamp")&&m.desc.equals("()I")));
        }
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())) {
            var clock=read(jar,"com/axial/cosmetics/mixin/NpcEmoteClockMixin");
            assertTrue(clock.methods.stream().anyMatch(m->m.name.equals("axial$sampleActorPose")&&m.desc.equals("(Lcom/zigythebird/playeranimcore/animation/AnimationData;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V")));
            assertTrue(clock.fields.stream().anyMatch(f->f.name.equals("tick")&&f.desc.equals("I")));
            assertTrue(clock.fields.stream().anyMatch(f->f.name.equals("startAnimFrom")&&f.desc.equals("F")));
            var render=read(jar,"com/axial/cosmetics/mixin/NpcEmoteRenderMixin");
            assertTrue(render.methods.stream().anyMatch(m->m.name.equals("axial$updateActorPose")&&m.desc.startsWith("(Lnet/minecraft/class_11890;")));
            var mixins=new String(jar.getInputStream(jar.getJarEntry("axial-cosmetics.mixins.json")).readAllBytes());
            assertTrue(mixins.contains("NpcEmoteClockMixin"));assertTrue(mixins.contains("NpcEmoteRenderMixin"));
        }
    }
}
