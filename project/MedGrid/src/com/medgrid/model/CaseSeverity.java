package com.medgrid.model;

public enum CaseSeverity {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int priority;

    CaseSeverity(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
