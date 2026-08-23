package com.medgrid.marl;

/**
 * Discrete bidding action space for ambulance agents in MARL negotiation.
 */
public enum BiddingAction {
    /**
     * Aggressive bid: discounts ETA by 15% to win contested dispatch.
     */
    AGGRESSIVE(0.85, "Aggressive Bid (-15% ETA modifier)"),

    /**
     * Standard true-ETA bid (identical to baseline Contract Net Protocol).
     */
    TRUE_ETA(1.00, "True ETA Bid (Baseline standard)"),

    /**
     * Conservative bid: inflates ETA by 25% when overloaded or distant.
     */
    CONSERVATIVE(1.25, "Conservative Bid (+25% ETA modifier)"),

    /**
     * Zone-Preserving bid: inflates ETA by 60% if protecting a high-risk zone.
     */
    ZONE_PRESERVING(1.60, "Zone-Preserving Bid (+60% ETA modifier)"),

    /**
     * Pass / Defer: Declines to bid (unless no other unit is eligible).
     */
    PASS(Double.MAX_VALUE, "Pass/Defer (Decline to bid)");

    private final double bidMultiplier;
    private final String description;

    BiddingAction(double bidMultiplier, String description) {
        this.bidMultiplier = bidMultiplier;
        this.description = description;
    }

    public double getBidMultiplier() { return bidMultiplier; }
    public String getDescription() { return description; }

    public double computeModifiedBid(double rawEta) {
        if (this == PASS) {
            return Double.MAX_VALUE;
        }
        return rawEta * bidMultiplier;
    }
}
