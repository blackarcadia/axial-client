package com.axial.cosmetics.client;

import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class ArmorCosmeticRegistrationTest {
    @Test void shippedClientRegistersUniqueArmorIdsAndCorrectNewSkins() throws Exception {
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())) {
            var node=new ClassNode();
            try(var in=jar.getInputStream(jar.getJarEntry("com/axial/cosmetics/AxialCosmetics.class"))) {
                new ClassReader(in).accept(node,0);
            }
            var initializer=node.methods.stream().filter(m->m.name.equals("onInitializeClient")).findFirst().orElseThrow();
            var models=new HashMap<Integer,String>();Integer cmd=null;String model=null;
            for(var instruction:initializer.instructions) {
                if(instruction instanceof IntInsnNode value&&(value.getOpcode()==Opcodes.BIPUSH||value.getOpcode()==Opcodes.SIPUSH))cmd=value.operand;
                if(instruction instanceof LdcInsnNode value) {
                    if(value.cst instanceof Integer id)cmd=id;
                    if(value.cst instanceof String path&&path.startsWith("geckolib/models/"))model=path;
                }
                if(instruction instanceof MethodInsnNode call&&call.owner.equals("org/axial/axialutils/client/cosmetics/ArmorCosmeticRegistry")
                        &&(call.name.equals("register")||call.name.equals("registerFullModel"))) {
                    assertNotNull(cmd);assertNotNull(model);
                    assertNull(models.putIfAbsent(cmd,model),"Duplicate armor cosmetic CMD: "+cmd);
                    cmd=null;model=null;
                }
            }
            assertTrue(models.size()>=10,"Expected armor registrations in shipped initializer");
            assertEquals("geckolib/models/cowboyboots.geo.json",models.get(311));
            assertEquals("geckolib/models/witchhat.geo.json",models.get(312));
            for(var path:models.values())assertNotNull(jar.getJarEntry("assets/axial_cosmetics/"+path),path);
        }
    }
}
