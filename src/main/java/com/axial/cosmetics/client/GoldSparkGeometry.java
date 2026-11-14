package com.axial.cosmetics.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.axial.cosmetics.client.HexwindGeometry.Point;
import com.axial.cosmetics.client.HexwindGeometry.Quad;

/** Seeded staggered births make a seamless six-second loop without frame-dependent particle state. */
public final class GoldSparkGeometry {
    public static final float PERIOD = 120;
    private record Spark(double birth, double life, double angle, double radius,
                         double base, double rise, double length, double width, double phase) { }
    private static final List<Spark> SPARKS;
    static {
        var random = new Random(0x601D5A);
        var sparks = new ArrayList<Spark>();
        for (int i = 0; i < 72; i++) {
            sparks.add(new Spark(i * (PERIOD / 72.0) + random.nextDouble() * 1.4,
                    12 + random.nextDouble() * 8, random.nextDouble() * Math.PI * 2,
                    .48 + random.nextDouble() * .3, .16 + random.nextDouble() * .65,
                    .85 + random.nextDouble() * .45, .16 + random.nextDouble() * .32,
                    .032 + random.nextDouble() * .020, random.nextDouble() * Math.PI * 2));
        }
        SPARKS = List.copyOf(sparks);
    }
    private GoldSparkGeometry() { }
    public static List<Quad> frame(float ticks, boolean detailed) {
        double time = ticks - Math.floor(ticks / PERIOD) * PERIOD;
        var mesh = new ArrayList<Quad>();
        for (int i = 0; i < SPARKS.size(); i++) {
            if (!detailed && i % 2 != 0) continue;
            var s = SPARKS.get(i);
            double age = (time - s.birth + PERIOD) % PERIOD;
            if (age >= s.life) continue;
            double t = age / s.life;
            double fade = Math.min(1, t * 10) * Math.min(1, (1 - t) * 6);
            double shimmer = .9 + .1 * Math.pow(Math.sin(age * 1.25 + s.phase), 2);
            double x = Math.cos(s.angle) * s.radius, z = Math.sin(s.angle) * s.radius;
            double y = s.base + s.rise * t;
            // Short separated steps keep trails crisp, with a brighter leading tip.
            for (int step = 0; step < 5; step++) {
                double strength = 1 - step / 5.0;
                panel(mesh, x, y - step * s.length / 5, z, s.width * (.35 + strength * .65),
                        s.length / 12, color(fade * shimmer * strength * .95, 0xF6B632));
            }
            panel(mesh, x, y, z, s.width, s.width, color(fade * shimmer, 0xFFF1A3));
            if (detailed) {
                panel(mesh, x, y, z, s.width * 2, s.width * 2, color(fade * shimmer * .22, 0xFFCC4A));
                double flash = Math.pow(Math.max(0, Math.sin(age * .9 + s.phase)), 6);
                panel(mesh, x, y, z, s.width * (1 + flash * 2), s.width * .42,
                        color(fade * flash, 0xFFF5C5));
                panel(mesh, x, y, z, s.width * .42, s.width * (1 + flash * 2),
                        color(fade * flash, 0xFFF5C5));
            }
        }
        return List.copyOf(mesh);
    }
    private static int color(double alpha, int rgb) {
        return ((int)(Math.pow(Math.max(0, Math.min(1, alpha)),.65) * 255) << 24) | rgb;
    }
    private static Point p(double x, double y, double z) { return new Point((float)x, (float)y, (float)z); }
    private static void panel(List<Quad> mesh, double x, double y, double z, double w, double h, int color) {
        mesh.add(new Quad(p(x-w,y-h,z),p(x+w,y-h,z),p(x+w,y+h,z),p(x-w,y+h,z),color));
        mesh.add(new Quad(p(x,y-h,z-w),p(x,y-h,z+w),p(x,y+h,z+w),p(x,y+h,z-w),color));
    }
}
