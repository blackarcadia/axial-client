package com.axial.cosmetics.mixin;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipInputStream;
import static org.junit.jupiter.api.Assertions.*;

class CooldownsMixinTest {
    @Test void packagedHudAndMoveCallbacksTargetExistingMethods()throws Exception{
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())){
            var json=JsonParser.parseString(new String(jar.getInputStream(jar.getJarEntry("axial-cosmetics.mixins.json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var hooks=new HashSet<String>();json.getAsJsonArray("client").forEach(v->hooks.add(v.getAsString()));
            for(String hook:List.of("InGameHudCooldownsMixin","AxialConfigScreenCooldownsMoveMixin")){assertTrue(hooks.contains(hook));assertNotNull(jar.getJarEntry("com/axial/cosmetics/mixin/"+hook+".class"));}
            var menu=nested(jar,"org/axial/axialutils/client/AxialConfigScreen");
            ClassNode mixin;try(var in=jar.getInputStream(jar.getJarEntry("com/axial/cosmetics/mixin/AxialConfigScreenCooldownsMoveMixin.class"))){mixin=read(in.readAllBytes());}
            int checked=0;
            for(var method:mixin.methods){
                if(method.visibleAnnotations==null)continue;
                for(var annotation:method.visibleAnnotations)if(annotation.desc.endsWith("/Inject;")){
                    @SuppressWarnings("unchecked")var selectors=(List<String>)value(annotation,"method");
                    for(String selector:selectors){assertTrue(menu.methods.stream().anyMatch(m->selector.equals(m.name)||selector.equals(m.name+m.desc)),selector);checked++;}
                }
            }
            assertEquals(5,checked);
        }
        ClassNode hud;try(var in=getClass().getResourceAsStream("/net/minecraft/client/gui/hud/InGameHud.class")){hud=read(Objects.requireNonNull(in).readAllBytes());}
        assertTrue(hud.methods.stream().anyMatch(m->m.name.equals("render")&&m.desc.equals("(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V")));
    }
    private static Object value(AnnotationNode a,String name){return a.values.get(a.values.indexOf(name)+1);}
    private static ClassNode read(byte[] bytes){var node=new ClassNode();new ClassReader(bytes).accept(node,0);return node;}
    private static ClassNode nested(JarFile jar,String name)throws Exception{
        var nested=jar.stream().filter(e->e.getName().contains("axialutils")&&e.getName().endsWith(".jar")).findFirst().orElseThrow();
        try(var zip=new ZipInputStream(jar.getInputStream(nested))){for(var entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry())if(entry.getName().equals(name+".class"))return read(zip.readAllBytes());}
        throw new AssertionError(name);
    }
}
