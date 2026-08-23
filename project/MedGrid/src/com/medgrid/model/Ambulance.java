package com.medgrid.model;

import java.util.concurrent.atomic.AtomicReference;

public class Ambulance {
    public enum State { IDLE, EN_ROUTE_TO_PATIENT, EN_ROUTE_TO_HOSPITAL, MAINTENANCE }

    private final String id;
    private final AtomicReference<Location> currentLocation;
    private final AtomicReference<State> state;

    public Ambulance(String id, Location startLocation) {
        this.id = id;
        this.currentLocation = new AtomicReference<>(startLocation);
        this.state = new AtomicReference<>(State.IDLE);
    }

    public String getId() { return id; }
    
    public Location getCurrentLocation() { return currentLocation.get(); }
    public void setCurrentLocation(Location location) { this.currentLocation.set(location); }
    
    public State getState() { return state.get(); }
    public boolean setState(State expected, State update) { return state.compareAndSet(expected, update); }
    public void setState(State update) { this.state.set(update); }

    public boolean isAvailable() {
        return state.get() == State.IDLE;
    }

    @Override
    public String toString() {
        return "Ambulance{" + id + ", state=" + state.get() + ", loc=" + currentLocation.get() + "}";
    }
}
