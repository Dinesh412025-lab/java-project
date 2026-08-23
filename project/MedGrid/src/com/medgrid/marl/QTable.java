package com.medgrid.marl;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe Tabular Q-Learning Engine for Multi-Agent Reinforcement Learning.
 */
public class QTable {
    private final Map<StateRepresentation, Map<BiddingAction, Double>> table;
    private double learningRate;      // alpha
    private double discountFactor;    // gamma
    private double epsilon;           // exploration rate
    private final double epsilonMin;
    private final double epsilonDecay;
    private final Random random;
    private int totalUpdates = 0;

    public QTable(double learningRate, double discountFactor, double epsilonInitial, double epsilonMin, double epsilonDecay) {
        this.table = new ConcurrentHashMap<>();
        this.learningRate = learningRate;
        this.discountFactor = discountFactor;
        this.epsilon = epsilonInitial;
        this.epsilonMin = epsilonMin;
        this.epsilonDecay = epsilonDecay;
        this.random = new Random();
    }

    public QTable() {
        this(0.15, 0.90, 0.30, 0.05, 0.995);
    }

    /**
     * Initializes default Q-values for a given state.
     */
    private Map<BiddingAction, Double> getOrInitStateActions(StateRepresentation state) {
        return table.computeIfAbsent(state, k -> {
            Map<BiddingAction, Double> actions = new EnumMap<>(BiddingAction.class);
            for (BiddingAction a : BiddingAction.values()) {
                // Initialize TRUE_ETA with slight optimistic bias
                actions.put(a, a == BiddingAction.TRUE_ETA ? 0.5 : 0.0);
            }
            return actions;
        });
    }

    /**
     * Epsilon-greedy action selection for the given state.
     */
    public BiddingAction selectAction(StateRepresentation state, boolean explore) {
        Map<BiddingAction, Double> actionValues = getOrInitStateActions(state);

        if (explore && random.nextDouble() < epsilon) {
            // Explore: pick random action
            BiddingAction[] actions = BiddingAction.values();
            return actions[random.nextInt(actions.length)];
        }

        // Exploit: pick best action with greedy tie-breaking
        BiddingAction bestAction = BiddingAction.TRUE_ETA;
        double maxQ = -Double.MAX_VALUE;

        for (Map.Entry<BiddingAction, Double> entry : actionValues.entrySet()) {
            if (entry.getValue() > maxQ) {
                maxQ = entry.getValue();
                bestAction = entry.getKey();
            }
        }
        return bestAction;
    }

    /**
     * Bellman Q-value update:
     * Q(s, a) = Q(s, a) + alpha * [ r + gamma * max_a' Q(s', a') - Q(s, a) ]
     */
    public synchronized void update(StateRepresentation state, BiddingAction action, double reward, StateRepresentation nextState) {
        Map<BiddingAction, Double> currentActions = getOrInitStateActions(state);
        double currentQ = currentActions.getOrDefault(action, 0.0);

        double maxNextQ = 0.0;
        if (nextState != null) {
            Map<BiddingAction, Double> nextActions = getOrInitStateActions(nextState);
            maxNextQ = nextActions.values().stream().max(Double::compare).orElse(0.0);
        }

        double newQ = currentQ + learningRate * (reward + (discountFactor * maxNextQ) - currentQ);
        currentActions.put(action, newQ);

        // Decay exploration rate
        epsilon = Math.max(epsilonMin, epsilon * epsilonDecay);
        totalUpdates++;
    }

    public double getQ(StateRepresentation state, BiddingAction action) {
        return getOrInitStateActions(state).getOrDefault(action, 0.0);
    }

    public double getEpsilon() { return epsilon; }
    public int getTotalUpdates() { return totalUpdates; }
    public int getStateCount() { return table.size(); }

    public Map<StateRepresentation, Map<BiddingAction, Double>> getSnapshot() {
        return Collections.unmodifiableMap(table);
    }

    public void setEpsilon(double eps) {
        this.epsilon = eps;
    }

    public void reset() {
        table.clear();
        totalUpdates = 0;
        epsilon = 0.30;
    }
}
