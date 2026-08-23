package com.medgrid.model;

import java.util.Objects;

public class Location {
    private final String id;
    private final int x;
    private final int y;

    public Location(String id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public String getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Objects.equals(id, location.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id + "(" + x + "," + y + ")";
    }
}
