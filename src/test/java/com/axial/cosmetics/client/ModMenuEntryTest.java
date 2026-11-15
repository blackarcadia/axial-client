package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class ModMenuEntryTest {
    @Test void filtersUseLiveStateIncludingChangesMadeInSettings() {
        var state=new AtomicBoolean(false);
        var entry=new ModMenuEntry("Armor HUD","Armor and durability",ModMenuEntry.Category.HUD,"shirt",state::get,()->state.set(!state.get()),()->{});
        assertTrue(entry.matches(ModMenuEntry.Category.ALL,"ARMOR",false));
        assertTrue(entry.matches(ModMenuEntry.Category.HUD,"durability",false));
        assertFalse(entry.matches(ModMenuEntry.Category.WORLD,"",false));
        assertFalse(entry.matches(ModMenuEntry.Category.ALL,"",true));
        entry.toggle().run();
        assertTrue(entry.matches(ModMenuEntry.Category.ALL,"",true));
        state.set(false);
        assertFalse(entry.matches(ModMenuEntry.Category.ALL,"",true));
    }
    @Test void gangBrowserIsAFullCardMenuActionWithoutAnEnabledState() {
        var entry=new ModMenuEntry("Gangs","Server gangs and rankings",ModMenuEntry.Category.WORLD,"account",null,null,()->{});
        assertTrue(entry.opensMenu());assertNull(entry.toggle());assertFalse(entry.isEnabled());assertNotNull(entry.configure());
        assertTrue(entry.matches(ModMenuEntry.Category.ALL,"gang",false));
    }
    @Test void configurationToolsAreNeverPresentedAsEnabledMods() {
        var entry=new ModMenuEntry("Item Scaler","Held item scale",ModMenuEntry.Category.VISUALS,"scale",null,null,()->{});
        assertTrue(entry.matches(ModMenuEntry.Category.ALL,"item",false));
        assertFalse(entry.matches(ModMenuEntry.Category.ALL,"",true));
        assertNull(entry.toggle());
        assertNotNull(entry.configure());
    }
    @Test void replayViewerIsAFullCardActionAndItsTitleIconFitsBesideMusicControls() {
        var entry=new ModMenuEntry("Replay Mod","Record gameplay and browse saved replays",ModMenuEntry.Category.WORLD,"replay",null,null,()->{});
        assertTrue(entry.opensMenu());assertNull(entry.toggle());assertFalse(entry.isEnabled());
        assertTrue(entry.matches(ModMenuEntry.Category.WORLD,"saved replay",false));
        assertFalse(entry.matches(ModMenuEntry.Category.ALL,"",true));
        for(int width:new int[]{260,320,427,640,960,1280}) {
            int musicX=Math.max(20,width-20-32-8-112);
            assertTrue(ReplayViewerButton.x(width)>=20);
            assertTrue(ReplayViewerButton.x(width)+24<=musicX-8);
        }
    }
}
