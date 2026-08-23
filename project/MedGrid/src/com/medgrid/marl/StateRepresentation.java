package com.medgrid.marl;

import com.medgrid.model.CaseSeverity;

import java.util.Objects;

/**
 * Discretized state representation for an ambulance agent in the MARL negotiation layer.
 * State tuple: (DistanceCategory, Severity, ZoneRiskCategory, ContentionCategory, WorkloadCategory)
 */
public class StateRepresentation {
    public enum DistanceCategory {
        NEAR,       // < 4 km
        MEDIUM,     // 4 - 8 km
        FAR         // > 8 km
    }

    public enum RiskCategory {
        LOW_RISK,       // Risk < 0.3
        MODERATE_RISK,  // Risk 0.3 - 0.7
        HIGH_RISK       // Risk > 0.7
    }

    public enum ContentionCategory {
        LOW_CONTENTION,   // Most ambulances idle (> 60% idle)
        HIGH_CONTENTION   // Few ambulances idle (<= 40% idle)
    }

    public enum WorkloadCategory {
        UNDERLOADED,  // Agent handled significantly fewer cases than average
        BALANCED,     // Agent handled close to average cases
        OVERLOADED    // Agent handled significantly more cases than average
    }

    private final DistanceCategory distanceCategory;
    private final CaseSeverity severity;
    private final RiskCategory riskCategory;
    private final ContentionCategory contentionCategory;
    private final WorkloadCategory workloadCategory;

    public StateRepresentation(DistanceCategory distanceCategory,
                               CaseSeverity severity,
                               RiskCategory riskCategory,
                               ContentionCategory contentionCategory,
                               WorkloadCategory workloadCategory) {
        this.distanceCategory = distanceCategory != null ? distanceCategory : DistanceCategory.MEDIUM;
        this.severity = severity != null ? severity : CaseSeverity.MEDIUM;
        this.riskCategory = riskCategory != null ? riskCategory : RiskCategory.LOW_RISK;
        this.contentionCategory = contentionCategory != null ? contentionCategory : ContentionCategory.LOW_CONTENTION;
        this.workloadCategory = workloadCategory != null ? workloadCategory : WorkloadCategory.BALANCED;
    }

    public static DistanceCategory categorizeDistance(double distance) {
        if (distance < 4.0) return DistanceCategory.NEAR;
        if (distance <= 8.0) return DistanceCategory.MEDIUM;
        return DistanceCategory.FAR;
    }

    public static RiskCategory categorizeRisk(double riskScore) {
        if (riskScore < 0.3) return RiskCategory.LOW_RISK;
        if (riskScore <= 0.7) return RiskCategory.MODERATE_RISK;
        return RiskCategory.HIGH_RISK;
    }

    public static ContentionCategory categorizeContention(int idleAmbulances, int totalAmbulances) {
        if (totalAmbulances <= 0) return ContentionCategory.LOW_CONTENTION;
        double ratio = (double) idleAmbulances / totalAmbulances;
        return ratio >= 0.5 ? ContentionCategory.LOW_CONTENTION : ContentionCategory.HIGH_CONTENTION;
    }

    public static WorkloadCategory categorizeWorkload(int agentCases, double avgCases) {
        if (avgCases <= 1.0) return WorkloadCategory.BALANCED;
        double diff = agentCases - avgCases;
        if (diff < -1.5) return WorkloadCategory.UNDERLOADED;
        if (diff > 1.5) return WorkloadCategory.OVERLOADED;
        return WorkloadCategory.BALANCED;
    }

    public DistanceCategory getDistanceCategory() { return distanceCategory; }
    public CaseSeverity getSeverity() { return severity; }
    public RiskCategory getRiskCategory() { return riskCategory; }
    public ContentionCategory getContentionCategory() { return contentionCategory; }
    public WorkloadCategory getWorkloadCategory() { return workloadCategory; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateRepresentation that = (StateRepresentation) o;
        return distanceCategory == that.distanceCategory &&
                severity == that.severity &&
                riskCategory == that.riskCategory &&
                contentionCategory == that.contentionCategory &&
                workloadCategory == that.workloadCategory;
    }

    @Override
    public int hashCode() {
        return Objects.hash(distanceCategory, severity, riskCategory, contentionCategory, workloadCategory);
    }

    @Override
    public String toString() {
        return String.format("[%s|%s|%s|%s|%s]",
                distanceCategory, severity, riskCategory, contentionCategory, workloadCategory);
    }
}
