package com.axial.cosmetics.client;

import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class ReplayRecordingRestartTest {
    @Test void reEnablingRestoresStartOnlyWhenThereIsNoActiveRecorder() {
        assertFalse(ReplayRecordingRestart.showStart(false,true,false,false));
        assertTrue(ReplayRecordingRestart.showStart(true,true,false,false));
        assertFalse(ReplayRecordingRestart.showStart(true,true,false,true));
        assertFalse(ReplayRecordingRestart.showStart(true,false,false,false));
        assertFalse(ReplayRecordingRestart.showStart(true,true,true,false));
        assertTrue(ReplayRecordingRestart.showStart(true,true,false,false)); // after finishing the old recorder
    }
    @Test void explicitStartUsesPinnedNativeSettingsWithoutChangingSavedPreferences()throws Exception {
        try(var jar=new JarFile("src/main/resources/replaymod-1.21.11-2.6.28.jar")) {
            var connection=read(jar,"com/replaymod/recording/handler/ConnectionEventHandler");
            var connect=connection.methods.stream().filter(m->m.name.equals("onConnectedToServerEvent")).findFirst().orElseThrow();int settings=0,serverChoice=0;
            for(var instruction:connect.instructions)if(instruction instanceof MethodInsnNode call) {
                if(call.owner.equals("com/replaymod/core/SettingsRegistry")&&call.name.equals("get")&&call.desc.equals("(Lcom/replaymod/core/SettingsRegistry$SettingKey;)Ljava/lang/Object;"))settings++;
                if(call.owner.equals("com/replaymod/recording/ServerInfoExt")&&call.name.equals("getAutoRecording")&&call.desc.equals("()Ljava/lang/Boolean;"))serverChoice++;
            }
            assertTrue(settings>=3);assertEquals(1,serverChoice);
            var type=read(jar,"com/replaymod/recording/Setting");for(String name:new String[]{"RECORD_SERVER","RECORD_SINGLEPLAYER","AUTO_START_RECORDING"})assertTrue(type.fields.stream().anyMatch(f->f.name.equals(name)));
        }
        try(var jar=new JarFile(System.getProperty("axial.test.modJar"))) {
            assertNotNull(jar.getJarEntry("com/axial/cosmetics/client/ReplayRecordingRestart.class"));
            assertNotNull(jar.getJarEntry("com/axial/cosmetics/mixin/ReplayRecordingRestartButtonMixin.class"));
            assertTrue(new String(jar.getInputStream(jar.getJarEntry("axial-cosmetics.mixins.json")).readAllBytes()).contains("ReplayRecordingRestartButtonMixin"));
        }
    }
    private ClassNode read(JarFile jar,String name)throws Exception {var node=new ClassNode();try(var in=jar.getInputStream(jar.getJarEntry(name+".class"))){new ClassReader(in).accept(node,0);}return node;}
}
