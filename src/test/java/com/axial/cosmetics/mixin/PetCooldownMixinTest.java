package com.axial.cosmetics.mixin;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

class PetCooldownMixinTest {
    @Test void itemOverlayHookMatchesTheSharedVanillaOverlayAndRunsAfterCooldownShading()throws Exception{
        var target=read("net/minecraft/client/gui/DrawContext");var mixin=read("com/axial/cosmetics/mixin/DrawContextPetCooldownMixin");
        var handler=mixin.methods.stream().filter(m->m.name.equals("axial$petCooldown")).findFirst().orElseThrow();
        var annotation=handler.visibleAnnotations.stream().filter(a->a.desc.endsWith("/Inject;")).findFirst().orElseThrow();
        @SuppressWarnings("unchecked")var selectors=(List<String>)value(annotation,"method");assertEquals(1,selectors.size());
        var method=target.methods.stream().filter(m->(m.name+m.desc).equals(selectors.getFirst())).findFirst().orElseThrow();
        assertTrue(Arrays.stream(method.instructions.toArray()).anyMatch(n->n instanceof MethodInsnNode call&&call.name.equals("drawCooldownProgress")));
        var at=(AnnotationNode)((List<?>)value(annotation,"at")).getFirst();assertEquals("TAIL",value(at,"value"));
    }
    @Test void nativeCooldownAccessorFieldsMatchCurrentMinecraft()throws Exception{
        var manager=read("net/minecraft/entity/player/ItemCooldownManager");
        assertTrue(manager.fields.stream().anyMatch(f->f.name.equals("entries")&&f.desc.equals("Ljava/util/Map;")));
        assertTrue(manager.fields.stream().anyMatch(f->f.name.equals("tick")&&f.desc.equals("I")));
        var entry=read("net/minecraft/entity/player/ItemCooldownManager$Entry");assertTrue(entry.fields.stream().anyMatch(f->f.name.equals("endTick")&&f.desc.equals("I")));
    }
    @Test void packagedModRegistersTheOverlayAndRemapsThePrivateEntryTarget()throws Exception{
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())){
            var json=JsonParser.parseString(new String(jar.getInputStream(jar.getJarEntry("axial-cosmetics.mixins.json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var hooks=new HashSet<String>();json.getAsJsonArray("client").forEach(v->hooks.add(v.getAsString()));
            for(String hook:List.of("DrawContextPetCooldownMixin","ItemCooldownManagerAccessor","ItemCooldownEntryAccessor")){assertTrue(hooks.contains(hook));assertNotNull(jar.getJarEntry("com/axial/cosmetics/mixin/"+hook+".class"));}
            var outer=read(jar,"com/axial/cosmetics/mixin/ItemCooldownManagerAccessor");var inner=read(jar,"com/axial/cosmetics/mixin/ItemCooldownEntryAccessor");
            var outerAnnotation=outer.invisibleAnnotations.stream().filter(a->a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
            var innerAnnotation=inner.invisibleAnnotations.stream().filter(a->a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
            String manager=((Type)((List<?>)value(outerAnnotation,"value")).getFirst()).getClassName();
            String entry=(String)((List<?>)value(innerAnnotation,"targets")).getFirst();
            assertTrue(entry.replace('/','.').startsWith(manager+"$"),"Private cooldown entry must use the packaged Minecraft mappings: "+entry);
        }
    }
    private static Object value(AnnotationNode a,String key){int i=a.values.indexOf(key);assertTrue(i>=0,key);return a.values.get(i+1);}
    private static ClassNode read(String name)throws Exception{try(var in=PetCooldownMixinTest.class.getResourceAsStream("/"+name+".class")){assertNotNull(in,name);var node=new ClassNode();new ClassReader(in).accept(node,0);return node;}}
    private static ClassNode read(JarFile jar,String name)throws Exception{var entry=jar.getJarEntry(name+".class");assertNotNull(entry,name);try(var in=jar.getInputStream(entry)){var node=new ClassNode();new ClassReader(in).accept(node,0);return node;}}
}
