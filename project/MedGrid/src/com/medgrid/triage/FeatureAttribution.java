package com.medgrid.triage;

/**
 * Represents a single feature's local attribution score explaining a clinical prediction.
 * Attribution phi_j = weight_{c, j} * x_j.
 */
public class FeatureAttribution implements Comparable<FeatureAttribution> {
    private final String featureName;
    private final double featureValue;
    private final double weight;
    private final double attributionScore;

    public FeatureAttribution(String featureName, double featureValue, double weight, double attributionScore) {
        this.featureName = featureName;
        this.featureValue = featureValue;
        this.weight = weight;
        this.attributionScore = attributionScore;
    }

    public String getFeatureName() { return featureName; }
    public double getFeatureValue() { return featureValue; }
    public double getWeight() { return weight; }
    public double getAttributionScore() { return attributionScore; }

    @Override
    public int compareTo(FeatureAttribution other) {
        // Descending order of absolute contribution
        return Double.compare(Math.abs(other.attributionScore), Math.abs(this.attributionScore));
    }

    @Override
    public String toString() {
        return String.format("%s (weight=%.2f, score=%+.2f)", featureName, weight, attributionScore);
    }
}
