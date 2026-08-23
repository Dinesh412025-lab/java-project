package com.medgrid.marl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Negotiation & Convergence Logger for MARL contract net protocol analysis.
 */
public class NegotiationLogger {
    private static volatile NegotiationLogger instance;

    public static class NegotiationRound {
        public final long timestamp;
        public final String caseId;
        public final String caseSeverity;
        public final Map<String, Double> bids;
        public final Map<String, String> actions;
        public final String winnerId;
        public final double winningBid;
        public final double jainsFairness;

        public NegotiationRound(long timestamp, String caseId, String caseSeverity,
                                Map<String, Double> bids, Map<String, String> actions,
                                String winnerId, double winningBid, double jainsFairness) {
            this.timestamp = timestamp;
            this.caseId = caseId;
            this.caseSeverity = caseSeverity;
            this.bids = new HashMap<>(bids);
            this.actions = new HashMap<>(actions);
            this.winnerId = winnerId;
            this.winningBid = winningBid;
            this.jainsFairness = jainsFairness;
        }
    }

    private final List<NegotiationRound> rounds;
    private final Map<String, Integer> agentWinCounts;
    private final Path logFilePath = Paths.get("data/output/marl_negotiation_log.csv");

    public static NegotiationLogger getInstance() {
        if (instance == null) {
            synchronized (NegotiationLogger.class) {
                if (instance == null) {
                    instance = new NegotiationLogger();
                }
            }
        }
        return instance;
    }

    public NegotiationLogger() {
        this.rounds = new CopyOnWriteArrayList<>();
        this.agentWinCounts = new ConcurrentHashMap<>();
    }

    public synchronized void recordRound(String caseId, String caseSeverity,
                                         Map<String, Double> bids, Map<String, BiddingAction> actions,
                                         String winnerId, double winningBid) {
        if (winnerId != null && !winnerId.isEmpty()) {
            agentWinCounts.merge(winnerId, 1, Integer::sum);
        }

        double currentJain = FairnessMetric.computeJainsIndex(agentWinCounts);

        Map<String, String> actionNames = new HashMap<>();
        if (actions != null) {
            actions.forEach((k, v) -> actionNames.put(k, v.name()));
        }

        NegotiationRound round = new NegotiationRound(
                System.currentTimeMillis(), caseId, caseSeverity,
                bids != null ? bids : Collections.emptyMap(),
                actionNames, winnerId, winningBid, currentJain
        );
        rounds.add(round);
    }

    public List<NegotiationRound> getRounds() {
        return Collections.unmodifiableList(rounds);
    }

    public Map<String, Integer> getAgentWinCounts() {
        return Collections.unmodifiableMap(agentWinCounts);
    }

    public double getCurrentJainsIndex() {
        return FairnessMetric.computeJainsIndex(agentWinCounts);
    }

    public void exportToCsv() {
        try {
            if (!Files.exists(logFilePath.getParent())) {
                Files.createDirectories(logFilePath.getParent());
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(logFilePath.toFile()))) {
                bw.write("timestamp,case_id,severity,winner_id,winning_bid,jains_fairness,num_bidders\n");
                for (NegotiationRound r : rounds) {
                    bw.write(String.format("%d,%s,%s,%s,%.2f,%.4f,%d\n",
                            r.timestamp, r.caseId, r.caseSeverity, r.winnerId, r.winningBid, r.jainsFairness, r.bids.size()));
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to export MARL negotiation logs: " + e.getMessage());
        }
    }

    public void clear() {
        rounds.clear();
        agentWinCounts.clear();
    }
}
