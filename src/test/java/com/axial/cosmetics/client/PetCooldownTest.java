package com.axial.cosmetics.client;

import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PetCooldownTest {
    private static final String ID="b60b2a81-ffed-40c3-a0c1-50e21117431b";
    private NbtCompound fields(){var n=new NbtCompound();n.putString("prisonscore:pet_id","snorlax");n.putString("prisonscore:pet_instance",ID);n.putLong("prisonscore:pet_cooldown_until",1_800_000_000_000L);return n;}
    @Test void readsActualPrisonsCorePersistentFieldsInBukkitAndDirectLayouts(){
        var expected=new PetCooldown.Pet(ID,1_800_000_000_000L);
        assertEquals(expected,PetCooldown.read(fields()));
        var wrapped=new NbtCompound();wrapped.put("PublicBukkitValues",fields());assertEquals(expected,PetCooldown.read(wrapped));
    }
    @Test void readyPetsAndUnrelatedOrMalformedHeadsHaveNoCooldownLabel(){
        assertNull(PetCooldown.read(new NbtCompound()));
        var n=fields();n.remove("prisonscore:pet_id");assertNull(PetCooldown.read(n));
        n=fields();n.putString("prisonscore:pet_instance","broken");assertNull(PetCooldown.read(n));
        n=fields();n.putLong("prisonscore:pet_cooldown_until",0);assertNull(PetCooldown.read(n));
        n=fields();n.putString("prisonscore:pet_cooldown_until","not a long");assertNull(PetCooldown.read(n));
    }
    @Test void compactCountdownRoundsUpAndNeverDisappearsBeforeReady(){
        assertEquals("10m",PetCooldown.label(600_000));assertEquals("10m",PetCooldown.label(599_999));
        assertEquals("9m",PetCooldown.label(540_000));assertEquals("1m",PetCooldown.label(60_000));
        assertEquals("60s",PetCooldown.label(59_999));assertEquals("59s",PetCooldown.label(59_000));
        assertEquals("1s",PetCooldown.label(1));assertEquals("",PetCooldown.label(0));assertEquals("",PetCooldown.label(-50));
        assertEquals("1h",PetCooldown.label(3_600_000));assertEquals("1d",PetCooldown.label(86_400_000));
    }
    @Test void nativeServerTimingOverridesLocalClockSkewAndDoesNotReappearAfterExpiry(){
        var clock=new PetCooldownClock();var pet=new PetCooldown.Pet(ID,50_000);
        assertEquals(600_000,clock.remaining(pet,1_000_000,600_000L));
        assertEquals(599_000,clock.remaining(pet,1_001_000,null));
        assertEquals(0,clock.remaining(pet,1_600_000,0L));assertEquals(0,clock.remaining(pet,1_600_100,null));
        pet=new PetCooldown.Pet(ID,9_999_999);assertEquals(50,clock.remaining(pet,2_000_000,50L));
        assertEquals(0,clock.remaining(pet,2_000_051,null));
    }
    @Test void reactivationAndServerSwitchDiscardPreviousTimerCorrections(){
        var clock=new PetCooldownClock();var pet=new PetCooldown.Pet(ID,10_000);
        assertEquals(500,clock.remaining(pet,1000,500L));assertEquals(400,clock.remaining(pet,1100,null));
        var reactivated=new PetCooldown.Pet(ID,9000);assertEquals(7900,clock.remaining(reactivated,1100,null));
        clock.reset();assertEquals(8900,clock.remaining(pet,1100,null));
    }
}
