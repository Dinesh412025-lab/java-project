package com.medgrid.patterns;

import java.util.*;

/**
 * Spatiotemporal Density-Based Spatial Clustering of Applications with Noise (ST-DBSCAN).
 * Groups emergency incidents by both geographical proximity (spatial epsilon)
 * and temporal proximity (temporal epsilon).
 */
public class STDBSCAN {
    private final double epsSpatial;
    private final long epsTemporalMs;
    private final int minPts;

    public STDBSCAN(double epsSpatial, long epsTemporalMs, int minPts) {
        this.epsSpatial = epsSpatial;
        this.epsTemporalMs = epsTemporalMs;
        this.minPts = minPts;
    }

    public List<HotspotCluster> cluster(List<IncidentRecord> incidents) {
        List<HotspotCluster> clusters = new ArrayList<>();
        if (incidents == null || incidents.isEmpty()) {
            return clusters;
        }

        Set<IncidentRecord> visited = new HashSet<>();
        Set<IncidentRecord> clustered = new HashSet<>();
        int clusterCounter = 1;

        for (IncidentRecord p : incidents) {
            if (visited.contains(p)) continue;
            visited.add(p);

            List<IncidentRecord> neighbors = getSpatiotemporalNeighbors(p, incidents);
            if (neighbors.size() >= minPts) {
                HotspotCluster cluster = new HotspotCluster(clusterCounter++);
                cluster.addIncident(p);
                clustered.add(p);

                expandCluster(cluster, neighbors, incidents, visited, clustered);
                clusters.add(cluster);
            }
        }

        return clusters;
    }

    private void expandCluster(HotspotCluster cluster,
                               List<IncidentRecord> neighbors,
                               List<IncidentRecord> allIncidents,
                               Set<IncidentRecord> visited,
                               Set<IncidentRecord> clustered) {
        Queue<IncidentRecord> queue = new ArrayDeque<>(neighbors);

        while (!queue.isEmpty()) {
            IncidentRecord q = queue.poll();

            if (!visited.contains(q)) {
                visited.add(q);
                List<IncidentRecord> qNeighbors = getSpatiotemporalNeighbors(q, allIncidents);
                if (qNeighbors.size() >= minPts) {
                    for (IncidentRecord qn : qNeighbors) {
                        if (!visited.contains(qn)) {
                            queue.add(qn);
                        }
                    }
                }
            }

            if (!clustered.contains(q)) {
                cluster.addIncident(q);
                clustered.add(q);
            }
        }
    }

    private List<IncidentRecord> getSpatiotemporalNeighbors(IncidentRecord p, List<IncidentRecord> allIncidents) {
        List<IncidentRecord> neighbors = new ArrayList<>();
        for (IncidentRecord q : allIncidents) {
            if (p.spatialDistance(q) <= epsSpatial && p.temporalDistance(q) <= epsTemporalMs) {
                neighbors.add(q);
            }
        }
        return neighbors;
    }

    public double getEpsSpatial() { return epsSpatial; }
    public long getEpsTemporalMs() { return epsTemporalMs; }
    public int getMinPts() { return minPts; }
}
