package com.example.licenta_test.entities;

import com.google.firebase.firestore.DocumentId;

import java.util.List;

//WARNING LIGHTS EXPLAINED IN A CLASS
public class WarningLight {
    @DocumentId
    private String id;
    private String name;
    private List<String> causes; //a warning sign turning on can have more causes
    private String otherDetails;
    private String urlImage;
    private List<String> symptoms;

    public WarningLight(){}

    public WarningLight(String name, List<String> causes, String otherDetails, String urlImage, List<String> symptoms) {
        this.name = name;
        this.causes = causes;
        this.otherDetails = otherDetails;
        this.urlImage = urlImage;
        this.symptoms = symptoms;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getCauses() {
        return causes;
    }

    public void setCauses(List<String> causes) {
        this.causes = causes;
    }

    public String getOtherDetails() {
        return otherDetails;
    }

    public void setOtherDetails(String otherDetails) {
        this.otherDetails = otherDetails;
    }

    public String getUrlImage() {
        return urlImage;
    }

    public void setUrlImage(String urlImage) {
        this.urlImage = urlImage;
    }
    public List<String> getSymptoms() {
        return symptoms;
    }
    public void setSymptoms(List<String> symptoms) {
        this.symptoms = symptoms;
    }
}
