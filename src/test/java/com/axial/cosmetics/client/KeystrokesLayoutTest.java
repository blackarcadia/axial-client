package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class KeystrokesLayoutTest {
    @Test void requestedThreeRowsAndMouseButtonsAreArrangedCorrectly(){
        var keys=KeystrokesLayout.keys(true,true);
        assertEquals(List.of("W","A","S","D","LFT","RHT"),keys.stream().map(KeystrokesLayout.Key::label).toList());
        assertTrue(keys.get(0).y()<keys.get(1).y());assertEquals(keys.get(1).y(),keys.get(2).y());
        assertTrue(keys.get(2).y()<keys.get(3).y());assertEquals(keys.get(0).x(),keys.get(3).x());
        assertTrue(keys.get(3).y()<keys.get(4).y());assertEquals(keys.get(4).y(),keys.get(5).y());
    }
    @Test void standardLayoutHasOneWAboveTheAsdRow(){
        var keys=KeystrokesLayout.keys(false,true);
        assertEquals(keys.get(1).y(),keys.get(2).y());assertEquals(keys.get(2).y(),keys.get(3).y());
        assertEquals(keys.get(0).x(),keys.get(2).x());assertTrue(keys.get(0).y()<keys.get(2).y());
        assertTrue(keys.get(4).y()>keys.get(3).y());
    }
    @Test void layoutsStayInsideTheirBoundsWithoutOverlappingOrSharingInputBits(){
        for(boolean stacked:new boolean[]{false,true})for(boolean mouse:new boolean[]{false,true}){
            var keys=KeystrokesLayout.keys(stacked,mouse);Set<Integer> bits=new HashSet<>();
            assertEquals(mouse?6:4,keys.size());
            for(int i=0;i<keys.size();i++){
                var a=keys.get(i);assertTrue(bits.add(a.bit()));assertTrue(a.x()>=0&&a.y()>=0);
                assertTrue(a.x()+a.width()<=KeystrokesLayout.WIDTH);assertTrue(a.y()+a.height()<=KeystrokesLayout.height(stacked,mouse));
                for(int j=i+1;j<keys.size();j++){var b=keys.get(j);assertFalse(a.x()<b.x()+b.width()&&a.x()+a.width()>b.x()&&a.y()<b.y()+b.height()&&a.y()+a.height()>b.y());}
            }
        }
    }
    @Test void everyVisibilityCombinationCompactsAndCentersRemainingRows(){
        for(boolean stacked:new boolean[]{false,true})for(int mask=0;mask<64;mask++){
            var layout=KeystrokesLayout.arrange(stacked,mask);
            assertEquals(Integer.bitCount(mask),layout.keys().size());
            if(mask==0){assertEquals(0,layout.width());assertEquals(0,layout.height());continue;}
            assertEquals(0,layout.keys().stream().mapToInt(KeystrokesLayout.Key::y).min().orElseThrow());
            assertEquals(layout.width(),layout.keys().stream().mapToInt(k->k.x()+k.width()).max().orElseThrow());
            assertEquals(layout.height(),layout.keys().stream().mapToInt(k->k.y()+k.height()).max().orElseThrow());
            var rows=new java.util.TreeMap<Integer,java.util.List<KeystrokesLayout.Key>>();
            for(var key:layout.keys()){assertNotEquals(0,mask&(1<<key.bit()));rows.computeIfAbsent(key.y(),v->new java.util.ArrayList<>()).add(key);}
            int expectedY=0;
            for(var row:rows.entrySet()){
                assertEquals(expectedY,row.getKey());var keys=row.getValue();
                int left=keys.getFirst().x(),right=layout.width()-keys.getLast().x()-keys.getLast().width();assertTrue(Math.abs(left-right)<=1);
                for(int i=1;i<keys.size();i++)assertEquals(keys.get(i-1).x()+keys.get(i-1).width()+2,keys.get(i).x());
                expectedY+=keys.getFirst().height()+2;
            }
        }
    }
    @Test void aSingleEnabledButtonHasNoEmptyKeyboardOrMouseSpace(){
        var keyboard=KeystrokesLayout.arrange(true,1<<2);assertEquals(22,keyboard.width());assertEquals(22,keyboard.height());assertEquals(0,keyboard.keys().getFirst().x());
        var mouse=KeystrokesLayout.arrange(false,1<<5);assertEquals(34,mouse.width());assertEquals(20,mouse.height());assertEquals(0,mouse.keys().getFirst().y());
    }
}
