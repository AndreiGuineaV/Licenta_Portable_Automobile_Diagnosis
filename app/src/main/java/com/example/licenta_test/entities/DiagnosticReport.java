package com.example.licenta_test.entities;

import com.google.firebase.firestore.DocumentId;

public class DiagnosticReport {
    @DocumentId
    private String id;

    private String carName;
    private String userSymptoms;
    private String aiDiagnosis;
    private long timestamp;

    public DiagnosticReport() {}

    public DiagnosticReport(String carName, String userSymptoms, String aiDiagnosis, long timestamp) {
        this.carName = carName;
        this.userSymptoms = userSymptoms;
        this.aiDiagnosis = aiDiagnosis;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCarName() {
        return carName;
    }

    public void setCarName(String carName) {
        this.carName = carName;
    }

    public String getUserSymptoms() {
        return userSymptoms;
    }

    public void setUserSymptoms(String userSymptoms) {
        this.userSymptoms = userSymptoms;
    }

    public String getAiDiagnosis() {
        return aiDiagnosis;
    }

    public void setAiDiagnosis(String aiDiagnosis) {
        this.aiDiagnosis = aiDiagnosis;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}