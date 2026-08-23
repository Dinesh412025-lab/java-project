package com.medgrid.agent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class Agent {
    protected final String id;
    protected final ExecutorService executor;
    private volatile boolean isRunning;

    public Agent(String id) {
        this.id = id;
        this.executor = Executors.newSingleThreadExecutor();
        this.isRunning = true;
    }

    public String getId() { return id; }

    public void shutdown() {
        isRunning = false;
        executor.shutdownNow();
    }
    
    protected boolean isRunning() {
        return isRunning;
    }
}
