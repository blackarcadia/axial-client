package com.axial.cosmetics.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.Path;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

class GangsMenuCompatibilityTest {
    @Test void packagedGenericButtonSkinExemptsTheGangBrowser()throws Exception{
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())){
            var skin=read(jar,"com/axial/cosmetics/mixin/InGameButtonSkinMixin");
            var method=skin.methods.stream().filter(m->m.name.equals("axial_cosmetics$renderInGameButton")).findFirst().orElseThrow();
            TypeInsnNode check=null;for(var n:method.instructions)if(n instanceof TypeInsnNode type&&type.getOpcode()==Opcodes.INSTANCEOF&&type.desc.equals("com/axial/cosmetics/client/GangsScreen")){check=type;break;}
            assertNotNull(check);AbstractInsnNode next=check.getNext();while(next.getOpcode()<0)next=next.getNext();
            var branch=(JumpInsnNode)next;assertEquals(Opcodes.IFNE,branch.getOpcode());next=branch.label.getNext();while(next.getOpcode()<0)next=next.getNext();assertEquals(Opcodes.RETURN,next.getOpcode());
            assertNotEquals(0,read(jar,"com/axial/cosmetics/client/GangsScreen").access&Opcodes.ACC_PUBLIC);
        }
    }
    private static ClassNode read(JarFile jar,String name)throws Exception{var entry=jar.getJarEntry(name+".class");assertNotNull(entry,name);try(var in=jar.getInputStream(entry)){var node=new ClassNode();new ClassReader(in).accept(node,0);return node;}}
}
