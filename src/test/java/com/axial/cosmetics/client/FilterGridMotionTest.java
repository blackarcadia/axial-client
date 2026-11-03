package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FilterGridMotionTest {
    private static Rect slot(int index){return new Rect(index*100,20,90,70);}
    @Test void survivorsMoveAndDepartingCardsFadeWithoutRemainingInteractive() {
        var clock=new AtomicLong();var grid=new FilterGridMotion<String>(clock::get);
        grid.update(List.of("a","b"),FilterGridMotionTest::slot);assertEquals(2,grid.frame().size());
        grid.update(List.of("b"),FilterGridMotionTest::slot);
        clock.set(50_000_000);var frame=grid.frame();
        var b=frame.stream().filter(t->t.key().equals("b")).findFirst().orElseThrow();
        var a=frame.stream().filter(t->t.key().equals("a")).findFirst().orElseThrow();
        assertTrue(b.bounds().x()>0&&b.bounds().x()<100);assertTrue(b.present());
        assertFalse(a.present());assertTrue(a.opacity()>0&&a.opacity()<1);
        clock.set(1_000_000_000);assertEquals(List.of("b"),grid.frame().stream().map(FilterGridMotion.Tile::key).toList());
    }
    @Test void rapidQueryReversalKeepsExistingCardPositionAndOpacity() {
        var clock=new AtomicLong();var grid=new FilterGridMotion<String>(clock::get);
        grid.update(List.of("a","b"),FilterGridMotionTest::slot);grid.frame();
        grid.update(List.of("b"),FilterGridMotionTest::slot);clock.set(40_000_000);
        var before=grid.frame();
        grid.update(List.of("a","b"),FilterGridMotionTest::slot);var after=grid.frame();
        for(var tile:before) {
            var restored=after.stream().filter(t->t.key().equals(tile.key())).findFirst().orElseThrow();
            assertEquals(tile.bounds(),restored.bounds());assertEquals(tile.opacity(),restored.opacity());assertTrue(restored.present());
        }
        clock.set(2_000_000_000);var settled=grid.frame();assertEquals(2,settled.size());
        for(var tile:settled)assertEquals(1,tile.opacity());
    }
    @Test void resettingScrollForSearchPreservesTheVisibleCardPosition() {
        var clock=new AtomicLong();var grid=new FilterGridMotion<String>(clock::get);
        grid.update(List.of("a","b"),index->new Rect(0,20+index*100,90,70));grid.frame();
        grid.offsetY(-50);
        grid.update(List.of("b"),index->new Rect(0,20,90,70));
        var b=grid.frame().stream().filter(tile->tile.key().equals("b")).findFirst().orElseThrow();
        assertEquals(70,b.bounds().y()); // 120 - previous scroll of 50, with no visual jump.
        clock.set(1_000_000_000);assertEquals(20,grid.frame().getFirst().bounds().y());
    }
    @Test void newResultsFadeInAndEmptyResultsReleaseAllDepartingCards() {
        var clock=new AtomicLong();var grid=new FilterGridMotion<String>(clock::get);
        grid.update(List.of(),FilterGridMotionTest::slot);assertTrue(grid.frame().isEmpty());
        grid.update(List.of("new"),FilterGridMotionTest::slot);assertEquals(0,grid.frame().getFirst().opacity());
        clock.set(60_000_000);assertTrue(grid.frame().getFirst().opacity()>0);
        grid.update(List.of(),FilterGridMotionTest::slot);clock.set(2_000_000_000);assertTrue(grid.frame().isEmpty());
    }
}
