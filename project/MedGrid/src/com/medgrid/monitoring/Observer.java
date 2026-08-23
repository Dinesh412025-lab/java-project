package com.medgrid.monitoring;

public interface Observer {
    void logEvent(String eventType, String message);
}
