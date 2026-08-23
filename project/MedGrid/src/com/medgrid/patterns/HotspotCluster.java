package com.medgrid.patterns;

import com.medgrid.model.CaseSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a detected spatiotemporal hotspot cluster of incidents.
 */
public class HotspotCluster {
    private final int clusterId;
    private final List<IncidentRecord> incidents;
    private double centroidX;
    private double centroidY;
    private long startTimeMs;
    private long endTimeMs;
    private double aggregateRiskScore;

    public HotspotCluster(int clusterId) {
        this.clusterId = clusterId;
        this.incidents = new ArrayList<>();
        this.startTimeMs = Long.MAX_VALUE;
        this.endTimeMs = Long.MIN_VALUE;
        this.aggregateRiskScore = 0.0;
    }

    public void addIncident(IncidentRecord incident) {
        incidents.add(incident);
        recomputeStats();
    }

    public void addAll(List<IncidentRecord> newIncidents) {
        incidents.addAll(newIncidents);
        recomputeStats();
    }

    private void recomputeStats() {
        if (incidents.isEmpty()) {
            centroidX = 0;
            centroidY = 0;
            startTimeMs = 0;
            endTimeMs = 0;
            aggregateRiskScore = 0.0;
            return;
        }

        double sumX = 0;
        double sumY = 0;
        long minT = Long.MAX_VALUE;
        long maxT = Long.MIN_VALUE;
        double severityWeightSum = 0.0;

        for (IncidentRecord inc : incidents) {
            sumX += inc.getX();
            sumY += inc.getY();
            minT = Math.min(minT, inc.getTimestampMs());
            maxT = Math.max(maxT, inc.getTimestampMs());

            // Severity multiplier for risk
            double sevMultiplier = (inc.getSeverity() == CaseSeverity.CRITICAL) ? 3.0 :
                                   (inc.getSeverity() == CaseSeverity.HIGH) ? 2.0 : 1.0;
            severityWeightSum += sevMultiplier;
        }

        this.centroidX = sumX / incidents.size();
        this.centroidY = sumY / incidents.size();
        this.startTimeMs = minT;
        this.endTimeMs = maxT;
        // Normalized risk score saturated at 1.0
        this.aggregateRiskScore = Math.min(1.0, severityWeightSum / 10.0);
    }

    public int getClusterId() { return clusterId; }
    public List<IncidentRecord> getIncidents() { return incidents; }
    public double getCentroidX() { return centroidX; }
    public double getCentroidY() { return centroidY; }
    public long getStartTimeMs() { return startTimeMs; }
    public long getEndTimeMs() { return endTimeMs; }
    public double getAggregateRiskScore() { return aggregateRiskScore; }
    public int size() { return incidents.size(); }

    public double distanceTo(double x, double y) {
        double dx = this.centroidX - x;
        double dy = this.centroidY - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        return String.format("HotspotCluster#%d[size=%d, centroid=(%.1f, %.1f), time=[%d..%d], risk=%.2f]",
                clusterId, incidents.size(), centroidX, centroidY, startTimeMs, endTimeMs, aggregateRiskScore);
    }
}
