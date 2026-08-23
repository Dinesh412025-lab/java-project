package com.medgrid.triage;

import com.medgrid.model.CaseSeverity;
import com.medgrid.model.CaseType;

import java.util.Collections;
import java.util.List;

/**
 * Result of clinical triage classification with comprehensive explainability features.
 */
public class TriagePrediction {
    private final String conditionName;
    private final CaseSeverity urgencyLevel;
    private final CaseType recommendedSpecialist;
    private final double confidence;
    private final List<FeatureAttribution> topContributingFeatures;
    private final String clinicalGuidance;

    public TriagePrediction(String conditionName,
                            CaseSeverity urgencyLevel,
                            CaseType recommendedSpecialist,
                            double confidence,
                            List<FeatureAttribution> topContributingFeatures,
                            String clinicalGuidance) {
        this.conditionName = conditionName != null ? conditionName : "Unknown Condition";
        this.urgencyLevel = urgencyLevel != null ? urgencyLevel : CaseSeverity.LOW;
        this.recommendedSpecialist = recommendedSpecialist != null ? recommendedSpecialist : CaseType.GENERAL;
        this.confidence = confidence;
        this.topContributingFeatures = topContributingFeatures != null ? topContributingFeatures : Collections.emptyList();
        this.clinicalGuidance = clinicalGuidance != null ? clinicalGuidance : "";
    }

    public String getConditionName() { return conditionName; }
    public CaseSeverity getUrgencyLevel() { return urgencyLevel; }
    public CaseType getRecommendedSpecialist() { return recommendedSpecialist; }
    public double getConfidence() { return confidence; }
    public List<FeatureAttribution> getTopContributingFeatures() { return topContributingFeatures; }
    public String getClinicalGuidance() { return clinicalGuidance; }

    public String getFormattedExplanation() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Triage: %s | Severity: %s | Specialist: %s | Confidence: %.1f%%\n",
                conditionName, urgencyLevel, recommendedSpecialist, confidence * 100.0));
        sb.append("Key Explaining Symptom Features (Local Attributions):\n");
        int count = 0;
        for (FeatureAttribution fa : topContributingFeatures) {
            if (count++ >= 4) break;
            sb.append(String.format("  • %-25s -> Contribution Score: %+.2f (weight=%.2f)\n",
                    fa.getFeatureName(), fa.getAttributionScore(), fa.getWeight()));
        }
        if (!clinicalGuidance.isEmpty()) {
            sb.append("Guidance: ").append(clinicalGuidance).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getFormattedExplanation();
    }
}
