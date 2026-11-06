package com.axial.cosmetics.client;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

/** Pairs health metadata with confirmed local hits in either packet order. */
final class HitDamageTracker {
    private static final double WINDOW = .25;
    private record Loss(double amount, double time) {}
    private final Map<Integer, Double> hits = new HashMap<>();
    private final Map<Integer, Loss> losses = new HashMap<>();
    void clear() { hits.clear(); losses.clear(); }
    private void expire(double now) {
        hits.values().removeIf(time -> now < time || now - time > WINDOW);
        losses.values().removeIf(loss -> now < loss.time || now - loss.time > WINDOW);
    }
    OptionalDouble hit(int id, double now) {
        expire(now);
        Loss loss = losses.remove(id);
        if (loss != null) { hits.remove(id); return OptionalDouble.of(loss.amount); }
        if (hits.size() >= 128) hits.clear();
        hits.put(id, now);
        return OptionalDouble.empty();
    }
    OptionalDouble loss(int id, double amount, double now) {
        expire(now);
        if (!Double.isFinite(amount) || amount <= 0) return OptionalDouble.empty();
        if (hits.remove(id) != null) { losses.remove(id); return OptionalDouble.of(amount); }
        if (losses.size() >= 128) losses.clear();
        losses.put(id, new Loss(amount, now));
        return OptionalDouble.empty();
    }
    static String label(double damage) {
        return "- " + java.math.BigDecimal.valueOf(damage).setScale(1, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString() + " HP";
    }
}
