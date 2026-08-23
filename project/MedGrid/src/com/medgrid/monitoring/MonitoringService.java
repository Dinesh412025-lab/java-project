package com.medgrid.monitoring;

import com.medgrid.model.Ambulance;
import com.medgrid.model.Hospital;
import com.medgrid.finance.FinancialLedger;
import com.medgrid.persistence.DataExporter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MonitoringService implements Observer {
    
    public static class DataPoint {
        public long timestamp;
        public int cases;
        public double netRevenue;
        
        public DataPoint(long timestamp, int cases, double netRevenue) {
            this.timestamp = timestamp;
            this.cases = cases;
            this.netRevenue = netRevenue;
        }
    }

    private static final MonitoringService instance = new MonitoringService();

    private final Map<String, Hospital> hospitals = new ConcurrentHashMap<>();
    private final Map<String, Ambulance> ambulances = new ConcurrentHashMap<>();
    private final Map<String, String> latestLogs = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<DataPoint> history = new ConcurrentLinkedQueue<>();
    
    private final AtomicInteger totalCases = new AtomicInteger(0);
    private final AtomicLong totalResponseTimeMs = new AtomicLong(0);

    private MonitoringService() {}

    public static MonitoringService getInstance() { return instance; }

    public void registerHospital(Hospital h) { hospitals.put(h.getId(), h); }
    public void registerAmbulance(Ambulance a) { ambulances.put(a.getId(), a); }
    
    public void recordCaseResponse(long responseTimeMs) {
        totalCases.incrementAndGet();
        totalResponseTimeMs.addAndGet(responseTimeMs);
    }

    @Override
    public void logEvent(String source, String message) {
        latestLogs.put(source, message);
        DataExporter.getInstance().logEvent("[" + source + "] " + message);
    }

    public void printDashboard() {
        System.out.println("\n=======================================================");
        System.out.println("                MEDGRID LIVE DASHBOARD                 ");
        System.out.println("=======================================================");
        
        System.out.println("[ HOSPITALS ]");
        for (Hospital h : hospitals.values()) {
            System.out.printf(" %-12s | ER: %2d/%2d | ICU: %2d/%2d | Load: %5.1f%% | Rev: $%.2f%n", 
                h.getId(), 
                h.getAvailableErBeds(), h.getAvailableErBeds() + h.getLoadPercentage() > 0 ? (int)(h.getAvailableErBeds() / (1 - h.getLoadPercentage()/100)) : h.getAvailableErBeds(), // simplified
                h.getAvailableIcuBeds(), h.getAvailableIcuBeds() + 2, // simplified for view
                h.getLoadPercentage(),
                FinancialLedger.getInstance().getRevenue(h.getId()));
        }

        System.out.println("\n[ AMBULANCES ]");
        for (Ambulance a : ambulances.values()) {
            System.out.printf(" %-12s | State: %-20s | Loc: %-10s | Cost: $%.2f%n", 
                a.getId(), a.getState(), a.getCurrentLocation().getId(), FinancialLedger.getInstance().getCost(a.getId()));
        }

        System.out.println("\n[ RECENT EVENTS ]");
        latestLogs.values().stream().limit(5).forEach(log -> System.out.println(" > " + log));
        
        int cases = totalCases.get();
        double avgTime = cases > 0 ? (totalResponseTimeMs.get() / (double)cases) : 0;
        System.out.println("\n[ METRICS ] Total Cases: " + cases + " | Avg Response: " + String.format("%.2f", avgTime) + " ms");
        System.out.println("=======================================================\n");
    }

    public List<Hospital> getHospitals() {
        return List.copyOf(hospitals.values());
    }

    public List<Ambulance> getAmbulances() {
        return List.copyOf(ambulances.values());
    }

    public List<String> getRecentLogs() {
        return latestLogs.values().stream().limit(10).toList();
    }

    public int getTotalCases() { return totalCases.get(); }
    public double getAverageResponseTime() { 
        int cases = totalCases.get();
        return cases > 0 ? (totalResponseTimeMs.get() / (double)cases) : 0;
    }

    public void takeSnapshot() {
        double net = FinancialLedger.getInstance().getTotalRevenue() - FinancialLedger.getInstance().getTotalCost();
        history.offer(new DataPoint(System.currentTimeMillis(), totalCases.get(), net));
        if (history.size() > 50) {
            history.poll();
        }
    }

    public List<DataPoint> getHistory() {
        return List.copyOf(history);
    }
}
