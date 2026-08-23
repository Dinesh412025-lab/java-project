package com.medgrid.model;

import java.util.ArrayList;
import java.util.List;

public class PatientRecord {
    private final String patientId;
    private final String name;
    private final int age;
    private final String bloodType;
    private final List<String> clinicalHistory;
    private final List<String> clinicalManagementLog;
    private String currentVitals;

    public PatientRecord(String patientId, String name, int age, String bloodType) {
        this.patientId = patientId;
        this.name = name;
        this.age = age;
        this.bloodType = bloodType;
        this.clinicalHistory = new ArrayList<>();
        this.clinicalManagementLog = new ArrayList<>();
        this.currentVitals = "Stable";
    }

    public synchronized void addHistory(String history) {
        this.clinicalHistory.add(history);
    }

    public synchronized void updateManagement(String update) {
        this.clinicalManagementLog.add(update);
    }

    public synchronized void setVitals(String vitals) {
        this.currentVitals = vitals;
    }

    public String getPatientId() { return patientId; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getBloodType() { return bloodType; }
    public synchronized String getCurrentVitals() { return currentVitals; }
    public synchronized List<String> getClinicalManagementLog() { return new ArrayList<>(clinicalManagementLog); }
    
    @Override
    public String toString() {
        return String.format("Patient[%s, %s, %d, %s]", patientId, name, age, bloodType);
    }
}
