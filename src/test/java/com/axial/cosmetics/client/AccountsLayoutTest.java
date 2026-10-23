package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountsLayoutTest {
    @Test void controlsStayInsideThePanelAcrossSupportedGuiSizes() {
        for (int width : new int[]{220, 240, 280, 320, 360, 427, 484, 640, 854, 1280}) {
            for (int height : new int[]{160, 180, 200, 240, 270, 360, 480, 720}) {
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
                inside(layout.close, panel);
                inside(layout.addAccount, panel);
                inside(layout.done, panel);
                assertTrue(layout.addAccount.right() + 8 <= layout.done.x());
                assertTrue(layout.addAccount.y() >= layout.content.bottom());
                assertTrue(panel.width() <= width * 0.83);
                assertTrue(panel.height() <= height * 0.81);
                for (int row = 0; row < layout.pageSize; row++) {
                    var bounds = layout.row(row);
                    inside(layout.rowAction(bounds), bounds);
                    inside(layout.rowMenu(bounds), bounds);
                    inside(layout.popover(bounds), layout.list);
                    assertTrue(layout.rowAction(bounds).right() < layout.rowMenu(bounds).x());
                }
                if (layout.preview != null) {
                    inside(layout.preview, layout.content);
                    assertEquals(layout.list.right(), layout.preview.x());
                    inside(layout.skin, layout.preview);
                    inside(layout.previewAction, layout.preview);
                    assertTrue(layout.skin.height() >= 88);
                    assertTrue(layout.skin.bottom() < layout.preview.bottom() - 73);
                }
            }
        }
    }

    @Test void normalWindowShowsThreeCardsAndSkinPreview() {
        var layout = new AccountsLayout(854, 480);
        assertNotNull(layout.preview);
        assertFalse(layout.compact);
        assertEquals(3, layout.pageSize);
        assertEquals(560, layout.panel.width());
        assertEquals(360, layout.panel.height());
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
