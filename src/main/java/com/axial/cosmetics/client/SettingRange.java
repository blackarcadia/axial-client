package com.axial.cosmetics.client;

/** Shared, bounded conversion for mouse, keyboard and numeric setting values. */
record SettingRange(double min, double max, double step) {
    SettingRange {
        if (!Double.isFinite(min) || !Double.isFinite(max) || !Double.isFinite(step) || max <= min || step <= 0)
            throw new IllegalArgumentException("Invalid setting range");
    }
    double clamp(double value) {
        if (!Double.isFinite(value)) return min;
        return Math.clamp(min + Math.round((Math.clamp(value,min,max)-min)/step)*step,min,max);
    }
    double value(double progress) { return clamp(min+Math.clamp(progress,0,1)*(max-min)); }
    double progress(double value) { return (clamp(value)-min)/(max-min); }
}
