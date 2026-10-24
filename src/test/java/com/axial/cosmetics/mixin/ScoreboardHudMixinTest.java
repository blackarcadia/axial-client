package com.axial.cosmetics.mixin;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

class ScoreboardHudMixinTest {
    private static final String SIDEBAR="renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V";
    @Test void vanillaTitleHookHasTheMeasuredBottomLocalUsedByTheBodyBackground()throws Exception{
        var hud=read("net/minecraft/client/gui/hud/InGameHud");
        var method=hud.methods.stream().filter(m->(m.name+m.desc).equals(SIDEBAR)).findFirst().orElseThrow();
        var calls=new ArrayList<Integer>();
        for(int i=0;i<method.instructions.size();i++)if(method.instructions.get(i) instanceof MethodInsnNode call&&call.owner.equals("net/minecraft/client/gui/DrawContext")&&call.name.equals("fill")&&call.desc.equals("(IIIII)V"))calls.add(i);
        assertEquals(2,calls.size(),"The hook requires vanilla's title and body backgrounds");
        var frames=new Analyzer<>(new BasicInterpreter()).analyze(hud.name,method);
        assertEquals(BasicValue.INT_VALUE,frames[calls.getFirst()].getLocal(13),"Sidebar bottom must be available when the first background is drawn");
        var sources=new Analyzer<>(new SourceInterpreter()).analyze(hud.name,method);
        var second=sources[calls.get(1)];var bottom=second.getStack(second.getStackSize()-2);
        assertTrue(bottom.insns.stream().anyMatch(n->n instanceof VarInsnNode v&&v.getOpcode()==Opcodes.ILOAD&&v.var==13),"Captured bottom must match the body background, including all score rows");
    }
    @Test void renderHookSelectorsMatchCurrentMinecraftAndEditorCallbacksMatchBundledMenu()throws Exception{
        var hud=read("net/minecraft/client/gui/hud/InGameHud");
        var mixin=read("com/axial/cosmetics/mixin/InGameHudScoreboardPositionMixin");int checked=0;
        for(var method:mixin.methods)for(var a:annotations(method))if(a.desc.endsWith("/WrapMethod;")||a.desc.endsWith("/WrapOperation;")){
            @SuppressWarnings("unchecked")var selectors=(List<String>)value(a,"method");
            for(String selector:selectors){assertTrue(hud.methods.stream().anyMatch(m->selector.equals(m.name+m.desc)),selector);checked++;}
        }
        assertEquals(2,checked);
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())){
            var screen=readBundled(jar,"org/axial/axialutils/client/AxialConfigScreen");var editor=read(jar,"com/axial/cosmetics/mixin/AxialConfigScreenScoreboardMoveMixin");
            checked=0;
            for(var method:editor.methods)for(var a:annotations(method))if(a.desc.endsWith("/Inject;")){
                @SuppressWarnings("unchecked")var selectors=(List<String>)value(a,"method");
                for(String selector:selectors){assertTrue(screen.methods.stream().anyMatch(m->selector.equals(m.name)||selector.equals(m.name+m.desc)),"Missing required editor injection: "+selector);checked++;}
            }
            assertEquals(5,checked);
        }
    }
    @Test void builtModRegistersAllRequiredScoreboardHooks()throws Exception{
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())){
            var json=JsonParser.parseString(new String(jar.getInputStream(jar.getJarEntry("axial-cosmetics.mixins.json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var hooks=new HashSet<String>();json.getAsJsonArray("client").forEach(v->hooks.add(v.getAsString()));
            for(String hook:List.of("InGameHudScoreboardInvoker","InGameHudScoreboardPositionMixin","AxialConfigScreenScoreboardMoveMixin")){
                assertTrue(hooks.contains(hook));assertNotNull(jar.getJarEntry("com/axial/cosmetics/mixin/"+hook+".class"));
            }
        }
    }
    private static List<AnnotationNode> annotations(MethodNode method){var result=new ArrayList<AnnotationNode>();if(method.visibleAnnotations!=null)result.addAll(method.visibleAnnotations);if(method.invisibleAnnotations!=null)result.addAll(method.invisibleAnnotations);return result;}
    private static Object value(AnnotationNode a,String key){int i=a.values.indexOf(key);assertTrue(i>=0);return a.values.get(i+1);}
    private static ClassNode read(String name)throws Exception{try(var in=ScoreboardHudMixinTest.class.getResourceAsStream("/"+name+".class")){assertNotNull(in,name);var node=new ClassNode();new ClassReader(in).accept(node,0);return node;}}
    private static ClassNode readBundled(JarFile jar,String name)throws Exception{
        var nested=jar.stream().filter(e->e.getName().contains("axialutils")&&e.getName().endsWith(".jar")).findFirst().orElseThrow();
        try(var zip=new java.util.zip.ZipInputStream(jar.getInputStream(nested))){
            for(var entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry())if(entry.getName().equals(name+".class")){
                var node=new ClassNode();new ClassReader(zip.readAllBytes()).accept(node,0);return node;
            }
        }
        throw new AssertionError("Bundled menu class missing: "+name);
    }
    private static ClassNode read(JarFile jar,String name)throws Exception{var entry=jar.getJarEntry(name+".class");assertNotNull(entry,name);try(var in=jar.getInputStream(entry)){var node=new ClassNode();new ClassReader(in).accept(node,0);return node;}}
}
