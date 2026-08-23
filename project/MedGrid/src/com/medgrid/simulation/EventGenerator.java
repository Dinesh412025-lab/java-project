package com.medgrid.simulation;

import com.medgrid.agent.DispatchAgent;
import com.medgrid.config.ScenarioRecord;
import com.medgrid.model.CaseSeverity;
import com.medgrid.model.Location;
import com.medgrid.routing.Node;

import java.util.List;

public class EventGenerator implements Runnable {
    private final DispatchAgent dispatchAgent;
    private final EmergencyCaseFactory factory;
    private final int totalCasesToGenerate;
    private final List<ScenarioRecord> scenarios;
    private volatile boolean running = true;
    private int generatedCount = 0;

    public EventGenerator(DispatchAgent dispatchAgent, EmergencyCaseFactory factory, int totalCasesToGenerate, List<ScenarioRecord> scenarios) {
        this.dispatchAgent = dispatchAgent;
        this.factory = factory;
        this.totalCasesToGenerate = totalCasesToGenerate;
        this.scenarios = scenarios;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        if (scenarios != null && !scenarios.isEmpty()) {
            runScenarios();
        } else {
            runRandom();
        }
    }

    private void runScenarios() {
        long startTime = System.currentTimeMillis();
        for (ScenarioRecord s : scenarios) {
            if (!running) break;
            long now = System.currentTimeMillis();
            long waitTime = s.timeOffsetMs - (now - startTime);
            if (waitTime > 0) {
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            // Find location by matching node names in factory's graph
            Location loc = new Location(s.locationName, 0, 0); // fallback
            dispatchAgent.processEmergencyCall(factory.createCase(loc, CaseSeverity.valueOf(s.severity), s.type));
        }
    }

    private void runRandom() {
        while (running && generatedCount < totalCasesToGenerate) {
            dispatchAgent.processEmergencyCall(factory.generateRandomCase());
            generatedCount++;
            try {
                Thread.sleep(500 + (long)(Math.random() * 1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
