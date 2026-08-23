package com.medgrid.simulation;

import com.medgrid.agent.AmbulanceAgent;
import com.medgrid.agent.DispatchAgent;
import com.medgrid.agent.HospitalAgent;
import com.medgrid.monitoring.MonitoringService;
import com.medgrid.routing.TrafficManager;

import java.util.List;

public class SimulationEngine {
    private final List<HospitalAgent> hospitals;
    private final List<AmbulanceAgent> ambulances;
    private final DispatchAgent dispatchAgent;
    private final EventGenerator eventGenerator;
    private final TrafficManager trafficManager;
    private Thread generatorThread;

    public SimulationEngine(List<HospitalAgent> hospitals, List<AmbulanceAgent> ambulances, DispatchAgent dispatchAgent, EventGenerator eventGenerator, TrafficManager trafficManager) {
        this.hospitals = hospitals;
        this.ambulances = ambulances;
        this.dispatchAgent = dispatchAgent;
        this.eventGenerator = eventGenerator;
        this.trafficManager = trafficManager;
    }

    public void start() {
        System.out.println("Starting MedGrid Simulation...");
        generatorThread = new Thread(eventGenerator);
        generatorThread.start();
        
        if (trafficManager != null) {
            trafficManager.start();
        }
        
        // Start dashboard thread
        Thread dashboard = new Thread(() -> {
            try {
                while (generatorThread.isAlive()) {
                    MonitoringService.getInstance().printDashboard();
                    MonitoringService.getInstance().takeSnapshot();
                    Thread.sleep(2000); // refresh every 2 seconds
                }
                // Print final dashboard
                MonitoringService.getInstance().printDashboard();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        dashboard.setDaemon(true);
        dashboard.start();
    }
    
    public void waitForCompletion() {
        try {
            if (generatorThread != null) {
                generatorThread.join();
                // Wait extra time for remaining cases to settle
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void shutdown() {
        eventGenerator.stop();
        if (trafficManager != null) {
            trafficManager.stop();
        }
        hospitals.forEach(HospitalAgent::shutdown);
        ambulances.forEach(AmbulanceAgent::shutdown);
        dispatchAgent.shutdown();
        System.out.println("MedGrid Simulation stopped.");
    }
}
