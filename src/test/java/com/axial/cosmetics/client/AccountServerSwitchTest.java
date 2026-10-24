package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountServerSwitchTest {
    @Test void rejoinUsesTheNormalConnectScreenWithNoPreviousAccountCookies()throws Exception{
        var adapter=read("com/axial/cosmetics/client/AccountServerSwitch$1");
        var reconnect=adapter.methods.stream().filter(m->m.name.equals("reconnect")).findFirst().orElseThrow();
        MethodInsnNode connect=null;
        for(var instruction:reconnect.instructions)if(instruction instanceof MethodInsnNode call&&call.owner.equals("net/minecraft/client/gui/screen/multiplayer/ConnectScreen")&&call.name.equals("connect"))connect=call;
        assertNotNull(connect);assertEquals(Opcodes.ACONST_NULL,previous(connect).getOpcode(),"Old session transfer cookies must never follow the new account");
        assertEquals(Opcodes.ICONST_0,previous(previous(connect)).getOpcode(),"An account switch must be a normal fresh login");
        var nativeScreen=read(connect.owner);String descriptor=connect.desc;
        assertTrue(nativeScreen.methods.stream().anyMatch(m->m.name.equals("connect")&&m.desc.equals(descriptor)));
    }
    @Test void accountSessionMutationStillRejectsLiveConnections()throws Exception{
        var mixin=read("com/axial/cosmetics/mixin/MinecraftClientAccountMixin");
        var apply=mixin.methods.stream().filter(m->m.name.equals("axial$setAccount")).findFirst().orElseThrow();
        assertTrue(java.util.Arrays.stream(apply.instructions.toArray()).anyMatch(n->n instanceof MethodInsnNode call&&call.name.equals("getNetworkHandler")));
        assertTrue(java.util.Arrays.stream(apply.instructions.toArray()).anyMatch(n->n instanceof TypeInsnNode type&&type.desc.equals("java/lang/IllegalStateException")));
    }
    private static AbstractInsnNode previous(AbstractInsnNode node){do{node=node.getPrevious();}while(node.getOpcode()<0);return node;}
    private static ClassNode read(String name)throws Exception{try(var in=AccountServerSwitchTest.class.getResourceAsStream("/"+name+".class")){assertNotNull(in,name);var node=new ClassNode();new ClassReader(in).accept(node,0);return node;}}
}
