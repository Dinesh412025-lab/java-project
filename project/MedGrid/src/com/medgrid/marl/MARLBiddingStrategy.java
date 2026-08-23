package com.medgrid.marl;

import com.medgrid.model.Ambulance;
import com.medgrid.model.CaseSeverity;
import com.medgrid.model.EmergencyCase;
import com.medgrid.model.Hospital;
import com.medgrid.patterns.PatternService;
import com.medgrid.protocol.Bid;
import com.medgrid.routing.Dijkstra;
import com.medgrid.routing.Graph;
import com.medgrid.strategy.BiddingStrategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MARL-Augmented Bidding Strategy using Q-Learning and Spatiotemporal Risk awareness.
 */
public class MARLBiddingStrategy implements BiddingStrategy {
    private final QTable qTable;
    private final boolean trainingMode;
    private final Map<String, StateRepresentation> pendingStates;
    private final Map<String, BiddingAction> pendingActions;
    private final Map<String, Double> pendingRawEtas;
    private final Map<String, Integer> agentCaseCounts;

    public MARLBiddingStrategy(QTable qTable, boolean trainingMode) {
        this.qTable = qTable != null ? qTable : new QTable();
        this.trainingMode = trainingMode;
        this.pendingStates = new ConcurrentHashMap<>();
        this.pendingActions = new ConcurrentHashMap<>();
        this.pendingRawEtas = new ConcurrentHashMap<>();
        this.agentCaseCounts = new ConcurrentHashMap<>();
    }

    public MARLBiddingStrategy() {
        this(new QTable(), true);
    }

    @Override
    public Bid generateAmbulanceBid(Ambulance ambulance, EmergencyCase eCase, Graph graph, String announcementId) {
        if (!ambulance.isAvailable()) {
            return new Bid(ambulance.getId(), announcementId, Double.MAX_VALUE, false);
        }

        // 1. Calculate baseline physical shortest path ETA
        double rawEta = Dijkstra.computeShortestPathETA(graph, ambulance.getCurrentLocation(), eCase.getLocation());
        if (rawEta == Double.MAX_VALUE) {
            return new Bid(ambulance.getId(), announcementId, Double.MAX_VALUE, false);
        }

        // 2. Query Spatiotemporal Risk Score from PatternService
        double riskScore = PatternService.getInstance().getRiskScore(eCase.getLocation());

        // 3. Compute workload disparity
        int myCases = agentCaseCounts.getOrDefault(ambulance.getId(), 0);
        double avgCases = computeAverageCases();

        // 4. Construct discretized state tuple
        StateRepresentation.DistanceCategory distCat = StateRepresentation.categorizeDistance(rawEta);
        StateRepresentation.RiskCategory riskCat = StateRepresentation.categorizeRisk(riskScore);
        StateRepresentation.ContentionCategory contentionCat = StateRepresentation.ContentionCategory.LOW_CONTENTION; // standard default
        StateRepresentation.WorkloadCategory workloadCat = StateRepresentation.categorizeWorkload(myCases, avgCases);

        StateRepresentation state = new StateRepresentation(distCat, eCase.getSeverity(), riskCat, contentionCat, workloadCat);

        // 5. Select strategic bidding action from Q-Table
        BiddingAction action = qTable.selectAction(state, trainingMode);

        // Cache state and action for reward feedback
        String contextKey = ambulance.getId() + ":" + announcementId;
        pendingStates.put(contextKey, state);
        pendingActions.put(contextKey, action);
        pendingRawEtas.put(contextKey, rawEta);

        if (action == BiddingAction.PASS) {
            // Pass unless distance is very close
            if (distCat != StateRepresentation.DistanceCategory.NEAR) {
                return new Bid(ambulance.getId(), announcementId, Double.MAX_VALUE, false);
            }
        }

        // 6. Compute action-modified bid value
        double finalBidValue = action.computeModifiedBid(rawEta);
        return new Bid(ambulance.getId(), announcementId, finalBidValue, true);
    }

    @Override
    public Bid generateHospitalBid(Hospital hospital, EmergencyCase eCase, Graph graph, Ambulance ambulance, String announcementId) {
        if (hospital.getAvailableErBeds() == 0 || !hospital.hasSpecialty(eCase.getType())) {
            return new Bid(hospital.getId(), announcementId, Double.MAX_VALUE, false);
        }
        double eta = Dijkstra.computeShortestPathETA(graph, ambulance.getCurrentLocation(), hospital.getLocation());
        return new Bid(hospital.getId(), announcementId, eta, true);
    }

    /**
     * Feedback signal after auction resolution to update Q-values.
     */
    public void recordAuctionOutcome(String ambulanceId, String announcementId, boolean won,
                                     EmergencyCase eCase, StateRepresentation nextState) {
        String contextKey = ambulanceId + ":" + announcementId;
        StateRepresentation state = pendingStates.remove(contextKey);
        BiddingAction action = pendingActions.remove(contextKey);
        Double rawEta = pendingRawEtas.remove(contextKey);

        if (state == null || action == null || rawEta == null) {
            return;
        }

        if (won) {
            agentCaseCounts.merge(ambulanceId, 1, Integer::sum);
        }

        if (!trainingMode) return;

        // Calculate Multi-Objective Reward:
        // R = - speed_penalty + severity_urgency_bonus - fairness_penalty - risk_exposure_penalty
        double reward = 0.0;
        if (won) {
            // 1. Response time penalty (lower ETA is better)
            double normEta = Math.min(rawEta / 10.0, 3.0);
            reward -= 0.6 * normEta;

            // 2. Clinical urgency bonus for critical/high cases
            double severityBonus = (eCase.getSeverity() == CaseSeverity.CRITICAL) ? 3.0 :
                                   (eCase.getSeverity() == CaseSeverity.HIGH) ? 1.8 : 0.8;
            reward += severityBonus;

            // 3. Workload disparity penalty (penalize overloaded agents winning more)
            int myCount = agentCaseCounts.getOrDefault(ambulanceId, 0);
            double avgCount = computeAverageCases();
            double disparity = Math.abs(myCount - avgCount);
            if (disparity > 2.0) {
                reward -= 0.5 * (disparity - 2.0);
            }
        } else {
            // Did not win: small neutral or positive feedback if avoiding an overloaded/distant task
            if (state.getWorkloadCategory() == StateRepresentation.WorkloadCategory.OVERLOADED ||
                state.getDistanceCategory() == StateRepresentation.DistanceCategory.FAR) {
                reward += 0.4; // good to delegate
            } else if (state.getSeverity() == CaseSeverity.CRITICAL && state.getDistanceCategory() == StateRepresentation.DistanceCategory.NEAR) {
                reward -= 0.8; // missed an urgent nearby case
            }
        }

        qTable.update(state, action, reward, nextState);
    }

    private double computeAverageCases() {
        if (agentCaseCounts.isEmpty()) return 0.0;
        int sum = 0;
        for (int c : agentCaseCounts.values()) sum += c;
        return (double) sum / agentCaseCounts.size();
    }

    public QTable getQTable() { return qTable; }
    public Map<String, Integer> getAgentCaseCounts() { return agentCaseCounts; }
    public boolean isTrainingMode() { return trainingMode; }
}
