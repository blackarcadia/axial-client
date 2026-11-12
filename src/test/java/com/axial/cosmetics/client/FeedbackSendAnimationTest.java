package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FeedbackSendAnimationTest {
    @Test void formFoldsBeforeEnvelopeSealsAndLaunches(){
        assertEquals(0,FeedbackSendAnimation.fold(0));
        assertEquals(1,FeedbackSendAnimation.fold(.4));
        assertEquals(0,FeedbackSendAnimation.seal(.4));
        assertEquals(1,FeedbackSendAnimation.seal(1.1));
        assertEquals(0,FeedbackSendAnimation.flight(1.1));
        assertEquals(1,FeedbackSendAnimation.flight(2));
        assertTrue(FeedbackSendAnimation.text(1.5)>.99);
        assertFalse(FeedbackSendAnimation.finished(2.3));
        assertTrue(FeedbackSendAnimation.finished(2.6));
        assertEquals(0,FeedbackSendAnimation.text(2.6));
    }
    @Test void motionIsBoundedAndContinuous(){
        float previous=0;
        for(double t=0;t<=3;t+=.005){
            float next=FeedbackSendAnimation.flight(t);
            assertTrue(next>=previous&&next<=1);
            assertTrue(next-previous<.03);
            previous=next;
        }
    }
}
