package com.axial.cosmetics.client;

/** A client-only marker in a particular Minecraft dimension. */
public final class Waypoint {
    public String name;
    public String dimension;
    public int x;
    public int y;
    public int z;
    public int color;

    public Waypoint(String name, String dimension, int x, int y, int z, int color) {
        this.name = name;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
    }
}
