package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountsLayoutTest {
    @Test void controlsStayInsideThePanelAcrossSupportedGuiSizes() {
        for (int width : new int[]{320, 360, 427, 484, 640, 854, 1280}) {
            for (int height : new int[]{240, 270, 360, 480, 720}) {
                var layout = new AccountsLayout(width, height);
                var panel = layout.panel;
                assertTrue(panel.x() >= 0 && panel.y() >= 0);
                assertTrue(panel.right() <= width && panel.bottom() <= height);
                inside(layout.content, panel);
                inside(layout.list, layout.content);
                inside(layout.search, layout.list);
                for (int row = 0; row < layout.pageSize; row++) {
                    inside(layout.row(row), layout.list);
                    assertTrue(layout.row(row).bottom() <= layout.list.bottom() - 18);
                }
                assertTrue(layout.footerY + 8 + (layout.compact ? 26 : 30) <= panel.bottom());
                if (layout.preview != null) {
                    inside(layout.preview, layout.content);
                    assertEquals(layout.list.right(), layout.preview.x());
                    assertTrue(layout.preview.height() - 100 >= 70);
                }
            }
        }
    }

    @Test void normalWindowShowsThreeCardsAndSkinPreview() {
        var layout = new AccountsLayout(854, 480);
        assertNotNull(layout.preview);
        assertFalse(layout.compact);
        assertEquals(3, layout.pageSize);
    }

    @Test void smallGuiKeepsAccountsUsableWithoutOverlappingPreview() {
        var layout = new AccountsLayout(320, 240);
        assertTrue(layout.compact);
        assertNull(layout.preview);
        assertEquals(1, layout.pageSize);
    }

    private static void inside(AccountsLayout.Rect child, AccountsLayout.Rect parent) {
        assertTrue(child.width() > 0 && child.height() > 0);
        assertTrue(child.x() >= parent.x() && child.y() >= parent.y());
        assertTrue(child.right() <= parent.right() && child.bottom() <= parent.bottom());
    }
}
