package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuickMenuLinksTest {
    @Test void absentLinksStayUnavailableInsteadOfOpeningInventedDestinations() {
        assertNull(QuickMenuLinks.parse(""));
        assertNull(QuickMenuLinks.parse(null));
        assertNull(QuickMenuLinks.parse("not a URL"));
    }
    @Test void onlyBrowserDestinationsAreAccepted() {
        assertEquals("example.com",QuickMenuLinks.parse("https://example.com/store").getHost());
        assertNull(QuickMenuLinks.parse("file:///private/tmp/example"));
        assertNull(QuickMenuLinks.parse("javascript:alert(1)"));
        assertNull(QuickMenuLinks.parse("https://name:password@example.com"));
    }
}
