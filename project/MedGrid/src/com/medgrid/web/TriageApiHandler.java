package com.medgrid.web;

import com.medgrid.advisor.Condition;
import com.medgrid.advisor.KnowledgeBaseLoader;
import com.medgrid.triage.ExplainableTriageClassifier;
import com.medgrid.triage.FeatureAttribution;
import com.medgrid.triage.SymptomVocabulary;
import com.medgrid.triage.TriageDataset;
import com.medgrid.triage.TriagePrediction;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

/**
 * REST Endpoint for Explainable Clinical Triage inference with feature contribution vectors.
 */
public class TriageApiHandler implements HttpHandler {
    private final ExplainableTriageClassifier classifier;

    public TriageApiHandler() {
        ExplainableTriageClassifier tc = null;
        try {
            List<Condition> conditions = KnowledgeBaseLoader.loadConditions(Paths.get("data/input/conditions.csv"));
            SymptomVocabulary vocab = SymptomVocabulary.buildFromConditions(conditions);
            tc = new ExplainableTriageClassifier(conditions, vocab);
            TriageDataset dataset = new TriageDataset(conditions);
            tc.train(dataset, 40, 0.1, 0.001);
        } catch (Exception e) {
            System.err.println("Error initializing TriageApiHandler classifier: " + e.getMessage());
        }
        this.classifier = tc;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String symptomsInput = "";

        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("symptoms=")) {
                String raw = query.substring(query.indexOf("symptoms=") + 9);
                if (raw.contains("&")) raw = raw.substring(0, raw.indexOf("&"));
                symptomsInput = URLDecoder.decode(raw, StandardCharsets.UTF_8);
            }
        } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
                String raw = body.toString();
                if (raw.contains("symptoms=")) {
                    symptomsInput = URLDecoder.decode(raw.substring(raw.indexOf("symptoms=") + 9), StandardCharsets.UTF_8);
                } else {
                    symptomsInput = raw.replace("{\"symptoms\":\"", "").replace("\"}", "").replace("{", "").replace("}", "").trim();
                }
            }
        }

        if (symptomsInput.isEmpty()) {
            symptomsInput = "chest pain, shortness of breath, sweating";
        }

        TriagePrediction pred = classifier.predict(symptomsInput);
        String json = buildPredictionJson(pred);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String buildPredictionJson(TriagePrediction p) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(String.format("\"condition\": \"%s\",", p.getConditionName()));
        sb.append(String.format("\"urgency\": \"%s\",", p.getUrgencyLevel().name()));
        sb.append(String.format("\"specialist\": \"%s\",", p.getRecommendedSpecialist().name()));
        sb.append(String.format("\"confidence\": %.1f,", p.getConfidence() * 100.0));
        sb.append(String.format("\"guidance\": \"%s\",", p.getClinicalGuidance().replace("\"", "\\\"")));
        sb.append("\"attributions\": [");
        boolean first = true;
        for (FeatureAttribution fa : p.getTopContributingFeatures()) {
            if (!first) sb.append(",");
            sb.append("{");
            sb.append(String.format("\"feature\": \"%s\",", fa.getFeatureName()));
            sb.append(String.format("\"weight\": %.3f,", fa.getWeight()));
            sb.append(String.format("\"contributionScore\": %.3f", fa.getAttributionScore()));
            sb.append("}");
            first = false;
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }
}
