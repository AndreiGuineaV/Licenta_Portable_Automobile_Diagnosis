package com.example.licenta_test.entities;

import com.google.firebase.firestore.DocumentId;

import java.util.Date;
import java.util.List;
//CLASS MADE TO STORE THE USER REQUESTED DIAGNOSTICS
public class DiagnosisHistory {
    @DocumentId
    private String id;
    private String diagName;
    private String causes;
    private String solutions;
    private String preventions;
    private Date diagnosticDate;

    private String userId;
    private String carId;
    private List<String> recommendedPartsIds; //The car parts that need to be changed or fixed
    private List<String> relatedWarningsIds; //Turned on warning lights

    public DiagnosisHistory(){}



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDiagName() {
        return diagName;
    }

    public void setDiagName(String diagName) {
        this.diagName = diagName;
    }

    public String getCauses() {
        return causes;
    }

    public void setCauses(String causes) {
        this.causes = causes;
    }

    public String getSolutions() {
        return solutions;
    }

    public void setSolutions(String solutions) {
        this.solutions = solutions;
    }

    public String getPreventions() {
        return preventions;
    }

    public void setPreventions(String preventions) {
        this.preventions = preventions;
    }

    public Date getDiagnosticDate() {
        return diagnosticDate;
    }

    public void setDiagnosticDate(Date diagnosticDate) {
        this.diagnosticDate = diagnosticDate;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public List<String> getRecommendedPartsIds() {
        return recommendedPartsIds;
    }

    public void setRecommendedPartsIds(List<String> recommendedPartsIds) {
        this.recommendedPartsIds = recommendedPartsIds;
    }

    public List<String> getRelatedWarningsIds() {
        return relatedWarningsIds;
    }

    public void setRelatedWarningsIds(List<String> relatedWarningsIds) {
        this.relatedWarningsIds = relatedWarningsIds;
    }
}
