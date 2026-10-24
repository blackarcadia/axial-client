package com.axial.cosmetics.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.nio.file.Path;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

class EnchantmentsButtonSkinTest {
    @Test void packagedSkinLeavesEnchantmentsControlsToTheirOwnRenderer()throws Exception{
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())){
            var skin=read(jar,"com/axial/cosmetics/mixin/InGameButtonSkinMixin");
            var method=skin.methods.stream().filter(m->m.name.equals("axial_cosmetics$renderInGameButton")).findFirst().orElseThrow();
            TypeInsnNode check=null;
            for(var instruction:method.instructions)if(instruction instanceof TypeInsnNode type&&type.getOpcode()==Opcodes.INSTANCEOF&&type.desc.equals("com/axial/cosmetics/client/EnchantmentsScreen")){check=type;break;}
            assertNotNull(check,"The generic texture skin must exempt the enchantment guide");
            AbstractInsnNode next=check.getNext();while(next.getOpcode()<0)next=next.getNext();
            assertInstanceOf(JumpInsnNode.class,next);
            var branch=(JumpInsnNode)next;assertEquals(Opcodes.IFNE,branch.getOpcode());
            next=branch.label.getNext();while(next.getOpcode()<0)next=next.getNext();
            assertEquals(Opcodes.RETURN,next.getOpcode(),"The exemption must return before replacing the button rendering");
            var guide=read(jar,"com/axial/cosmetics/client/EnchantmentsScreen");
            assertNotEquals(0,guide.access&Opcodes.ACC_PUBLIC,"The skin mixin targets another package");
        }
    }
    private static ClassNode read(JarFile jar,String name)throws Exception{
        var entry=jar.getJarEntry(name+".class");assertNotNull(entry,name);
        try(var in=jar.getInputStream(entry)){var node=new ClassNode();new ClassReader(in).accept(node,0);return node;}
    }
}
