package com.medgrid.advisor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KnowledgeBaseLoader {

    public static List<Condition> loadConditions(Path path) throws IOException {
        List<Condition> conditions = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
            String line = br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length >= 5) {
                    String name = parts[0].trim();
                    List<String> symptoms = Arrays.asList(parts[1].split(";"));
                    String urgencyLevel = parts[2].trim().toLowerCase();
                    String otcGuidance = parts[3].trim();
                    String specialistRecommended = parts[4].trim();
                    
                    conditions.add(new Condition(name, symptoms, urgencyLevel, otcGuidance, specialistRecommended));
                }
            }
        }
        return conditions;
    }
}
