package com.axial.cosmetics.client;

import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

/** Presentation-only offset; never sent to the presence service or stored as real activity. */
final class PresenceDisplayOffset {
    static final int MIN_OFFSET = 20;
    static final int MAX_OFFSET = 70;
    private final RandomGenerator random;
    private int offset, target;
    private long nextChange;

    PresenceDisplayOffset(RandomGenerator random, long now) {
        this.random = random;
        offset = random.nextInt(MIN_OFFSET, MAX_OFFSET + 1);
        target = random.nextInt(MIN_OFFSET, MAX_OFFSET + 1);
        schedule(now);
    }

    synchronized int value(long now) {
        if (now - nextChange >= 0) {
            if (offset == target) {
                target = random.nextInt(MIN_OFFSET, MAX_OFFSET + 1);
            } else {
                int step = Math.min(Math.abs(target - offset), random.nextInt(1, 3));
                offset += Integer.signum(target - offset) * step;
            }
            // Returning to the menu after a long absence never catches up in one large jump.
            schedule(now);
        }
        return offset;
    }

    synchronized long displayedCount(int realCount, long now) {
        return realCount < 0 ? -1 : (long) realCount + value(now);
    }

    private void schedule(long now) {
        nextChange = now + TimeUnit.SECONDS.toNanos(random.nextInt(25, 76));
    }
}
