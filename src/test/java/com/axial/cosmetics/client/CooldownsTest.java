package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class CooldownsTest {
    @TempDir Path directory;
    private byte[] fixture(String name)throws IOException{try(var in=getClass().getResourceAsStream("/cooldowns/combat-"+name+".bin")){assertNotNull(in);return in.readAllBytes();}}
    @Test void realServerPacketsStartRefreshAndClearTheCombatCountdown()throws Exception{
        var now=new AtomicLong(1_000_000_000L);var state=new CombatCooldownState(now::get);
        state.receive(fixture("start"));assertEquals(15000,state.remainingMillis());assertEquals(1,state.fraction());
        now.addAndGet(5_000_000_000L);assertEquals(10000,state.remainingMillis());assertEquals(2f/3,state.fraction(),.001);
        state.receive(fixture("start"));assertEquals(15000,state.remainingMillis(),"Another hit restarts the existing tag");
        state.receive(fixture("progress"));assertEquals(5000,state.remainingMillis());assertEquals(1f/3,state.fraction(),.001);
        state.receive(fixture("clear"));assertEquals(0,state.remainingMillis());assertEquals(0,state.fraction());
    }
    @Test void naturalExpiryAndConnectionResetNeverLeaveAStuckCombatTimer()throws Exception{
        var now=new AtomicLong();var state=new CombatCooldownState(now::get);state.receive(fixture("start"));
        now.set(15_000_000_001L);assertEquals(0,state.remainingMillis());assertEquals(0,state.fraction());
        state.receive(fixture("start"));state.reset();assertEquals(0,state.remainingMillis());assertEquals(0,state.fraction());
    }
    @Test void malformedUpdatesCannotReplaceTheLastGoodTag()throws Exception{
        var state=new CombatCooldownState(()->0);state.receive(fixture("start"));
        for(byte[] packet:new byte[][]{new byte[0],new byte[10],ByteBuffer.allocate(9).put((byte)1).putInt(16000).putInt(15000).array(),ByteBuffer.allocate(9).put((byte)1).putInt(-1).putInt(15000).array()}){
            assertThrows(IOException.class,()->state.receive(packet));assertEquals(15000,state.remainingMillis());
        }
    }
    @Test void masterAndIndividualSwitchesAreIndependentAndPersistAcrossReload()throws Exception{
        Path file=directory.resolve("cooldowns.json");
        Files.writeString(file,"{\"enabled\":false,\"pets\":true,\"combat\":true,\"x\":80,\"y\":90}");CooldownsConfig.load(file);
        assertFalse(CooldownsConfig.petsVisible());assertFalse(CooldownsConfig.combatVisible());assertTrue(CooldownsConfig.pets());assertTrue(CooldownsConfig.combat());
        Files.writeString(file,"{\"enabled\":true,\"pets\":false,\"combat\":true}");CooldownsConfig.load(file);
        assertFalse(CooldownsConfig.petsVisible());assertTrue(CooldownsConfig.combatVisible());
        CooldownsConfig.setPosition(80,90);CooldownsConfig.save(file);CooldownsConfig.load(directory.resolve("missing.json"));
        assertTrue(CooldownsConfig.petsVisible());assertTrue(CooldownsConfig.combatVisible());
        CooldownsConfig.load(file);assertFalse(CooldownsConfig.petsVisible());assertTrue(CooldownsConfig.combatVisible());assertEquals(80,CooldownsConfig.x(640));assertEquals(90,CooldownsConfig.y(360));
    }
    @Test void badConfigAndSmallWindowsKeepTheHudUsable()throws Exception{
        Path file=directory.resolve("cooldowns.json");Files.writeString(file,"{broken");CooldownsConfig.load(file);
        assertTrue(CooldownsConfig.enabled());assertTrue(CooldownsConfig.pets());assertTrue(CooldownsConfig.combat());
        CooldownsConfig.setPosition(999,999);assertEquals(264,CooldownsConfig.x(320));assertEquals(184,CooldownsConfig.y(240));
        assertEquals(0,CooldownsConfig.x(40));assertEquals(0,CooldownsConfig.y(40));
    }
    @Test void remainingArcRetreatsCounterclockwiseFromTopThroughLeftBottomThenRight(){
        assertTrue(CooldownRing.covered(2,28,.75f));assertFalse(CooldownRing.covered(2,28,.5f));
        assertTrue(CooldownRing.covered(28,53,.5f));assertFalse(CooldownRing.covered(28,53,.25f));
        assertTrue(CooldownRing.covered(53,27,.25f));assertFalse(CooldownRing.covered(53,27,.1f));
        assertTrue(CooldownRing.covered(28,1,.1f));assertFalse(CooldownRing.covered(28,1,0));
        assertEquals(0,CooldownRing.alpha(28,28));assertEquals(0,CooldownRing.alpha(0,0));
        for(int y=0;y<56;y++)for(int x=0;x<56;x++)if(CooldownRing.covered(x,y,.3f))assertTrue(CooldownRing.covered(x,y,.7f));
    }
}
