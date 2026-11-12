package com.axial.cosmetics.client;

import com.axial.cosmetics.data.CosmeticSlot;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CosmeticLoadoutTest {
    record Item(String id,Set<CosmeticSlot> slots){}
    private static Item item(String id,CosmeticSlot... slots){return new Item(id,Set.of(slots));}
    @Test void helmetChestLegsAndBootsCoexistAndOnlyAConflictingSlotIsReplaced(){
        var head=item("hat",CosmeticSlot.HEAD);var chest=item("shirt",CosmeticSlot.CHEST);
        var legs=item("trousers",CosmeticSlot.LEGS);var boots=item("shoes",CosmeticSlot.BOOTS);
        var all=List.of(head,chest,legs,boots);
        assertEquals(all,CosmeticLoadout.compatible(all,Item::slots));
        var replacement=item("new_shirt",CosmeticSlot.CHEST);
        var updated=CosmeticLoadout.toggle(all,replacement,Item::slots);
        assertEquals(List.of(head,legs,boots,replacement),updated);
        assertEquals(List.of(head,legs,boots),CosmeticLoadout.toggle(updated,replacement,Item::slots));
        var suit=item("suit",CosmeticSlot.CHEST,CosmeticSlot.LEGS);
        assertEquals(List.of(head,boots,suit),CosmeticLoadout.toggle(all,suit,Item::slots));
    }
    @Test void malformedOverlappingSelectionsResolveWithoutStackingTheSameSlot(){
        var first=item("first",CosmeticSlot.HEAD);var second=item("second",CosmeticSlot.HEAD);
        assertEquals(List.of(first),CosmeticLoadout.compatible(List.of(first,first,second),Item::slots));
    }
    @Test void legacySelectionMigratesAndIndependentAccessoriesSurviveSaveReloadAndRemoval()throws Exception{
        UUID uuid=UUID.fromString("12345678-1234-4234-8234-123456789012");
        var field=CosmeticAssignments.class.getDeclaredField("grants");field.setAccessible(true);var previous=field.get(null);
        try{
            field.set(null,Map.of(uuid,Set.of("swim_bands","white_kicks")));
            var values=new Properties();values.setProperty(uuid+".accessory","swim_bands");values.setProperty(uuid+".aura","gold_spark");
            assertEquals(List.of(WardrobeAccessory.SWIM_BANDS),WardrobePreferences.accessorySelections(values,uuid));
            var both=WardrobePreferences.accessoryToggle(values,uuid,WardrobeAccessory.WHITE_KICKS);
            var writer=new StringWriter();both.store(writer,"");var restored=new Properties();restored.load(new StringReader(writer.toString()));
            assertEquals(List.of(WardrobeAccessory.SWIM_BANDS,WardrobeAccessory.WHITE_KICKS),WardrobePreferences.accessorySelections(restored,uuid));
            assertEquals("gold_spark",restored.getProperty(uuid+".aura"));
            var onlyShoes=WardrobePreferences.accessoryToggle(restored,uuid,WardrobeAccessory.SWIM_BANDS);
            assertEquals(List.of(WardrobeAccessory.WHITE_KICKS),WardrobePreferences.accessorySelections(onlyShoes,uuid));
            assertTrue(WardrobePreferences.accessorySelections(WardrobePreferences.accessoryToggle(onlyShoes,uuid,WardrobeAccessory.WHITE_KICKS),uuid).isEmpty());
            restored.setProperty(uuid+".accessories","none");assertTrue(WardrobePreferences.accessorySelections(restored,uuid).isEmpty());
            field.set(null,Map.of(uuid,Set.of("white_kicks")));
            assertEquals(List.of(WardrobeAccessory.WHITE_KICKS),WardrobePreferences.accessorySelections(both,uuid));
            assertTrue(WardrobePreferences.accessorySelections(both,new UUID(0,1)).isEmpty());
        }finally{field.set(null,previous);}
    }
}
