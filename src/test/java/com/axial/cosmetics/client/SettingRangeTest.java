package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SettingRangeTest {
    @Test void draggingAlwaysStaysWithinSavedRanges() {
        for(var range:new SettingRange[]{new SettingRange(.1,4,.01),new SettingRange(0,32,2),new SettingRange(2,12,2),new SettingRange(.25,3,.01)}) {
            assertEquals(range.min(),range.value(-10),.00001);
            assertEquals(range.max(),range.value(10),.00001);
            assertEquals(range.min(),range.clamp(Double.NaN),.00001);
            for(int i=0;i<=100;i++) {
                double value=range.value(i/100.0);
                assertTrue(value>=range.min()&&value<=range.max());
                assertEquals(value,range.value(range.progress(value)),.00001);
            }
        }
    }
    @Test void crosshairDimensionsKeepTheirExistingEvenSteps() {
        assertEquals(6,new SettingRange(0,32,2).clamp(5));
        assertEquals(2,new SettingRange(2,12,2).clamp(1));
        assertEquals(4,new SettingRange(0,20,2).clamp(4.9));
    }
}
