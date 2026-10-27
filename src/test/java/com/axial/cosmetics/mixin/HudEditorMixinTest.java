package com.axial.cosmetics.mixin;

import com.google.gson.JsonParser;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class HudEditorMixinTest {
    @Test void packagedEditorCallbacksExistAndPrecedeEveryLegacyDragHandler() throws Exception {
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())) {
            var json=JsonParser.parseString(new String(jar.getInputStream(jar.getJarEntry("axial-cosmetics.mixins.json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(json.getAsJsonArray("client").asList().stream().anyMatch(v->v.getAsString().equals("AxialConfigScreenHudEditorMixin")));
            var editor=read(jar.getInputStream(jar.getJarEntry("com/axial/cosmetics/mixin/AxialConfigScreenHudEditorMixin.class")).readAllBytes());
            var annotation=editor.invisibleAnnotations.stream().filter(a->a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
            assertEquals(1400,value(annotation,"priority"));
            var screen=nested(jar,"org/axial/axialutils/client/AxialConfigScreen");
            int hooks=0;
            for(var method:editor.methods)if(method.visibleAnnotations!=null)for(var hook:method.visibleAnnotations)
                if(hook.desc.endsWith("/Inject;"))for(Object selector:(List<?>)value(hook,"method")) {
                    assertTrue(screen.methods.stream().anyMatch(m->m.name.equals(selector)),"Missing editor target: "+selector);hooks++;
                }
            assertEquals(9,hooks);
            var begin=editor.methods.stream().filter(m->m.name.equals("axial$beginHudDrag")).findFirst().orElseThrow();
            assertTrue(begin.instructions.iterator().hasNext());
            assertTrue(java.util.stream.StreamSupport.stream(begin.instructions.spliterator(),false)
                    .anyMatch(n->n instanceof MethodInsnNode call&&call.name.equals("setReturnValue")),"Shared picker must prevent legacy handlers from starting a second drag");
        }
    }
    private static Object value(AnnotationNode annotation,String key){return annotation.values.get(annotation.values.indexOf(key)+1);}
    private static ClassNode read(byte[] bytes){var node=new ClassNode();new ClassReader(bytes).accept(node,0);return node;}
    private static ClassNode nested(JarFile jar,String name) throws Exception {
        var entry=jar.stream().filter(e->e.getName().contains("axialutils")&&e.getName().endsWith(".jar")).findFirst().orElseThrow();
        try(var zip=new ZipInputStream(jar.getInputStream(entry))) {
            for(var file=zip.getNextEntry();file!=null;file=zip.getNextEntry())if(file.getName().equals(name+".class"))return read(zip.readAllBytes());
        }
        throw new AssertionError(name);
    }
}
