package com.medgrid.patterns;

import com.medgrid.model.EmergencyCase;
import com.medgrid.model.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for spatiotemporal pattern mining, hotspot detection, and zone risk evaluation.
 */
public class PatternService {
    private static volatile PatternService instance;

    private final List<IncidentRecord> incidentHistory;
    private final Map<String, Double> cachedNodeRiskScores;
    private final STDBSCAN stDbScan;
    private final HawkesIntensityEstimator hawkesEstimator;
    private volatile List<HotspotCluster> activeHotspots;
    private long lastClusteringTimeMs = 0;
    private static final long CLUSTERING_INTERVAL_MS = 2000; // recluster every 2 seconds

    public static PatternService getInstance() {
        if (instance == null) {
            synchronized (PatternService.class) {
                if (instance == null) {
                    instance = new PatternService();
                }
            }
        }
        return instance;
    }

    public PatternService() {
        this.incidentHistory = new CopyOnWriteArrayList<>();
        this.cachedNodeRiskScores = new ConcurrentHashMap<>();
        // Spatial radius = 4.0 km, Temporal window = 30 seconds (30000ms), MinPts = 2
        this.stDbScan = new STDBSCAN(4.5, 30000, 2);
        this.hawkesEstimator = new HawkesIntensityEstimator();
        this.activeHotspots = new ArrayList<>();
    }

    /**
     * Ingest an emergency case from the live dispatch / simulation.
     */
    public void recordIncident(EmergencyCase eCase) {
        if (eCase == null) return;
        IncidentRecord rec = new IncidentRecord(
                eCase.getCaseId(),
                eCase.getLocation(),
                eCase.getCreationTime(),
                eCase.getSeverity(),
                eCase.getType()
        );
        incidentHistory.add(rec);
        refreshPatternsIfNeeded();
    }

    /**
     * Ingest an explicit incident record (e.g. from historical logs).
     */
    public void recordIncident(IncidentRecord record) {
        if (record != null) {
            incidentHistory.add(record);
            refreshPatternsIfNeeded();
        }
    }

    /**
     * Batch ingest historical incidents.
     */
    public void ingestHistory(List<IncidentRecord> records) {
        if (records != null) {
            incidentHistory.addAll(records);
            recluster();
        }
    }

    private void refreshPatternsIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastClusteringTimeMs >= CLUSTERING_INTERVAL_MS) {
            recluster();
        }
    }

    /**
     * Re-runs ST-DBSCAN clustering over recent sliding window.
     */
    public synchronized void recluster() {
        long now = System.currentTimeMillis();
        lastClusteringTimeMs = now;

        // Filter for incidents in the sliding temporal window (e.g. last 2 minutes)
        long windowStart = now - 120000;
        List<IncidentRecord> recent = new ArrayList<>();
        for (IncidentRecord r : incidentHistory) {
            if (r.getTimestampMs() >= windowStart) {
                recent.add(r);
            }
        }

        if (recent.isEmpty()) {
            activeHotspots = Collections.emptyList();
            cachedNodeRiskScores.clear();
            return;
        }

        this.activeHotspots = stDbScan.cluster(recent);
    }

    /**
     * Returns the dynamic risk score R(loc) in [0.0, 1.0] for a given Location.
     */
    public double getRiskScore(Location location) {
        if (location == null) return 0.0;
        return getRiskScore(location.getId(), location.getX(), location.getY());
    }

    /**
     * Computes the compound risk score combining Hawkes temporal intensity
     * and spatial cluster density.
     */
    public double getRiskScore(String locationId, double x, double y) {
        long now = System.currentTimeMillis();

        // 1. Hawkes process intensity risk
        double hawkesRisk = hawkesEstimator.computeNormalizedRisk(x, y, now, incidentHistory);

        // 2. ST-DBSCAN Cluster proximity risk bonus
        double clusterBonus = 0.0;
        for (HotspotCluster cluster : activeHotspots) {
            double dist = cluster.distanceTo(x, y);
            if (dist <= stDbScan.getEpsSpatial()) {
                double proximityWeight = 1.0 - (dist / stDbScan.getEpsSpatial());
                clusterBonus += cluster.getAggregateRiskScore() * proximityWeight * 0.35;
            }
        }

        double totalRisk = Math.min(1.0, hawkesRisk + clusterBonus);
        if (locationId != null) {
            cachedNodeRiskScores.put(locationId, totalRisk);
        }
        return totalRisk;
    }

    public List<HotspotCluster> getActiveHotspots() {
        return Collections.unmodifiableList(activeHotspots);
    }

    public List<IncidentRecord> getIncidentHistory() {
        return Collections.unmodifiableList(incidentHistory);
    }

    public void clear() {
        incidentHistory.clear();
        cachedNodeRiskScores.clear();
        activeHotspots.clear();
    }
}
