package com.medgrid.triage;

import com.medgrid.advisor.Condition;

import java.util.*;

/**
 * Clinical Symptom Vocabulary & Vectorizer for feature encoding.
 */
public class SymptomVocabulary {
    private final List<String> vocabularyList;
    private final Map<String, Integer> termToIndex;

    public SymptomVocabulary(List<String> terms) {
        this.vocabularyList = new ArrayList<>();
        this.termToIndex = new HashMap<>();
        if (terms != null) {
            for (String term : terms) {
                String clean = normalizeTerm(term);
                if (!clean.isEmpty() && !termToIndex.containsKey(clean)) {
                    termToIndex.put(clean, vocabularyList.size());
                    vocabularyList.add(clean);
                }
            }
        }
    }

    public static SymptomVocabulary buildFromConditions(List<Condition> conditions) {
        Set<String> uniqueTerms = new LinkedHashSet<>();
        if (conditions != null) {
            for (Condition cond : conditions) {
                if (cond.getSymptoms() != null) {
                    for (String s : cond.getSymptoms()) {
                        String clean = normalizeTerm(s);
                        if (!clean.isEmpty()) {
                            uniqueTerms.add(clean);
                        }
                    }
                }
            }
        }
        return new SymptomVocabulary(new ArrayList<>(uniqueTerms));
    }

    public static String normalizeTerm(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ");
    }

    /**
     * Vectorizes a comma-separated or raw text input into binary feature vector x in {0, 1}^M.
     */
    public double[] vectorize(String input) {
        double[] vector = new double[vocabularyList.size()];
        if (input == null || input.isEmpty()) return vector;

        String normalizedInput = normalizeTerm(input);
        String[] tokens = normalizedInput.split(",");

        for (int i = 0; i < vocabularyList.size(); i++) {
            String term = vocabularyList.get(i);
            // Check direct token match or substring match
            for (String token : tokens) {
                String cleanToken = normalizeTerm(token);
                if (cleanToken.contains(term) || term.contains(cleanToken)) {
                    vector[i] = 1.0;
                    break;
                }
            }
            if (vector[i] == 0.0 && normalizedInput.contains(term)) {
                vector[i] = 1.0;
            }
        }
        return vector;
    }

    public int size() { return vocabularyList.size(); }
    public String getFeatureName(int index) { return vocabularyList.get(index); }
    public List<String> getVocabularyList() { return Collections.unmodifiableList(vocabularyList); }
}
