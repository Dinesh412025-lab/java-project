package com.medgrid.advisor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SymptomAdvisor {

    private final List<Condition> knowledgeBase;
    private final Path logPath = Paths.get("data/output/symptom_checks_log.csv");
    private final com.medgrid.triage.ExplainableTriageClassifier mlClassifier;

    public SymptomAdvisor(List<Condition> knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
        
        // Initialize and train Explainable ML Triage Classifier
        com.medgrid.triage.SymptomVocabulary vocab = com.medgrid.triage.SymptomVocabulary.buildFromConditions(knowledgeBase);
        this.mlClassifier = new com.medgrid.triage.ExplainableTriageClassifier(knowledgeBase, vocab);
        com.medgrid.triage.TriageDataset dataset = new com.medgrid.triage.TriageDataset(knowledgeBase);
        this.mlClassifier.train(dataset, 40, 0.1, 0.001);

        try {
            if (!Files.exists(logPath.getParent())) {
                Files.createDirectories(logPath.getParent());
            }
            if (!Files.exists(logPath)) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(logPath.toFile()))) {
                    bw.write("timestamp,input_symptoms,condition_matched,specialist_recommended\n");
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize symptom_checks_log.csv: " + e.getMessage());
        }
    }

    public void checkSymptoms(String input) {
        System.out.println("\n---------------------------------------------------------");
        System.out.println("⚠ DISCLAIMER: This tool provides general guidance only and ");
        System.out.println("is not a substitute for professional medical advice, ");
        System.out.println("diagnosis, or treatment.");
        System.out.println("---------------------------------------------------------");

        String[] inputSymptoms = input.toLowerCase().split(",");
        for (int i = 0; i < inputSymptoms.length; i++) {
            inputSymptoms[i] = inputSymptoms[i].trim();
        }

        MatchResult bestMatch = null;
        for (Condition condition : knowledgeBase) {
            int matchCount = 0;
            for (String conditionSymptom : condition.getSymptoms()) {
                for (String userSymptom : inputSymptoms) {
                    if (userSymptom.contains(conditionSymptom.trim().toLowerCase()) || 
                        conditionSymptom.trim().toLowerCase().contains(userSymptom)) {
                        matchCount++;
                    }
                }
            }
            if (matchCount > 0 && (bestMatch == null || matchCount > bestMatch.getMatchedSymptomCount())) {
                bestMatch = new MatchResult(condition, matchCount);
            }
        }

        if (bestMatch == null) {
            System.out.println("\nCould not match symptoms to a known condition.");
            System.out.println("Recommendation: Consult a GENERAL physician.");
            logCheck(input, "Unknown", "GENERAL");
            return;
        }

        Condition condition = bestMatch.getCondition();
        
        System.out.println("\nPossible Condition Match: " + condition.getName());

        if ("emergency".equalsIgnoreCase(condition.getUrgencyLevel())) {
            System.out.println("\n⚠ EMERGENCY: Seek immediate medical attention or call emergency services. Do not wait for an appointment.");
        } else {
            System.out.println("Urgency Level: " + condition.getUrgencyLevel().toUpperCase());
            if (condition.getOtcGuidance() != null && !condition.getOtcGuidance().isEmpty()) {
                System.out.println("OTC Guidance: " + condition.getOtcGuidance());
            }
            System.out.println("Recommended Specialist: " + condition.getSpecialistRecommended());
        }

        logCheck(input, condition.getName(), condition.getSpecialistRecommended());
    }

    public com.medgrid.triage.TriagePrediction triageWithAI(String symptomsInput) {
        return mlClassifier.predict(symptomsInput);
    }

    public com.medgrid.triage.ExplainableTriageClassifier getMlClassifier() {
        return mlClassifier;
    }

    private void logCheck(String symptoms, String condition, String specialist) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(logPath.toFile(), true))) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            // Escape quotes if needed
            String escapedSymptoms = "\"" + symptoms.replace("\"", "\"\"") + "\"";
            bw.write(String.format("%s,%s,%s,%s\n", timestamp, escapedSymptoms, condition, specialist));
        } catch (IOException e) {
            System.err.println("Failed to write to symptom_checks_log.csv: " + e.getMessage());
        }
    }
}
