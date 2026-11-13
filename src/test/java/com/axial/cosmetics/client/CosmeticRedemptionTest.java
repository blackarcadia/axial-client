package com.axial.cosmetics.client;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CosmeticRedemptionTest {
    @Test void emptyWardrobeAllowsRedemptionButDoesNotGrantEquipment() {
        var account=UUID.fromString("11223344-1234-4234-8234-123456789012");
        assertTrue(WardrobeAccess.allows(account,null));assertTrue(WardrobeAccess.allows(account,account));
        assertFalse(WardrobeAccess.allows(null,null));assertFalse(WardrobeAccess.allows(account,UUID.randomUUID()));
        assertTrue(WardrobeItem.owned(account,WardrobeCategory.ALL).isEmpty());
    }
    @Test void responseMustNameAKnownCosmeticPresentInTheVerifiedGrantList() {
        var r=CosmeticRedemptionService.parse(JsonParser.parseString("{\"revision\":10,\"uuid\":\"11223344-1234-4234-8234-123456789012\",\"cosmetic\":\"magenta_city\",\"cosmetics\":[\"magenta_city\",\"ribcage\"]}").getAsJsonObject());
        assertEquals(WardrobeCape.MAGENTA_CITY,r.item().cape());assertEquals(Set.of("magenta_city","ribcage"),r.owned());
        assertThrows(IllegalArgumentException.class,()->CosmeticRedemptionService.parse(JsonParser.parseString("{\"revision\":10,\"uuid\":\"11223344-1234-4234-8234-123456789012\",\"cosmetic\":\"magenta_city\",\"cosmetics\":[]}").getAsJsonObject()));
    }
    @Test void authoritativeUnlockAppearsImmediatelyWithoutChangingOtherAccounts() throws Exception {
        var field=CosmeticAssignments.class.getDeclaredField("grants");field.setAccessible(true);var original=field.get(null);
        var account=UUID.randomUUID();var other=UUID.randomUUID();
        try{
            field.set(null,Map.of(other,Set.of("ribcage")));
            CosmeticAssignments.acceptRedemption(account,Set.of("magenta_city"),10);
            assertTrue(WardrobeCape.MAGENTA_CITY.ownedBy(account));assertTrue(WardrobeAccessory.RIBCAGE.ownedBy(other));
            assertFalse(WardrobeAccessory.RIBCAGE.ownedBy(account));
        }finally{field.set(null,original);}
    }
    @Test void revealThenImpactThenFadeHaveStableBoundedTimings() {
        assertEquals(0,CosmeticUnlockAnimation.appear(0));assertEquals(1,CosmeticUnlockAnimation.appear(.35));
        assertEquals(0,CosmeticUnlockAnimation.stamp(.40));assertEquals(1,CosmeticUnlockAnimation.stamp(.63));
        assertEquals(0,CosmeticUnlockAnimation.fade(2));assertEquals(1,CosmeticUnlockAnimation.fade(2.85));
        assertFalse(CosmeticUnlockAnimation.finished(2.7));assertTrue(CosmeticUnlockAnimation.finished(2.85));
        for(double t=0;t<3;t+=.01){assertTrue(Float.isFinite(CosmeticUnlockAnimation.stampScale(t)));assertTrue(Math.abs(CosmeticUnlockAnimation.shake(t))<3.01);}
    }
}
