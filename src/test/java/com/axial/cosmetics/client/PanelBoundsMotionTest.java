package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PanelBoundsMotionTest {
    @Test void resizingRedirectsFromItsCurrentVisibleBounds() {
        var clock=new AtomicLong();var motion=new PanelBoundsMotion(clock::get);
        var mods=new Rect(80,100,720,304);var settings=new Rect(180,80,520,342);
        var wardrobe=new Rect(60,40,760,410);
        motion.jump(mods);motion.redirect(settings);assertEquals(mods,motion.value());
        clock.set(60_000_000);var midway=motion.value();
        assertTrue(midway.width()<mods.width()&&midway.width()>settings.width());
        motion.redirect(wardrobe);assertEquals(midway,motion.value());
        clock.set(90_000_000);assertTrue(motion.value().width()>midway.width());
        var interrupted=motion.value();motion.redirect(mods);assertEquals(interrupted,motion.value());
        clock.set(500_000_000);assertEquals(mods,motion.value());
    }
    @Test void pointerCoordinatesFollowTheDisplayedPanelAtEveryResizeStage() {
        var clock=new AtomicLong();var motion=new PanelBoundsMotion(clock::get);
        var from=new Rect(70,40,720,304);var target=new Rect(170,20,520,342);
        motion.jump(from);motion.redirect(target);
        for(long nanos:new long[]{0,30_000_000,90_000_000,180_000_000}) {
            clock.set(nanos);var shown=motion.value();
            for(double fraction:new double[]{0,.25,.5,1}) {
                assertEquals(target.x()+fraction*target.width(),PanelBoundsMotion.inverseX(shown,target,shown.x()+fraction*shown.width()),.00001);
                assertEquals(target.y()+fraction*target.height(),PanelBoundsMotion.inverseY(shown,target,shown.y()+fraction*shown.height()),.00001);
            }
        }
    }
}
