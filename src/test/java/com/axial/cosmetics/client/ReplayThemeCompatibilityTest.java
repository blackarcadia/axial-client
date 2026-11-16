package com.axial.cosmetics.client;

import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class ReplayThemeCompatibilityTest {
    static final String GUI="com/replaymod/lib/de/johni0702/minecraft/gui/";
    ClassNode read(JarFile jar,String name)throws Exception {
        var type=new ClassNode();try(var in=jar.getInputStream(jar.getJarEntry(name+".class"))){new ClassReader(in).accept(type,0);}return type;
    }
    void method(JarFile jar,String type,String name,String descriptor)throws Exception {
        assertTrue(read(jar,type).methods.stream().anyMatch(m->m.name.equals(name)&&m.desc.equals(descriptor)),type+"."+name+descriptor);
    }
    @Test void pinnedViewerHasTheNonVanillaBackgroundAndWidgetApisWeTheme()throws Exception {
        try(var jar=new JarFile("src/main/resources/replaymod-1.21.11-2.6.28.jar")) {
            method(jar,GUI+"container/AbstractGuiScreen$MinecraftGuiScreen","method_25420","(Lnet/minecraft/class_332;IIF)V");
            method(jar,GUI+"element/AbstractGuiButton","draw","(L"+GUI+"GuiRenderer;L"+GUI+"utils/lwjgl/ReadableDimension;L"+GUI+"RenderInfo;)V");
            method(jar,GUI+"element/AbstractGuiButton","getLabel","()Ljava/lang/String;");
            method(jar,GUI+"element/AbstractGuiButton","getTexture","()Lnet/minecraft/class_2960;");
            method(jar,GUI+"element/AbstractGuiElement","isEnabled","()Z");
            method(jar,GUI+"GuiRenderer","getContext","()Lnet/minecraft/class_332;");
            method(jar,GUI+"GuiRenderer","getOpenGlOffset","()L"+GUI+"utils/lwjgl/ReadablePoint;");
            method(jar,GUI+"utils/lwjgl/ReadablePoint","getX","()I");method(jar,GUI+"utils/lwjgl/ReadablePoint","getY","()I");
        }
    }
    @Test void recordingGatePinsTheNativeStartControlsAndSafeFinalizationContract()throws Exception {
        try(var jar=new JarFile("src/main/resources/replaymod-1.21.11-2.6.28.jar")) {
            method(jar,"com/replaymod/recording/ReplayModRecording","initiateRecording","(Lnet/minecraft/class_2535;)V");
            method(jar,"com/replaymod/recording/handler/ConnectionEventHandler","onConnectedToServerEvent","(Lnet/minecraft/class_2535;)V");
            method(jar,"com/replaymod/recording/gui/GuiRecordingControls","injectIntoIngameMenu","(Lnet/minecraft/class_437;Ljava/util/Collection;)V");
            for(String name:new String[]{"updateState","lambda$new$1","lambda$new$3"})method(jar,"com/replaymod/recording/gui/GuiRecordingControls",name,"()V");
            var controls=read(jar,"com/replaymod/recording/gui/GuiRecordingControls");
            for(String field:new String[]{"packetListener","buttonStartStop","buttonPauseResume"})assertTrue(controls.fields.stream().anyMatch(f->f.name.equals(field)));
            var listener=read(jar,"com/replaymod/recording/packet/PacketListener");
            for(String field:new String[]{"channel","RAW_RECORDER_KEY","DECODED_RECORDER_KEY"})assertTrue(listener.fields.stream().anyMatch(f->f.name.equals(field)));
            var finish=listener.methods.stream().filter(m->m.name.equals("channelInactive")).findFirst().orElseThrow();
            // This native method finalizes the archive, never dereferences the optional context.
            for(var instruction:finish.instructions)if(instruction instanceof VarInsnNode variable)assertFalse(variable.getOpcode()==Opcodes.ALOAD&&variable.var==1,"Native finish must accept a null handler context");
            method(jar,"com/replaymod/replaystudio/protocol/Packet","release","()Z");
        }
        try(var jar=new JarFile(System.getProperty("axial.test.modJar"))) {
            var config=new String(jar.getInputStream(jar.getJarEntry("axial-cosmetics.mixins.json")).readAllBytes());
            for(String mixin:new String[]{"ReplayViewerBackgroundMixin","ReplayViewerButtonsMixin","ReplayRecordingStartMixin","ReplayRecordingConnectionMixin","ReplayRecordingControlsMixin","ReplayRecordingPacketMixin"}) {
                assertTrue(config.contains(mixin));assertNotNull(jar.getJarEntry("com/axial/cosmetics/mixin/"+mixin+".class"));
            }
        }
    }
}
