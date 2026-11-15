package com.axial.cosmetics.client;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WardrobeCapeSelectionTest {
    private void withGrants(java.util.function.Consumer<UUID> scenario)throws Exception {
        var uuid=UUID.randomUUID();var field=CosmeticAssignments.class.getDeclaredField("grants");field.setAccessible(true);var previous=field.get(null);
        try{field.set(null,Map.of(uuid,Set.of("purrr","magenta_city","ribcage","pookles","black_sneakers","swim_bands","gold_spark")));scenario.accept(uuid);}
        finally{field.set(null,previous);}
    }
    @Test void selectingHiddenCurrentCapeRestoresItAndRemovesOnlyBlockingBodywear()throws Exception {
        withGrants(uuid->{
            var values=new Properties();values.setProperty(uuid.toString(),"purrr");
            values.setProperty(uuid+".accessories","ribcage,black_sneakers,pookles,swim_bands");values.setProperty(uuid+".accessory","ribcage");values.setProperty(uuid+".aura","gold_spark");
            assertTrue(CapeObstruction.wardrobeBlocks(WardrobePreferences.accessorySelections(values,uuid)));
            var updated=WardrobePreferences.capeToggle(values,uuid,WardrobeCape.PURRR);
            assertEquals(WardrobeCape.PURRR,WardrobePreferences.selection(updated,uuid));
            assertEquals(List.of(WardrobeAccessory.BLACK_SNEAKERS,WardrobeAccessory.POOKLES,WardrobeAccessory.SWIM_BANDS),WardrobePreferences.accessorySelections(updated,uuid));
            assertFalse(CapeObstruction.wardrobeBlocks(WardrobePreferences.accessorySelections(updated,uuid)));
            assertEquals("black_sneakers",updated.getProperty(uuid+".accessory"));assertEquals("gold_spark",updated.getProperty(uuid+".aura"));
            assertTrue(values.getProperty(uuid+".accessories").contains("ribcage"),"input snapshot remains untouched");
        });
    }
    @Test void selectingDifferentCapeClearsLegacyBlockerAndVisibleCapeStillTogglesOff()throws Exception {
        withGrants(uuid->{
            var values=new Properties();values.setProperty(uuid.toString(),"purrr");values.setProperty(uuid+".accessory","ribcage");
            var updated=WardrobePreferences.capeToggle(values,uuid,WardrobeCape.MAGENTA_CITY);
            assertEquals(WardrobeCape.MAGENTA_CITY,WardrobePreferences.selection(updated,uuid));
            assertTrue(WardrobePreferences.accessorySelections(updated,uuid).isEmpty());
            assertEquals("none",updated.getProperty(uuid+".accessories"));assertEquals("none",updated.getProperty(uuid+".accessory"));
            var off=WardrobePreferences.capeToggle(updated,uuid,WardrobeCape.MAGENTA_CITY);
            assertNull(WardrobePreferences.selection(off,uuid));assertEquals("minecraft",off.getProperty(uuid.toString()));
        });
    }
    @Test void unownedCapeCannotChangeTheLoadoutAndNonBlockingItemsRemainEquipped()throws Exception {
        withGrants(uuid->{
            var values=new Properties();values.setProperty(uuid.toString(),"minecraft");values.setProperty(uuid+".accessories","black_sneakers,pookles");
            assertEquals(values,WardrobePreferences.capeToggle(values,uuid,WardrobeCape.DUCKING_AROUND));
            assertEquals(values,WardrobePreferences.capeToggle(values,null,WardrobeCape.PURRR));
            var updated=WardrobePreferences.capeToggle(values,uuid,WardrobeCape.PURRR);
            assertEquals(WardrobeCape.PURRR,WardrobePreferences.selection(updated,uuid));
            assertEquals("black_sneakers,pookles",updated.getProperty(uuid+".accessories"));
        });
    }
}
