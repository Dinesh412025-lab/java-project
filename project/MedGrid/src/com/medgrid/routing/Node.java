package com.medgrid.routing;

import com.medgrid.model.Location;

public class Node {
    private final Location location;

    public Node(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public String getId() {
        return location.getId();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Node)) return false;
        Node other = (Node) obj;
        return location.equals(other.location);
    }

    @Override
    public int hashCode() {
        return location.hashCode();
    }
}
