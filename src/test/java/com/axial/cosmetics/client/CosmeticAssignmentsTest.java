package com.axial.cosmetics.client;

import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CosmeticAssignmentsTest {
    @Test void acceptsNewGrantsEmptyRevocationsAndUnknownFutureCosmeticIds() throws Exception {
        UUID recipient=UUID.fromString("12345678-1234-4234-8234-123456789012");
        var snapshot=CosmeticAssignments.parse("{\"version\":1,\"players\":{\""+recipient+"\":[\"staff\",\"future_cape\"]}}");
        assertEquals(java.util.Set.of("staff"),snapshot.get(recipient));
        assertEquals(java.util.Set.of(),CosmeticAssignments.parse("{\"version\":1,\"players\":{\""+recipient+"\":[]}}").get(recipient));
        assertTrue(CosmeticAssignments.parse("{\"version\":1,\"players\":{}}").isEmpty());
        assertThrows(UnsupportedOperationException.class,()->snapshot.clear());
        assertThrows(UnsupportedOperationException.class,()->snapshot.get(recipient).clear());
    }
    @Test void malformedSnapshotsCannotAccidentallyRevokeValidCachedAssignments() {
        for(String json:new String[]{"null","[]","{}","{\"version\":2,\"players\":{}}","{\"version\":1.5,\"players\":{}}",
                "{\"version\":1,\"players\":{\"bad\":[\"staff\"]}}","{\"version\":1,\"players\":{\"12345678-1234-4234-8234-123456789012\":[7]}}"})
            assertThrows(IOException.class,()->CosmeticAssignments.parse(json),json);
    }
    @Test void bundledGrantsRemainAvailableBeforeFirstSuccessfulSync() {
        assertTrue(CosmeticAssignments.owns(CreatorCosmetics.OWNER,"gold_owner",true));
        assertFalse(CosmeticAssignments.owns(new UUID(0,1),"staff",false));
    }
}
