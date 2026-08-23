package com.medgrid.config;

import com.medgrid.model.CaseType;
import com.medgrid.model.Hospital;
import com.medgrid.model.Location;
import com.medgrid.routing.Graph;
import com.medgrid.routing.Node;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ConfigLoader {

    public static Graph loadGraph(Path path) throws Exception {
        Graph graph = new Graph();
        Map<String, Node> nodes = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            boolean isEdgeSection = false;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("NodeName") || line.startsWith("Source")) continue;
                if (line.equals("---")) {
                    isEdgeSection = true;
                    continue;
                }

                String[] parts = line.split(",");
                if (!isEdgeSection) {
                    // NodeName,X,Y
                    String name = parts[0];
                    int x = (int) Double.parseDouble(parts[1]);
                    int y = (int) Double.parseDouble(parts[2]);
                    Node node = new Node(new Location(name, x, y));
                    nodes.put(name, node);
                } else {
                    // Source,Dest,Distance
                    String srcName = parts[0];
                    String destName = parts[1];
                    double dist = Double.parseDouble(parts[2]);
                    
                    Node src = nodes.get(srcName);
                    Node dest = nodes.get(destName);
                    if (src != null && dest != null) {
                        graph.addEdge(src, dest, dist);
                    }
                }
            }
        }
        return graph;
    }

    public static List<Hospital> loadHospitals(Path path, Graph graph) throws Exception {
        List<Hospital> hospitals = new ArrayList<>();
        // We need location instances from the graph. We'll search for the node by location name.
        Map<String, Location> locMap = new HashMap<>();
        for (Node n : graph.getNodes()) {
            locMap.put(n.getLocation().getId(), n.getLocation());
        }

        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("HospitalName")) continue;
                
                String[] parts = line.split(",");
                String name = parts[0];
                String locName = parts[1];
                int er = Integer.parseInt(parts[2]);
                int icu = Integer.parseInt(parts[3]);
                String[] specs = parts[4].split(";");
                
                Set<CaseType> specialties = new HashSet<>();
                for (String s : specs) {
                    specialties.add(CaseType.valueOf(s));
                }

                Location loc = locMap.get(locName);
                if (loc != null) {
                    hospitals.add(new Hospital(name, loc, er, icu, specialties));
                }
            }
        }
        return hospitals;
    }

    public static List<ScenarioRecord> loadScenarios(Path path) throws Exception {
        List<ScenarioRecord> scenarios = new ArrayList<>();
        if (!Files.exists(path)) return scenarios;

        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("TimeOffsetMs")) continue;
                
                String[] parts = line.split(",");
                long time = Long.parseLong(parts[0]);
                String loc = parts[1];
                String sev = parts[2];
                CaseType type = CaseType.valueOf(parts[3]);
                
                scenarios.add(new ScenarioRecord(time, loc, sev, type));
            }
        }
        return scenarios;
    }
}
