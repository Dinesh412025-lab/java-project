package com.medgrid.triage;

import com.medgrid.advisor.Condition;
import com.medgrid.advisor.KnowledgeBaseLoader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Triage dataset loader and synthesizer for training and evaluating triage classifiers.
 */
public class TriageDataset {

    public static class Example {
        public final double[] features;
        public final String conditionName;
        public final String urgencyLevel;
        public final String specialist;
        public final List<String> rawSymptoms;

        public Example(double[] features, String conditionName, String urgencyLevel, String specialist, List<String> rawSymptoms) {
            this.features = features;
            this.conditionName = conditionName;
            this.urgencyLevel = urgencyLevel;
            this.specialist = specialist;
            this.rawSymptoms = rawSymptoms;
        }
    }

    private final List<Condition> conditions;
    private final SymptomVocabulary vocabulary;
    private final List<Example> examples;

    public TriageDataset(List<Condition> conditions) {
        this.conditions = conditions != null ? conditions : Collections.emptyList();
        this.vocabulary = SymptomVocabulary.buildFromConditions(this.conditions);
        this.examples = new ArrayList<>();
        generateExamples();
    }

    public static TriageDataset loadDefault() {
        Path path = Paths.get("data/input/conditions.csv");
        try {
            List<Condition> conds = KnowledgeBaseLoader.loadConditions(path);
            return new TriageDataset(conds);
        } catch (Exception e) {
            System.err.println("Warning: could not load default conditions: " + e.getMessage());
            return new TriageDataset(Collections.emptyList());
        }
    }

    private void generateExamples() {
        Random rnd = new Random(42);

        for (Condition cond : conditions) {
            List<String> symptoms = cond.getSymptoms();
            if (symptoms == null || symptoms.isEmpty()) continue;

            // 1. Full symptom vector
            String fullJoined = String.join(", ", symptoms);
            examples.add(new Example(vocabulary.vectorize(fullJoined), cond.getName(), cond.getUrgencyLevel(), cond.getSpecialistRecommended(), symptoms));

            // 2. Subsets (drop 1 or 2 symptoms to simulate partial clinical reporting)
            if (symptoms.size() > 1) {
                for (int i = 0; i < symptoms.size(); i++) {
                    List<String> subset = new ArrayList<>(symptoms);
                    subset.remove(i);
                    String subsetJoined = String.join(", ", subset);
                    examples.add(new Example(vocabulary.vectorize(subsetJoined), cond.getName(), cond.getUrgencyLevel(), cond.getSpecialistRecommended(), subset));
                }
            }

            // 3. Pair combinations if 3+ symptoms
            if (symptoms.size() >= 3) {
                for (int rep = 0; rep < 3; rep++) {
                    int idx1 = rnd.nextInt(symptoms.size());
                    int idx2 = rnd.nextInt(symptoms.size());
                    if (idx1 != idx2) {
                        String pair = symptoms.get(idx1) + ", " + symptoms.get(idx2);
                        examples.add(new Example(vocabulary.vectorize(pair), cond.getName(), cond.getUrgencyLevel(), cond.getSpecialistRecommended(), List.of(symptoms.get(idx1), symptoms.get(idx2))));
                    }
                }
            }
        }
    }

    public List<Condition> getConditions() { return conditions; }
    public SymptomVocabulary getVocabulary() { return vocabulary; }
    public List<Example> getExamples() { return examples; }
    public int size() { return examples.size(); }
}
