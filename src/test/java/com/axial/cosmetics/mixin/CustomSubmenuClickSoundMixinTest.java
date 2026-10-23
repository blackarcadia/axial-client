package com.axial.cosmetics.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

class CustomSubmenuClickSoundMixinTest {
    @Test void everyPackagedSoundTargetDeclaresTheRequiredClickHandler() throws Exception {
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())) {
            ClassNode mixin=read(jar,"com/axial/cosmetics/mixin/CustomSubmenuClickSoundMixin");
            var targetAnnotation=mixin.invisibleAnnotations.stream().filter(a->a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")).findFirst().orElseThrow();
            @SuppressWarnings("unchecked") var targets=(List<Type>)value(targetAnnotation,"value");
            assertFalse(targets.isEmpty());
            int checked=0;
            for(var handler:mixin.methods) {
                var annotations=new ArrayList<AnnotationNode>();
                if(handler.visibleAnnotations!=null)annotations.addAll(handler.visibleAnnotations);
                if(handler.invisibleAnnotations!=null)annotations.addAll(handler.invisibleAnnotations);
                for(var a:annotations)if(a.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")) {
                    @SuppressWarnings("unchecked") var selectors=(List<String>)value(a,"method");
                    for(Type target:targets) {
                        ClassNode screen=read(jar,target.getInternalName());
                        for(String selector:selectors) {
                            assertTrue(screen.methods.stream().anyMatch(m->selector.equals(m.name)||selector.equals(m.name+m.desc)),
                                    target.getClassName()+" has no declared "+selector+"; required sound injection will crash at startup");
                            checked++;
                        }
                    }
                }
            }
            assertTrue(checked>=4);
        }
    }
    private static Object value(AnnotationNode annotation,String key){int i=annotation.values.indexOf(key);assertTrue(i>=0);return annotation.values.get(i+1);}
    private static ClassNode read(JarFile jar,String name) throws Exception {
        var entry=jar.getJarEntry(name+".class");assertNotNull(entry,name);
        try(var in=jar.getInputStream(entry)){var node=new ClassNode();new ClassReader(in).accept(node,0);return node;}
    }
}
