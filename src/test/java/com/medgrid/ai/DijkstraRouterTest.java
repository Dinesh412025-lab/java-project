package com.medgrid.ai.routing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;

class DijkstraRouterTest {

    @Test
    void testShortestPath() {
        DijkstraRouter router = new DijkstraRouter();
        
        GraphNode n1 = new GraphNode("A", 0, 0, new HashMap<>());
        GraphNode n2 = new GraphNode("B", 0, 0, new HashMap<>());
        GraphNode n3 = new GraphNode("C", 0, 0, new HashMap<>());
        
        n1.addEdge("B", 5.0);
        n2.addEdge("C", 2.0);
        n1.addEdge("C", 10.0);
        
        router.addNode(n1);
        router.addNode(n2);
        router.addNode(n3);
        
        double dist = router.calculateShortestPath("A", "C");
        assertEquals(7.0, dist);
    }
}
