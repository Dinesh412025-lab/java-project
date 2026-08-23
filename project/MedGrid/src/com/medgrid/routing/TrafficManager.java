package com.medgrid.routing;

import com.medgrid.monitoring.MonitoringService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TrafficManager {
    private final Graph graph;
    private final ScheduledExecutorService scheduler;
    private final Random random;

    public TrafficManager(Graph graph) {
        this.graph = graph;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.random = new Random();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::simulateTrafficEvents, 10, 20, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    private void simulateTrafficEvents() {
        List<Node> nodes = new ArrayList<>(graph.getNodes());
        if (nodes.size() < 2) return;

        // Select a random edge (by selecting two connected or random nodes)
        Node source = nodes.get(random.nextInt(nodes.size()));
        List<Edge> edges = graph.getEdges(source);
        if (edges.isEmpty()) return;

        Edge selectedEdge = edges.get(random.nextInt(edges.size()));
        Node target = selectedEdge.getTarget();

        double originalWeight = selectedEdge.getWeight();
        
        // Either heavy traffic (weight * 3) or Blocked (weight * 10)
        boolean isBlocked = random.nextBoolean();
        double newWeight = isBlocked ? originalWeight * 10 : originalWeight * 3;
        
        graph.updateEdgeWeight(source.getId(), target.getId(), newWeight);
        
        String eventType = isBlocked ? "ROAD_BLOCKED" : "HEAVY_TRAFFIC";
        String message = eventType + ": " + source.getId() + " to " + target.getId() + " (Delay increased!)";
        MonitoringService.getInstance().logEvent("TRAFFIC_ALERT", message);
        
        // Reset after 10 seconds
        scheduler.schedule(() -> {
            graph.updateEdgeWeight(source.getId(), target.getId(), originalWeight);
            MonitoringService.getInstance().logEvent("TRAFFIC_CLEAR", "Road cleared: " + source.getId() + " to " + target.getId());
        }, 10, TimeUnit.SECONDS);
    }
}
