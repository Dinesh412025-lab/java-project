package com.medgrid.routing;

import java.util.*;
import com.medgrid.model.Location;
import com.medgrid.patterns.PatternService;

/**
 * Enhanced Dijkstra Routing Engine with Pattern-Weighted Dynamic Impedance.
 */
public class Dijkstra {

    public static class PathResult {
        private final List<Location> path;
        private final double totalCost;
        private final double physicalDistance;

        public PathResult(List<Location> path, double totalCost, double physicalDistance) {
            this.path = path;
            this.totalCost = totalCost;
            this.physicalDistance = physicalDistance;
        }

        public List<Location> getPath() { return path; }
        public double getTotalCost() { return totalCost; }
        public double getPhysicalDistance() { return physicalDistance; }
    }

    /**
     * Baseline shortest path computation using raw physical edge weights.
     */
    public static double computeShortestPathETA(Graph graph, Location startLoc, Location endLoc) {
        return computePathInternal(graph, startLoc, endLoc, 0.0).getTotalCost();
    }

    /**
     * Dynamic Pattern-Weighted ETA incorporating real-time spatiotemporal risk from PatternService.
     * Edge weight: W'(u, v) = W_0(u, v) * (1.0 + riskSensitivity * Risk(v))
     */
    public static double computePatternWeightedETA(Graph graph, Location startLoc, Location endLoc, double riskSensitivity) {
        return computePathInternal(graph, startLoc, endLoc, riskSensitivity).getTotalCost();
    }

    /**
     * Computes full path sequence and physical vs weighted distances.
     */
    public static PathResult computePatternWeightedPath(Graph graph, Location startLoc, Location endLoc, double riskSensitivity) {
        return computePathInternal(graph, startLoc, endLoc, riskSensitivity);
    }

    private static PathResult computePathInternal(Graph graph, Location startLoc, Location endLoc, double riskSensitivity) {
        Node startNode = new Node(startLoc);
        Node endNode = new Node(endLoc);

        if (startNode.equals(endNode)) {
            return new PathResult(Collections.singletonList(startLoc), 0.0, 0.0);
        }

        Map<Node, Double> distances = new HashMap<>();
        Map<Node, Double> physicalDistances = new HashMap<>();
        Map<Node, Node> predecessors = new HashMap<>();

        for (Node n : graph.getNodes()) {
            distances.put(n, Double.MAX_VALUE);
            physicalDistances.put(n, Double.MAX_VALUE);
        }
        distances.put(startNode, 0.0);
        physicalDistances.put(startNode, 0.0);

        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(distances::get));
        pq.add(startNode);
        Set<Node> visited = new HashSet<>();

        while (!pq.isEmpty()) {
            Node current = pq.poll();

            if (current.equals(endNode)) {
                break;
            }

            if (visited.contains(current)) continue;
            visited.add(current);

            for (Edge edge : graph.getEdges(current)) {
                Node neighbor = edge.getTarget();
                if (visited.contains(neighbor)) continue;

                double baseWeight = edge.getWeight();
                double effectiveWeight = baseWeight;

                // Modulate by real-time spatiotemporal risk if sensitivity > 0
                if (riskSensitivity > 0.0) {
                    double targetRisk = PatternService.getInstance().getRiskScore(neighbor.getLocation());
                    effectiveWeight = baseWeight * (1.0 + riskSensitivity * targetRisk);
                }

                double newDist = distances.get(current) + effectiveWeight;
                if (newDist < distances.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    distances.put(neighbor, newDist);
                    physicalDistances.put(neighbor, physicalDistances.get(current) + baseWeight);
                    predecessors.put(neighbor, current);
                    pq.add(neighbor);
                }
            }
        }

        if (distances.get(endNode) == Double.MAX_VALUE) {
            return new PathResult(Collections.emptyList(), Double.MAX_VALUE, Double.MAX_VALUE);
        }

        // Reconstruct path
        List<Location> path = new ArrayList<>();
        Node curr = endNode;
        while (curr != null) {
            path.add(curr.getLocation());
            curr = predecessors.get(curr);
        }
        Collections.reverse(path);

        return new PathResult(path, distances.get(endNode), physicalDistances.get(endNode));
    }
}
