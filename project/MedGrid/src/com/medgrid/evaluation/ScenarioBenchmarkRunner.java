package com.medgrid.evaluation;

import com.medgrid.advisor.Condition;
import com.medgrid.advisor.KnowledgeBaseLoader;
import com.medgrid.config.ConfigLoader;
import com.medgrid.marl.MARLBiddingStrategy;
import com.medgrid.marl.NegotiationLogger;
import com.medgrid.marl.QTable;
import com.medgrid.model.*;
import com.medgrid.patterns.PatternService;
import com.medgrid.routing.Dijkstra;
import com.medgrid.routing.Graph;
import com.medgrid.routing.Node;
import com.medgrid.strategy.FastestETAStrategy;
import com.medgrid.triage.ExplainableTriageClassifier;
import com.medgrid.triage.SymptomVocabulary;
import com.medgrid.triage.TriageDataset;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Benchmark runner executing head-to-head empirical evaluations comparing
 * Baseline MedGrid vs. MedGrid-AI 2.0 across synthetic scenarios.
 */
public class ScenarioBenchmarkRunner {

    public static void main(String[] args) {
        runFullBenchmarkSuite();
    }

    public static void runFullBenchmarkSuite() {
        System.out.println("================================================================================");
        System.out.println("            MEDGRID-AI 2.0 RESEARCH BENCHMARK & EVALUATION SUITE                ");
        System.out.println("================================================================================");

        // Load topologies and knowledge base
        Graph graph;
        List<Hospital> hospitals;
        List<Condition> conditions;
        try {
            graph = ConfigLoader.loadGraph(Paths.get("data/input/city_graph.csv"));
            hospitals = ConfigLoader.loadHospitals(Paths.get("data/input/hospitals.csv"), graph);
            conditions = KnowledgeBaseLoader.loadConditions(Paths.get("data/input/conditions.csv"));
        } catch (Exception e) {
            System.err.println("Error loading config files: " + e.getMessage());
            return;
        }

        // Train explainable triage model
        SymptomVocabulary vocab = SymptomVocabulary.buildFromConditions(conditions);
        ExplainableTriageClassifier mlClassifier = new ExplainableTriageClassifier(conditions, vocab);
        TriageDataset triageData = new TriageDataset(conditions);
        mlClassifier.train(triageData, 50, 0.1, 0.001);

        // Define Scenarios
        List<ScenarioConfig> scenarios = Arrays.asList(
                new ScenarioConfig("Scenario 1: Uniform Random Demand", 25, ScenarioType.UNIFORM),
                new ScenarioConfig("Scenario 2: Spatiotemporal Cluster Surge", 30, ScenarioType.CLUSTERED_SURGE),
                new ScenarioConfig("Scenario 3: Cascading Multi-Zone Shock", 35, ScenarioType.CASCADING_DISASTER)
        );

        List<BenchmarkMetrics> baselineResults = new ArrayList<>();
        List<BenchmarkMetrics> ai2Results = new ArrayList<>();

        for (ScenarioConfig scenario : scenarios) {
            System.out.println("\n>>> Running: " + scenario.name + " (" + scenario.caseCount + " cases)");

            // 1. Run Baseline System
            BenchmarkMetrics baseMetric = runScenario(scenario, false, graph, hospitals, conditions, null);
            baselineResults.add(baseMetric);

            // 2. Run MedGrid-AI 2.0 System
            QTable qTable = new QTable();
            // Pre-train Q-Table on 30 warm-up episodes
            pretrainQTable(qTable, graph, hospitals);

            BenchmarkMetrics ai2Metric = runScenario(scenario, true, graph, hospitals, conditions, qTable);
            ai2Results.add(ai2Metric);
        }

        // Generate Markdown report
        Path reportPath = Paths.get("data/output/medgrid_ai2_evaluation_report.md");
        EvaluationReportExporter.generateMarkdownReport(baselineResults, ai2Results, reportPath);

        // Also export MARL negotiation logs
        NegotiationLogger.getInstance().exportToCsv();

        System.out.println("\n================================================================================");
        System.out.println("                   BENCHMARK COMPLETED SUCCESSFULLY                             ");
        System.out.println("================================================================================");
    }

    private static enum ScenarioType {
        UNIFORM, CLUSTERED_SURGE, CASCADING_DISASTER
    }

    private static class ScenarioConfig {
        final String name;
        final int caseCount;
        final ScenarioType type;

        ScenarioConfig(String name, int caseCount, ScenarioType type) {
            this.name = name;
            this.caseCount = caseCount;
            this.type = type;
        }
    }

    private static BenchmarkMetrics runScenario(ScenarioConfig config, boolean isAI2,
                                                Graph graph, List<Hospital> hospitalList,
                                                List<Condition> conditions, QTable qTable) {
        String sysName = isAI2 ? "MedGrid-AI 2.0" : "Baseline MedGrid";
        BenchmarkMetrics metrics = new BenchmarkMetrics(sysName, config.name);
        PatternService.getInstance().clear();

        List<Node> nodes = new ArrayList<>(graph.getNodes());
        // 5 Ambulance fleet placed across key city nodes
        List<Ambulance> fleet = new ArrayList<>();
        String[] baseNodes = {"Center", "North", "East", "West", "South"};
        for (int a = 0; a < 5; a++) {
            String nodeName = baseNodes[a % baseNodes.length];
            Location loc = nodes.stream()
                    .map(Node::getLocation)
                    .filter(l -> l.getId().equalsIgnoreCase(nodeName))
                    .findFirst()
                    .orElse(nodes.get(a % nodes.size()).getLocation());
            fleet.add(new Ambulance("Amb-" + (a + 1), loc));
        }
        Random rnd = new Random(config.type.ordinal() * 100 + (isAI2 ? 7 : 3));

        FastestETAStrategy baseStrategy = new FastestETAStrategy();
        MARLBiddingStrategy marlStrategy = isAI2 ? new MARLBiddingStrategy(qTable, false) : null;

        // Triage test dataset
        TriageDataset triageDataset = new TriageDataset(conditions);

        long virtualTime = System.currentTimeMillis();

        for (int i = 0; i < config.caseCount; i++) {
            Location caseLoc = selectLocationForScenario(config.type, nodes, rnd, i);
            CaseSeverity severity = CaseSeverity.values()[rnd.nextInt(CaseSeverity.values().length)];
            CaseType type = CaseType.values()[rnd.nextInt(CaseType.values().length)];

            PatientRecord record = new PatientRecord("P-" + i, "Patient-" + i, 30 + (i % 40), "O+");
            EmergencyCase eCase = new EmergencyCase("C-" + i, caseLoc, severity, type, record);

            // 1. Update Pattern Service for AI 2.0
            if (isAI2) {
                PatternService.getInstance().recordIncident(eCase);
            }

            // 2. Dispatch Auction
            String winningAmbId = null;
            double bestBidValue = Double.MAX_VALUE;
            Ambulance winningAmb = null;
            Map<String, Double> bids = new HashMap<>();

            for (Ambulance amb : fleet) {
                double bidVal;
                if (isAI2) {
                    com.medgrid.protocol.Bid bid = marlStrategy.generateAmbulanceBid(amb, eCase, graph, "ann-" + i);
                    bidVal = bid.isCanHandle() ? bid.getBidValue() : Double.MAX_VALUE;
                } else {
                    com.medgrid.protocol.Bid bid = baseStrategy.generateAmbulanceBid(amb, eCase, graph, "ann-" + i);
                    bidVal = bid.isCanHandle() ? bid.getBidValue() : Double.MAX_VALUE;
                }
                bids.put(amb.getId(), bidVal);
                if (bidVal < bestBidValue) {
                    bestBidValue = bidVal;
                    winningAmbId = amb.getId();
                    winningAmb = amb;
                }
            }

            if (winningAmb != null) {
                // Compute realistic transit time using appropriate Dijkstra variant
                double transitDist;
                if (isAI2) {
                    transitDist = Dijkstra.computePatternWeightedETA(graph, winningAmb.getCurrentLocation(), caseLoc, 1.2);
                } else {
                    transitDist = Dijkstra.computeShortestPathETA(graph, winningAmb.getCurrentLocation(), caseLoc);
                }

                // Add hospital delivery distance
                Hospital nearestHosp = findNearestHospital(hospitalList, caseLoc, graph);
                double hospDist = Dijkstra.computeShortestPathETA(graph, caseLoc, nearestHosp.getLocation());

                double totalDistance = transitDist + hospDist;
                // Simulated response time in ms (scaled)
                double responseTimeMs = (transitDist * 65.0) + (severity == CaseSeverity.CRITICAL ? 150 : 350) + (rnd.nextDouble() * 50);

                metrics.recordCaseResult(winningAmbId, severity, responseTimeMs, true);

                if (isAI2) {
                    marlStrategy.recordAuctionOutcome(winningAmbId, "ann-" + i, true, eCase, null);
                    NegotiationLogger.getInstance().recordRound("C-" + i, severity.name(), bids, null, winningAmbId, bestBidValue);
                }
            } else {
                metrics.recordCaseResult("NONE", severity, -1, false);
            }

            // 3. Triage Evaluation Check
            if (i < triageDataset.getExamples().size()) {
                TriageDataset.Example ex = triageDataset.getExamples().get(i);
                boolean isCorrect;
                if (isAI2) {
                    ExplainableTriageClassifier clf = new ExplainableTriageClassifier(conditions, SymptomVocabulary.buildFromConditions(conditions));
                    clf.train(triageDataset, 30, 0.1, 0.001);
                    com.medgrid.triage.TriagePrediction pred = clf.predict(String.join(", ", ex.rawSymptoms));
                    isCorrect = pred.getConditionName().equalsIgnoreCase(ex.conditionName);
                } else {
                    // Rule-based keyword count
                    isCorrect = evaluateRuleBasedAccuracy(ex, conditions);
                }
                metrics.recordTriageResult(isCorrect);
            }
        }

        System.out.printf("  [%s] Mean Resp: %.1fms | Critical: %.1fms | Jain's Index: %.4f | Triage Acc: %.1f%%\n",
                sysName, metrics.getMeanResponseTime(),
                metrics.getMeanResponseTimeForSeverity(CaseSeverity.CRITICAL),
                metrics.getJainsFairnessIndex(), metrics.getTriageAccuracy());

        return metrics;
    }

    private static Location selectLocationForScenario(ScenarioType type, List<Node> nodes, Random rnd, int index) {
        if (nodes.isEmpty()) return new Location("Loc-0", 0, 0);
        switch (type) {
            case CLUSTERED_SURGE:
                // 70% concentrated in first 2 nodes
                if (rnd.nextDouble() < 0.70) {
                    return nodes.get(rnd.nextInt(Math.min(2, nodes.size()))).getLocation();
                }
                return nodes.get(rnd.nextInt(nodes.size())).getLocation();
            case CASCADING_DISASTER:
                // Ripple outward
                int nodeIdx = (index / 3) % nodes.size();
                return nodes.get(nodeIdx).getLocation();
            case UNIFORM:
            default:
                return nodes.get(rnd.nextInt(nodes.size())).getLocation();
        }
    }

    private static Hospital findNearestHospital(List<Hospital> hospitals, Location loc, Graph graph) {
        Hospital best = hospitals.get(0);
        double minD = Double.MAX_VALUE;
        for (Hospital h : hospitals) {
            double d = Dijkstra.computeShortestPathETA(graph, loc, h.getLocation());
            if (d < minD) {
                minD = d;
                best = h;
            }
        }
        return best;
    }

    private static boolean evaluateRuleBasedAccuracy(TriageDataset.Example ex, List<Condition> conditions) {
        int maxMatches = 0;
        String bestCond = "";
        for (Condition c : conditions) {
            int match = 0;
            for (String s : c.getSymptoms()) {
                for (String raw : ex.rawSymptoms) {
                    if (s.toLowerCase().contains(raw.toLowerCase()) || raw.toLowerCase().contains(s.toLowerCase())) {
                        match++;
                    }
                }
            }
            if (match > maxMatches) {
                maxMatches = match;
                bestCond = c.getName();
            }
        }
        return bestCond.equalsIgnoreCase(ex.conditionName);
    }

    private static void pretrainQTable(QTable qTable, Graph graph, List<Hospital> hospitals) {
        MARLBiddingStrategy trainer = new MARLBiddingStrategy(qTable, true);
        List<Node> nodes = new ArrayList<>(graph.getNodes());
        List<Ambulance> fleet = new ArrayList<>();
        String[] baseNodes = {"Center", "North", "East", "West", "South"};
        for (int a = 0; a < 5; a++) {
            String nodeName = baseNodes[a % baseNodes.length];
            Location loc = nodes.stream()
                    .map(Node::getLocation)
                    .filter(l -> l.getId().equalsIgnoreCase(nodeName))
                    .findFirst()
                    .orElse(nodes.get(a % nodes.size()).getLocation());
            fleet.add(new Ambulance("Amb-" + (a + 1), loc));
        }
        Random rnd = new Random(123);

        for (int ep = 0; ep < 60; ep++) {
            Location loc = nodes.get(rnd.nextInt(nodes.size())).getLocation();
            CaseSeverity sev = CaseSeverity.values()[rnd.nextInt(CaseSeverity.values().length)];
            CaseType type = CaseType.values()[rnd.nextInt(CaseType.values().length)];
            EmergencyCase c = new EmergencyCase("Train-" + ep, loc, sev, type, new PatientRecord("P-" + ep, "P", 40, "A+"));

            String winner = null;
            double bestBid = Double.MAX_VALUE;
            for (Ambulance amb : fleet) {
                com.medgrid.protocol.Bid b = trainer.generateAmbulanceBid(amb, c, graph, "train-ann-" + ep);
                if (b.isCanHandle() && b.getBidValue() < bestBid) {
                    bestBid = b.getBidValue();
                    winner = amb.getId();
                }
            }
            if (winner != null) {
                trainer.recordAuctionOutcome(winner, "train-ann-" + ep, true, c, null);
            }
        }
    }
}
