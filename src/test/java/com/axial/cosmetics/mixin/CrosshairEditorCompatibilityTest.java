package com.axial.cosmetics.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;
import java.nio.file.Path;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

class CrosshairEditorCompatibilityTest {
    @Test void customCrosshairWrapsOnlyTheSpriteAndPreservesRemainingHudWork()throws Exception{
        var mixin=resource("com/axial/cosmetics/mixin/TitleOverlayCrosshairMixin");
        var handler=mixin.methods.stream().filter(m->m.name.equals("axial_cosmetics$renderCustomCrosshair")).findFirst().orElseThrow();
        var wrap=handler.visibleAnnotations.stream().filter(a->a.desc.endsWith("/WrapOperation;")).findFirst().orElseThrow();
        AnnotationNode at=(AnnotationNode)((List<?>)value(wrap,"at")).getFirst();String selector=(String)value(at,"target");
        assertEquals(0,value(at,"ordinal"));
        int separator=selector.indexOf(';');String owner=selector.substring(1,separator),target=selector.substring(separator+1);
        var hud=resource("net/minecraft/client/gui/hud/InGameHud");var method=hud.methods.stream().filter(m->m.name.equals("renderCrosshair")).findFirst().orElseThrow();
        var calls=new ArrayList<MethodInsnNode>();for(var instruction:method.instructions)if(instruction instanceof MethodInsnNode call&&call.owner.equals(owner)&&target.equals(call.name+call.desc))calls.add(call);
        assertTrue(calls.size()>1,"Attack indicator sprites should still follow the replaced crosshair sprite");
        var expected=new ArrayList<Type>();expected.add(Type.getObjectType(owner));expected.addAll(List.of(Type.getArgumentTypes(calls.getFirst().desc)));expected.add(Type.getObjectType("com/llamalad7/mixinextras/injector/wrapoperation/Operation"));
        assertArrayEquals(expected.toArray(Type[]::new),Type.getArgumentTypes(handler.desc));
        boolean delegates=false;for(var n:handler.instructions)if(n instanceof MethodInsnNode call){assertFalse(call.name.equals("cancel"));if(call.owner.endsWith("/Operation")&&call.name.equals("call"))delegates=true;}
        assertTrue(delegates,"Disabled custom crosshairs must delegate to the vanilla sprite");
    }
    @Test void packagedEditorIsExemptFromGenericButtonSkinAndRoutedFromTheModMenu()throws Exception{
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())){
            var skin=read(jar,"com/axial/cosmetics/mixin/InGameButtonSkinMixin");boolean exempt=false;
            for(var method:skin.methods)for(var n:method.instructions)if(n instanceof TypeInsnNode type&&type.getOpcode()==Opcodes.INSTANCEOF&&type.desc.endsWith("/CrosshairSettingsScreen"))exempt=true;
            assertTrue(exempt);
            var binding=read(jar,"com/axial/cosmetics/client/ModFeatureSettings");boolean opens=false;
            for(var method:binding.methods)if(method.name.equals("create"))for(var n:method.instructions)if(n instanceof TypeInsnNode type&&type.getOpcode()==Opcodes.NEW&&type.desc.endsWith("/CrosshairSettingsScreen"))opens=true;
            assertTrue(opens);
        }
    }
    private static Object value(AnnotationNode node,String key){int i=node.values.indexOf(key);assertTrue(i>=0,key);return node.values.get(i+1);}
    private static ClassNode resource(String path)throws Exception{try(var in=CrosshairEditorCompatibilityTest.class.getClassLoader().getResourceAsStream(path+".class")){assertNotNull(in,path);var node=new ClassNode();new ClassReader(in).accept(node,0);return node;}}
    private static ClassNode read(JarFile jar,String path)throws Exception{try(var in=jar.getInputStream(jar.getJarEntry(path+".class"))){var node=new ClassNode();new ClassReader(in).accept(node,0);return node;}}
}
