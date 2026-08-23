package com.medgrid.routing;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Graph {
    private final Map<Node, List<Edge>> adjacencyList = new ConcurrentHashMap<>();

    public void addNode(Node node) {
        adjacencyList.putIfAbsent(node, Collections.synchronizedList(new ArrayList<>()));
    }

    public void addEdge(Node source, Node target, double weight) {
        addNode(source);
        addNode(target);
        adjacencyList.get(source).add(new Edge(source, target, weight));
        // Bidirectional for simplicity
        adjacencyList.get(target).add(new Edge(target, source, weight));
    }

    public List<Edge> getEdges(Node node) {
        return adjacencyList.getOrDefault(node, Collections.emptyList());
    }
    
    public Set<Node> getNodes() {
        return adjacencyList.keySet();
    }

    public void updateEdgeWeight(String sourceId, String targetId, double newWeight) {
        Node source = adjacencyList.keySet().stream().filter(n -> n.getId().equals(sourceId)).findFirst().orElse(null);
        Node target = adjacencyList.keySet().stream().filter(n -> n.getId().equals(targetId)).findFirst().orElse(null);
        
        if (source != null && target != null) {
            for (Edge edge : adjacencyList.get(source)) {
                if (edge.getTarget().equals(target)) {
                    edge.setWeight(newWeight);
                }
            }
            for (Edge edge : adjacencyList.get(target)) {
                if (edge.getTarget().equals(source)) {
                    edge.setWeight(newWeight);
                }
            }
        }
    }
}
