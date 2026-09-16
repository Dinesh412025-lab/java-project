package com.medgrid.ai.routing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphNode {
    private String id;
    private double latitude;
    private double longitude;
    
    // Neighbor ID -> Distance (or time)
    private Map<String, Double> edges = new HashMap<>();
    
    public void addEdge(String targetId, double distance) {
        edges.put(targetId, distance);
    }
}
