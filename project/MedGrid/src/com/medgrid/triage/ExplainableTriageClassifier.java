package com.medgrid.triage;

import com.medgrid.advisor.Condition;
import com.medgrid.model.CaseSeverity;
import com.medgrid.model.CaseType;

import java.util.*;

/**
 * Interpretable Multi-Class Softmax Logistic Classifier with exact Feature Attribution.
 * 100% JVM-native, transparent linear model providing local explanation scores.
 */
public class ExplainableTriageClassifier {
    private final SymptomVocabulary vocabulary;
    private final List<Condition> conditionClasses;
    private final Map<String, Integer> conditionToIndex;
    private final double[][] weights; // [numClasses][numFeatures]
    private final double[] biases;    // [numClasses]
    private boolean isTrained = false;

    public ExplainableTriageClassifier(List<Condition> conditionClasses, SymptomVocabulary vocabulary) {
        this.conditionClasses = new ArrayList<>(conditionClasses);
        this.vocabulary = vocabulary;
        this.conditionToIndex = new HashMap<>();
        for (int i = 0; i < this.conditionClasses.size(); i++) {
            conditionToIndex.put(this.conditionClasses.get(i).getName(), i);
        }

        int numClasses = this.conditionClasses.size();
        int numFeatures = this.vocabulary.size();
        this.weights = new double[numClasses][numFeatures];
        this.biases = new double[numClasses];
    }

    /**
     * Trains the classifier on the provided dataset using Stochastic Gradient Descent (SGD).
     */
    public void train(TriageDataset dataset, int epochs, double learningRate, double l2Reg) {
        List<TriageDataset.Example> examples = dataset.getExamples();
        if (examples.isEmpty()) return;

        int numClasses = conditionClasses.size();
        int numFeatures = vocabulary.size();
        Random rnd = new Random(42);

        for (int ep = 0; ep < epochs; ep++) {
            List<TriageDataset.Example> shuffled = new ArrayList<>(examples);
            Collections.shuffle(shuffled, rnd);

            for (TriageDataset.Example ex : shuffled) {
                Integer targetClassIdx = conditionToIndex.get(ex.conditionName);
                if (targetClassIdx == null) continue;

                // Compute logits z_k = w_k^T x + b_k
                double[] logits = new double[numClasses];
                double maxLogit = -Double.MAX_VALUE;
                for (int c = 0; c < numClasses; c++) {
                    double dot = biases[c];
                    for (int f = 0; f < numFeatures; f++) {
                        dot += weights[c][f] * ex.features[f];
                    }
                    logits[c] = dot;
                    if (dot > maxLogit) maxLogit = dot;
                }

                // Compute softmax probabilities
                double sumExp = 0.0;
                double[] probs = new double[numClasses];
                for (int c = 0; c < numClasses; c++) {
                    probs[c] = Math.exp(logits[c] - maxLogit);
                    sumExp += probs[c];
                }
                for (int c = 0; c < numClasses; c++) {
                    probs[c] /= sumExp;
                }

                // Gradient step: dL/dz_c = prob_c - 1(c == target)
                for (int c = 0; c < numClasses; c++) {
                    double error = probs[c] - (c == targetClassIdx ? 1.0 : 0.0);
                    biases[c] -= learningRate * error;
                    for (int f = 0; f < numFeatures; f++) {
                        if (ex.features[f] != 0.0) {
                            double grad = error * ex.features[f] + (l2Reg * weights[c][f]);
                            weights[c][f] -= learningRate * grad;
                        }
                    }
                }
            }
        }
        this.isTrained = true;
    }

    /**
     * Evaluates symptoms input and produces an Explainable Triage Prediction with Feature Attributions.
     */
    public TriagePrediction predict(String symptomsInput) {
        double[] x = vocabulary.vectorize(symptomsInput);
        int numClasses = conditionClasses.size();
        int numFeatures = vocabulary.size();

        double[] logits = new double[numClasses];
        double maxLogit = -Double.MAX_VALUE;
        for (int c = 0; c < numClasses; c++) {
            double dot = biases[c];
            for (int f = 0; f < numFeatures; f++) {
                dot += weights[c][f] * x[f];
            }
            logits[c] = dot;
            if (dot > maxLogit) maxLogit = dot;
        }

        double sumExp = 0.0;
        double[] probs = new double[numClasses];
        for (int c = 0; c < numClasses; c++) {
            probs[c] = Math.exp(logits[c] - maxLogit);
            sumExp += probs[c];
        }
        int bestClassIdx = 0;
        double bestProb = 0.0;
        for (int c = 0; c < numClasses; c++) {
            probs[c] /= sumExp;
            if (probs[c] > bestProb) {
                bestProb = probs[c];
                bestClassIdx = c;
            }
        }

        Condition predCondition = conditionClasses.get(bestClassIdx);

        // Compute local feature attributions: phi_j = weight_{c*, j} * x_j
        List<FeatureAttribution> attributions = new ArrayList<>();
        for (int f = 0; f < numFeatures; f++) {
            if (x[f] > 0) {
                double w = weights[bestClassIdx][f];
                double score = w * x[f];
                attributions.add(new FeatureAttribution(vocabulary.getFeatureName(f), x[f], w, score));
            }
        }
        Collections.sort(attributions);

        CaseSeverity severity = mapUrgencyToSeverity(predCondition.getUrgencyLevel());
        CaseType specialist = mapSpecialistToCaseType(predCondition.getSpecialistRecommended());

        return new TriagePrediction(
                predCondition.getName(),
                severity,
                specialist,
                bestProb,
                attributions,
                predCondition.getOtcGuidance()
        );
    }

    private CaseSeverity mapUrgencyToSeverity(String urgency) {
        if (urgency == null) return CaseSeverity.LOW;
        switch (urgency.toLowerCase()) {
            case "emergency": return CaseSeverity.CRITICAL;
            case "high": return CaseSeverity.HIGH;
            case "medium": return CaseSeverity.MEDIUM;
            default: return CaseSeverity.LOW;
        }
    }

    private CaseType mapSpecialistToCaseType(String specialist) {
        if (specialist == null) return CaseType.GENERAL;
        try {
            return CaseType.valueOf(specialist.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CaseType.GENERAL;
        }
    }

    public boolean isTrained() { return isTrained; }
    public SymptomVocabulary getVocabulary() { return vocabulary; }
    public List<Condition> getConditionClasses() { return conditionClasses; }
}
