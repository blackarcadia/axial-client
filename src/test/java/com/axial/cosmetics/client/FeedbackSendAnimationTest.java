package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FeedbackSendAnimationTest {
    @Test void formFoldsBeforeEnvelopeSealsAndLaunches(){
        assertEquals(0,FeedbackSendAnimation.fold(0));
        assertEquals(1,FeedbackSendAnimation.fold(.23));
        assertEquals(0,FeedbackSendAnimation.seal(.23));
        assertEquals(1,FeedbackSendAnimation.seal(.51));
        assertEquals(0,FeedbackSendAnimation.flight(.51));
        assertEquals(1,FeedbackSendAnimation.flight(.96));
        // The faster envelope still leaves the confirmation fully readable for over a second.
        assertTrue(FeedbackSendAnimation.text(.56)>.99);
        assertTrue(FeedbackSendAnimation.text(1.64)>.99);
        assertFalse(FeedbackSendAnimation.finished(1.85));
        assertTrue(FeedbackSendAnimation.finished(1.91));
        assertEquals(0,FeedbackSendAnimation.text(1.91));
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
