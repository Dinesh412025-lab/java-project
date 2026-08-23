package com.medgrid;

import com.medgrid.model.*;
import com.medgrid.routing.*;
import com.medgrid.strategy.*;
import com.medgrid.agent.*;
import com.medgrid.simulation.*;
import com.medgrid.web.WebServer;
import com.medgrid.config.*;
import com.medgrid.persistence.*;
import com.medgrid.analytics.*;
import com.medgrid.advisor.*;
import com.medgrid.patterns.*;
import com.medgrid.marl.*;
import com.medgrid.triage.*;
import com.medgrid.evaluation.*;

import java.nio.file.Paths;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("--benchmark")) {
            ScenarioBenchmarkRunner.runFullBenchmarkSuite();
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("--triage")) {
            runTriageDemo();
            return;
        }

        // 0. Historical Analytics
        HistoricalAnalytics.computeAndPrintGlobalMetrics();
        
        try {
            List<Condition> conditions = KnowledgeBaseLoader.loadConditions(Paths.get("data/input/conditions.csv"));
            SymptomAdvisor advisor = new SymptomAdvisor(conditions);

            // Run quick Explainable Triage test
            System.out.println("\n[MedGrid-AI 2.0] Explainable ML Triage Model Initialized.");
            TriagePrediction samplePred = advisor.triageWithAI("chest pain, shortness of breath, sweating, nausea");
            System.out.println(samplePred.getFormattedExplanation());

            // Run Research Benchmark Evaluation Suite
            System.out.println("\n=== Executing MedGrid-AI 2.0 Research Benchmark Suite ===");
            ScenarioBenchmarkRunner.runFullBenchmarkSuite();

            System.out.println("\n=== Starting MedGrid-AI 2.0 Live Simulation & Web Server ===");
            runSimulation();
            
            // Keep main thread alive for web dashboard
            while (true) {
                Thread.sleep(10000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runTriageDemo() {
        try {
            List<Condition> conditions = KnowledgeBaseLoader.loadConditions(Paths.get("data/input/conditions.csv"));
            SymptomAdvisor advisor = new SymptomAdvisor(conditions);
            String[] testCases = {
                    "chest pain, shortness of breath, sweating",
                    "face drooping, arm weakness, speech difficulty",
                    "runny nose, sore throat, sneezing",
                    "tooth pain, swollen gums, sensitivity",
                    "severe burn, charred skin, large area"
            };
            System.out.println("\n=======================================================");
            System.out.println("     EXPLAINABLE CLINICAL TRIAGE CLASSIFIER DEMO      ");
            System.out.println("=======================================================");
            for (String tc : testCases) {
                System.out.println("\nInput Symptoms: \"" + tc + "\"");
                TriagePrediction pred = advisor.triageWithAI(tc);
                System.out.println(pred.getFormattedExplanation());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runSimulation() {
        try {
            // 1. Setup Graph City
            Graph graph = ConfigLoader.loadGraph(Paths.get("data/input/city_graph.csv"));

            // 2. Setup MARL Strategy with Q-Learning
            QTable qTable = new QTable();
            BiddingStrategy strategy = new MARLBiddingStrategy(qTable, true);

            // 3. Setup Hospitals
            List<Hospital> hospitals = ConfigLoader.loadHospitals(Paths.get("data/input/hospitals.csv"), graph);
            List<HospitalAgent> hospitalAgents = new ArrayList<>();
            for (Hospital h : hospitals) {
                hospitalAgents.add(new HospitalAgent(h, strategy, graph));
            }

            // 4. Setup Ambulances
            List<AmbulanceAgent> ambulanceAgents = new ArrayList<>();
            Location center = new Location("Center", 0, 0);
            for (Node n : graph.getNodes()) {
                if (n.getLocation().getId().equals("Center")) center = n.getLocation();
            }
            for (int i = 1; i <= 5; i++) {
                Ambulance amb = new Ambulance("Amb-" + i, center);
                ambulanceAgents.add(new AmbulanceAgent(amb, strategy, graph, hospitalAgents));
            }

            // 5. Setup Dispatcher
            DispatchAgent dispatchAgent = new DispatchAgent(ambulanceAgents, true);

            // 6. Setup Simulation
            TrafficManager trafficManager = new TrafficManager(graph);
            EmergencyCaseFactory factory = new EmergencyCaseFactory(graph);
            List<ScenarioRecord> scenarios = ConfigLoader.loadScenarios(Paths.get("data/input/emergency_scenarios.csv"));
            EventGenerator eventGenerator = new EventGenerator(dispatchAgent, factory, 1000, scenarios);
            SimulationEngine engine = new SimulationEngine(hospitalAgents, ambulanceAgents, dispatchAgent, eventGenerator, trafficManager);

            // 7. Start Web Server
            WebServer webServer = new WebServer(8080, dispatchAgent, graph);
            webServer.start();

            // 8. Run
            engine.start();
            engine.waitForCompletion();
            engine.shutdown();
            
            System.out.println("Simulation run completed successfully. Web server remains alive at http://localhost:8080");
            
            // 9. Shutdown persistence and generate report
            DataExporter.getInstance().generateUtilizationReport(hospitals);
            DataExporter.getInstance().shutdown();

            System.out.println("Simulation run completed successfully. Final metrics displayed above.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
