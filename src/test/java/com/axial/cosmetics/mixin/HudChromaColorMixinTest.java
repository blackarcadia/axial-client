package com.axial.cosmetics.mixin;

import java.util.*;
import java.util.jar.JarFile;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class HudChromaColorMixinTest {
    @Test void chromaHooksCoverEveryConfiguredColourReadInTheBundledHudRenderers() throws Exception {
        var mixin=read("com/axial/cosmetics/mixin/HudChromaColorMixin");var targets=new HashSet<String>();
        for(var method:mixin.methods)if(method.invisibleAnnotations!=null)for(var annotation:method.invisibleAnnotations)
            if(annotation.desc.endsWith("/ModifyExpressionValue;")) {
                @SuppressWarnings("unchecked")var hooks=(List<AnnotationNode>)value(annotation,"at");
                for(var hook:hooks)targets.add((String)value(hook,"target"));
            }
        // MixinExtras may retain the annotation at runtime, depending on the bundled version.
        for(var method:mixin.methods)if(method.visibleAnnotations!=null)for(var annotation:method.visibleAnnotations)
            if(annotation.desc.endsWith("/ModifyExpressionValue;")) {
                @SuppressWarnings("unchecked")var hooks=(List<AnnotationNode>)value(annotation,"at");
                for(var hook:hooks)targets.add((String)value(hook,"target"));
            }
        var observed=new HashSet<String>();
        for(String renderer:List.of("AxialHudRenderer","CpsHudRenderer","SatchelHudRenderer","ArmorHudRenderer")) {
            var node=read("org/axial/axialutils/client/"+renderer);int reads=0;
            for(var method:node.methods)for(var insn:method.instructions)if(insn instanceof FieldInsnNode field
                    &&field.owner.equals("org/axial/axialutils/client/AxialConfig")&&field.name.endsWith("Color")&&field.desc.equals("I")) {
                String target="L"+field.owner+";"+field.name+":"+field.desc;
                assertTrue(targets.contains(target),renderer+": "+target);observed.add(target);reads++;
            }
            assertTrue(reads>0,renderer);
        }
        assertTrue(observed.size()>=9);
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())) {
            var json=new String(jar.getInputStream(jar.getJarEntry("axial-cosmetics.mixins.json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(json.contains("\"HudChromaColorMixin\""));
        }
    }
    private static Object value(AnnotationNode a,String key){int index=a.values.indexOf(key);assertTrue(index>=0);return a.values.get(index+1);}
    private static ClassNode read(String name)throws Exception {
        try(var input=HudChromaColorMixinTest.class.getResourceAsStream("/"+name+".class")) {
            assertNotNull(input,name);var node=new ClassNode();new ClassReader(input).accept(node,0);return node;
        }
    }
}
