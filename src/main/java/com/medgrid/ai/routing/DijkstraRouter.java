package com.medgrid.ai.routing;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DijkstraRouter {
    
    private final Map<String, GraphNode> graph = new HashMap<>();

    public void addNode(GraphNode node) {
        graph.put(node.getId(), node);
    }

    public double calculateShortestPath(String startId, String endId) {
        if (!graph.containsKey(startId) || !graph.containsKey(endId)) {
            return Double.MAX_VALUE;
        }

        Map<String, Double> distances = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingDouble(nd -> nd.distance));

        for (String nodeId : graph.keySet()) {
            distances.put(nodeId, Double.MAX_VALUE);
        }
        
        distances.put(startId, 0.0);
        pq.add(new NodeDistance(startId, 0.0));

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            
            if (current.nodeId.equals(endId)) {
                return current.distance;
            }

            if (current.distance > distances.get(current.nodeId)) {
                continue;
            }

            GraphNode node = graph.get(current.nodeId);
            for (Map.Entry<String, Double> edge : node.getEdges().entrySet()) {
                String neighborId = edge.getKey();
                double newDist = current.distance + edge.getValue();
                
                if (newDist < distances.get(neighborId)) {
                    distances.put(neighborId, newDist);
                    pq.add(new NodeDistance(neighborId, newDist));
                }
            }
        }
        return Double.MAX_VALUE;
    }

    private static class NodeDistance {
        String nodeId;
        double distance;
        NodeDistance(String nodeId, double distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }
    
    // Very simple method to find nearest node by lat/long (O(N) for prototype)
    public String findNearestNodeId(double lat, double lon) {
        String nearest = null;
        double minDistance = Double.MAX_VALUE;
        for (GraphNode node : graph.values()) {
            double dist = Math.pow(node.getLatitude() - lat, 2) + Math.pow(node.getLongitude() - lon, 2);
            if (dist < minDistance) {
                minDistance = dist;
                nearest = node.getId();
            }
        }
        return nearest;
    }
}
