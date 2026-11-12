package com.axial.cosmetics.mixin;

import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class FeedbackButtonSkinTest {
    @Test void packagedSkinPreservesTheFeedbackRendererAcrossAllStages() throws Exception {
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())){
            var skin=read(jar,"com/axial/cosmetics/mixin/InGameButtonSkinMixin");
            var render=skin.methods.stream().filter(m->m.name.equals("axial_cosmetics$renderInGameButton")).findFirst().orElseThrow();
            TypeInsnNode exemption=null;
            for(var instruction:render.instructions)if(instruction instanceof TypeInsnNode type&&type.getOpcode()==Opcodes.INSTANCEOF&&type.desc.equals("com/axial/cosmetics/client/FeedbackScreen"))exemption=type;
            assertNotNull(exemption,"Feedback must retain its own button renderer rather than the generic texture skin");
            var next=exemption.getNext();while(next.getOpcode()<0)next=next.getNext();
            var branch=assertInstanceOf(JumpInsnNode.class,next);assertEquals(Opcodes.IFNE,branch.getOpcode());
            next=branch.label.getNext();while(next.getOpcode()<0)next=next.getNext();
            assertEquals(Opcodes.RETURN,next.getOpcode());
            var screen=read(jar,"com/axial/cosmetics/client/FeedbackScreen");
            var factory=screen.methods.stream().filter(m->m.name.equals("control")).findFirst().orElseThrow();
            assertTrue(java.util.stream.StreamSupport.stream(factory.instructions.spliterator(),false).anyMatch(n->n instanceof TypeInsnNode t&&t.getOpcode()==Opcodes.NEW&&t.desc.endsWith("FeedbackScreen$FeedbackButton")),"Category, type, Back and Submit share the dedicated feedback control");
            var button=read(jar,"com/axial/cosmetics/client/FeedbackScreen$FeedbackButton");
            assertTrue(button.methods.stream().anyMatch(m->m.name.equals("drawContents")));
        }
    }
    private static ClassNode read(JarFile jar,String path)throws Exception{
        var entry=jar.getJarEntry(path+".class");assertNotNull(entry,path);
        try(var input=jar.getInputStream(entry)){var node=new ClassNode();new ClassReader(input).accept(node,0);return node;}
    }
}
