package com.axial.cosmetics.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

/** Fail before shipping if a Minecraft/Sodium update breaks any required render hook. */
class PerformanceRenderCompatibilityTest {
    @Test void renderHooksResolveToExactMinecraftMethods()throws Exception{
        Map<String,String> hooks=Map.of(
            "PerformanceDroppedItemsMixin","net/minecraft/client/render/entity/EntityRenderManager",
            "PerformanceLiquidsMixin","net/minecraft/client/render/block/BlockRenderManager",
            "PerformanceSpawnerMobsMixin","net/minecraft/client/render/block/entity/TrialSpawnerBlockEntityRenderer",
            "PerformanceParticlesMixin","net/minecraft/client/particle/ParticleManager",
            "PerformanceWeatherMixin","net/minecraft/client/render/WeatherRendering");
        for(var hook:hooks.entrySet())check(readResource("com/axial/cosmetics/mixin/"+hook.getKey()),readResource(hook.getValue()));
    }
    @Test void sodiumFluidHookExistsInBundledRenderer()throws Exception{
        try(var jar=new JarFile("src/main/resources/sodium-fabric-0.8.13+mc1.21.11.jar")){
            var target=new ClassNode();try(var in=jar.getInputStream(jar.getJarEntry("net/caffeinemc/mods/sodium/fabric/render/FluidRendererImpl.class"))){new ClassReader(in).accept(target,0);}
            check(readResource("com/axial/cosmetics/mixin/PerformanceSodiumLiquidsMixin"),target);
        }
    }
    private static ClassNode readResource(String name)throws Exception{
        try(var in=PerformanceRenderCompatibilityTest.class.getClassLoader().getResourceAsStream(name+".class")){
            assertNotNull(in,name);var node=new ClassNode();new ClassReader(in).accept(node,0);return node;
        }
    }
    private static void check(ClassNode mixin,ClassNode target){
        int count=0;
        for(var handler:mixin.methods){
            if(handler.visibleAnnotations==null)continue;
            for(var annotation:handler.visibleAnnotations)if(annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")){
                count++;
                @SuppressWarnings("unchecked") List<String> selectors=(List<String>)annotation.values.get(annotation.values.indexOf("method")+1);
                for(String selector:selectors){
                    var matching=target.methods.stream().filter(m->selector.equals(m.name)||selector.equals(m.name+m.desc)).toList();
                    assertEquals(1,matching.size(),mixin.name+" -> "+selector);
                    var method=matching.getFirst();assertEquals(method.access&Opcodes.ACC_STATIC,handler.access&Opcodes.ACC_STATIC);
                    var args=Type.getArgumentTypes(handler.desc);var targetArgs=Type.getArgumentTypes(method.desc);
                    if(args.length>1)assertArrayEquals(targetArgs,Arrays.copyOf(args,args.length-1),selector);
                    assertEquals(Boolean.TRUE,annotation.values.get(annotation.values.indexOf("cancellable")+1));
                }
            }
        }
        assertTrue(count>0,mixin.name);
    }
}
