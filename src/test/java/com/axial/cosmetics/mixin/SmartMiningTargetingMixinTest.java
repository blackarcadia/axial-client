package com.axial.cosmetics.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

class SmartMiningTargetingMixinTest {
    @Test void targetingHooksOnlyReplaceTheEntityPredicateInBothNativeAimPaths()throws Exception{
        var mixin=resource("com/axial/cosmetics/mixin/ClientPlayerSmartMiningTargetingMixin");
        var player=resource("net/minecraft/client/network/ClientPlayerEntity");int checked=0;
        for(var handler:mixin.methods){
            if(handler.visibleAnnotations==null)continue;
            for(var annotation:handler.visibleAnnotations)if(annotation.desc.endsWith("/ModifyArg;")){
                @SuppressWarnings("unchecked")var selectors=(List<String>)value(annotation,"method");
                var at=(AnnotationNode)((List<?>)value(annotation,"at")).getFirst();String selector=(String)value(at,"target");int index=(Integer)value(annotation,"index");
                int split=selector.indexOf(';');String owner=selector.substring(1,split),method=selector.substring(split+1);
                for(String name:selectors){
                    var target=player.methods.stream().filter(m->name.equals(m.name)).findFirst().orElseThrow();
                    assertEquals(target.access&Opcodes.ACC_STATIC,handler.access&Opcodes.ACC_STATIC);
                    var calls=new ArrayList<MethodInsnNode>();for(var n:target.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(owner)&&method.equals(call.name+call.desc))calls.add(call);
                    assertEquals(1,calls.size(),selector);Type predicate=Type.getType("Ljava/util/function/Predicate;");
                    assertEquals(predicate,Type.getArgumentTypes(calls.getFirst().desc)[index]);
                    assertArrayEquals(new Type[]{predicate},Type.getArgumentTypes(handler.desc));assertEquals(predicate,Type.getReturnType(handler.desc));checked++;
                }
            }
        }
        assertEquals(2,checked);
    }
    @Test void packagedFilterUsesPlayerTypeAndExistingFadeEligibility()throws Exception{
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())){
            ClassNode helper;try(var in=jar.getInputStream(jar.getJarEntry("com/axial/cosmetics/client/SmartMiningTargeting.class"))){helper=new ClassNode();new ClassReader(in).accept(helper,0);}
            boolean players=false,eligibility=false,vanillaPredicate=false;
            // Type/method names in the packaged mod are intermediary; the library helper keeps its API name.
            for(var method:helper.methods)for(var n:method.instructions){
                if(n instanceof TypeInsnNode type&&type.getOpcode()==Opcodes.INSTANCEOF&&type.desc.equals("net/minecraft/class_1657"))players=true;
                if(n instanceof MethodInsnNode call&&call.owner.equals("org/axial/axialutils/client/AxialVisualEffects")&&call.name.equals("shouldFadeLivingEntity"))eligibility=true;
                if(n instanceof MethodInsnNode call&&call.owner.equals("java/util/function/Predicate")&&call.name.equals("and"))vanillaPredicate=true;
            }
            assertTrue(players);assertTrue(eligibility);assertTrue(vanillaPredicate);
            try(var in=jar.getInputStream(jar.getJarEntry("axial-cosmetics.mixins.json"))){assertTrue(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).contains("ClientPlayerSmartMiningTargetingMixin"));}
        }
    }
    private static Object value(AnnotationNode node,String name){return node.values.get(node.values.indexOf(name)+1);}
    private static ClassNode resource(String name)throws Exception{try(var in=SmartMiningTargetingMixinTest.class.getClassLoader().getResourceAsStream(name+".class")){assertNotNull(in,name);var node=new ClassNode();new ClassReader(in).accept(node,0);return node;}}
}
