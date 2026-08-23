package com.medgrid.evaluation;

import com.medgrid.marl.FairnessMetric;
import com.medgrid.model.CaseSeverity;

import java.util.*;

/**
 * Statistical metrics collector for research evaluation.
 */
public class BenchmarkMetrics {
    private final String systemName;
    private final String scenarioName;
    private final List<Double> responseTimes;
    private final Map<CaseSeverity, List<Double>> responseTimesBySeverity;
    private final Map<String, Integer> agentWorkloadCounts;
    private int totalCases;
    private int successfulCases;
    private int failedCases;
    private int correctTriagePredictions;
    private int totalTriageEvaluations;

    public BenchmarkMetrics(String systemName, String scenarioName) {
        this.systemName = systemName;
        this.scenarioName = scenarioName;
        this.responseTimes = new ArrayList<>();
        this.responseTimesBySeverity = new EnumMap<>(CaseSeverity.class);
        for (CaseSeverity sev : CaseSeverity.values()) {
            responseTimesBySeverity.put(sev, new ArrayList<>());
        }
        this.agentWorkloadCounts = new HashMap<>();
        this.totalCases = 0;
        this.successfulCases = 0;
        this.failedCases = 0;
        this.correctTriagePredictions = 0;
        this.totalTriageEvaluations = 0;
    }

    public synchronized void recordCaseResult(String ambulanceId, CaseSeverity severity, double responseTimeMs, boolean success) {
        totalCases++;
        if (success) {
            successfulCases++;
            responseTimes.add(responseTimeMs);
            responseTimesBySeverity.get(severity).add(responseTimeMs);
            if (ambulanceId != null && !ambulanceId.isEmpty()) {
                agentWorkloadCounts.merge(ambulanceId, 1, Integer::sum);
            }
        } else {
            failedCases++;
        }
    }

    public synchronized void recordTriageResult(boolean isCorrect) {
        totalTriageEvaluations++;
        if (isCorrect) correctTriagePredictions++;
    }

    public double getMeanResponseTime() {
        if (responseTimes.isEmpty()) return 0.0;
        double sum = 0;
        for (double t : responseTimes) sum += t;
        return sum / responseTimes.size();
    }

    public double getP95ResponseTime() {
        if (responseTimes.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(responseTimes);
        Collections.sort(sorted);
        int idx = (int) Math.ceil(0.95 * sorted.size()) - 1;
        return sorted.get(Math.max(0, idx));
    }

    public double getMeanResponseTimeForSeverity(CaseSeverity severity) {
        List<Double> list = responseTimesBySeverity.get(severity);
        if (list == null || list.isEmpty()) return 0.0;
        double sum = 0;
        for (double t : list) sum += t;
        return sum / list.size();
    }

    public double getJainsFairnessIndex() {
        return FairnessMetric.computeJainsIndex(agentWorkloadCounts);
    }

    public double getGiniCoefficient() {
        if (agentWorkloadCounts.isEmpty()) return 0.0;
        double[] vals = new double[agentWorkloadCounts.size()];
        int i = 0;
        for (int c : agentWorkloadCounts.values()) vals[i++] = c;
        return FairnessMetric.computeGiniCoefficient(vals);
    }

    public double getWorkloadStdDev() {
        if (agentWorkloadCounts.isEmpty()) return 0.0;
        double[] vals = new double[agentWorkloadCounts.size()];
        int i = 0;
        for (int c : agentWorkloadCounts.values()) vals[i++] = c;
        return FairnessMetric.computeWorkloadStdDev(vals);
    }

    public double getTriageAccuracy() {
        if (totalTriageEvaluations == 0) return 0.0;
        return (double) correctTriagePredictions / totalTriageEvaluations * 100.0;
    }

    public String getSystemName() { return systemName; }
    public String getScenarioName() { return scenarioName; }
    public int getTotalCases() { return totalCases; }
    public int getSuccessfulCases() { return successfulCases; }
    public int getFailedCases() { return failedCases; }
    public Map<String, Integer> getAgentWorkloadCounts() { return Collections.unmodifiableMap(agentWorkloadCounts); }
}
