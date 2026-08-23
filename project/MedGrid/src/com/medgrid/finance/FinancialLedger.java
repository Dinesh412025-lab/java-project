package com.medgrid.finance;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class FinancialLedger {
    private static final FinancialLedger instance = new FinancialLedger();
    
    private final ConcurrentHashMap<String, AtomicReference<Double>> revenueMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicReference<Double>> costMap = new ConcurrentHashMap<>();

    private FinancialLedger() {}

    public static FinancialLedger getInstance() { return instance; }

    public void addRevenue(String entityId, double amount) {
        revenueMap.computeIfAbsent(entityId, k -> new AtomicReference<>(0.0))
                  .accumulateAndGet(amount, Double::sum);
    }

    public void addCost(String entityId, double amount) {
        costMap.computeIfAbsent(entityId, k -> new AtomicReference<>(0.0))
               .accumulateAndGet(amount, Double::sum);
    }
    
    public double getRevenue(String entityId) {
        AtomicReference<Double> ref = revenueMap.get(entityId);
        return ref != null ? ref.get() : 0.0;
    }

    public double getCost(String entityId) {
        AtomicReference<Double> ref = costMap.get(entityId);
        return ref != null ? ref.get() : 0.0;
    }

    public double getTotalRevenue() {
        return revenueMap.values().stream().mapToDouble(AtomicReference::get).sum();
    }

    public double getTotalCost() {
        return costMap.values().stream().mapToDouble(AtomicReference::get).sum();
    }
}
