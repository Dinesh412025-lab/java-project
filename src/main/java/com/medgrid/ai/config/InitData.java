package com.medgrid.ai.config;

import com.medgrid.ai.agent.AmbulanceAgent;
import com.medgrid.ai.agent.DispatcherAgent;
import com.medgrid.ai.agent.HospitalAgent;
import com.medgrid.ai.model.Ambulance;
import com.medgrid.ai.model.Hospital;
import com.medgrid.ai.routing.DijkstraRouter;
import com.medgrid.ai.routing.GraphNode;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;

@Configuration
public class InitData {

    @Bean
    public CommandLineRunner init(DijkstraRouter router, DispatcherAgent dispatcher) {
        return args -> {
            // 1. Setup Graph Nodes
            GraphNode n1 = new GraphNode("node1", 40.7128, -74.0060, new HashMap<>());
            GraphNode n2 = new GraphNode("node2", 40.7138, -74.0070, new HashMap<>());
            GraphNode n3 = new GraphNode("node3", 40.7148, -74.0080, new HashMap<>());
            
            n1.addEdge("node2", 2.5); // Example minutes/distance
            n2.addEdge("node1", 2.5);
            n2.addEdge("node3", 3.0);
            n3.addEdge("node2", 3.0);
            
            router.addNode(n1);
            router.addNode(n2);
            router.addNode(n3);

            // 2. Setup Hospital
            Hospital h1 = new Hospital();
            h1.setId("HOSP-001");
            h1.setName("Central City Hospital");
            h1.setLatitude(40.7148);
            h1.setLongitude(-74.0080);
            h1.setTotalIcuBeds(10);
            h1.setAvailableIcuBeds(3);
            h1.setSpecializations(Arrays.asList("CARDIAC ARREST", "TRAUMA"));
            
            HospitalAgent ha1 = new HospitalAgent(h1, router);
            dispatcher.registerHospitalAgent(ha1);

            // 3. Setup Ambulance
            Ambulance a1 = new Ambulance();
            a1.setId("AMB-101");
            a1.setLatitude(40.7138);
            a1.setLongitude(-74.0070);
            a1.setStatus(Ambulance.Status.FREE);
            a1.setType("ALS");

            AmbulanceAgent aa1 = new AmbulanceAgent(a1, router);
            dispatcher.registerAmbulanceAgent(aa1);
            
            System.out.println("MedGrid-AI Initialized with Sample Data");
        };
    }
}
