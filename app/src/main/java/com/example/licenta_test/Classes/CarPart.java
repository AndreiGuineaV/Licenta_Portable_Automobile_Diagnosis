package com.example.licenta_test.Classes;

import com.google.firebase.firestore.DocumentId;

import java.util.List;

//CAR PARTS CLASS
public class CarPart {
    @DocumentId //The object id will be given by the id of the Firestore document
    private String id;
    private String name;
    private String functionalities;
    private List<String> malfunctionSymptoms; //a defective part can have multiple symptoms
    private String otherDetails;
    private String urlImage; //url for the part image
    private List<String> carsCompatibileIds; //the ids of the cars that are compatible with the part

    public CarPart() {} //Firestore needs a no parameters constructor

    public CarPart(String name, String functionalities, List<String> malfunctionSymptoms, String otherDetails, String urlImage, List<String> carsCompatibileIds) {
        this.name = name;
        this.functionalities = functionalities;
        this.malfunctionSymptoms = malfunctionSymptoms;
        this.otherDetails = otherDetails;
        this.urlImage = urlImage;
        this.carsCompatibileIds = carsCompatibileIds;
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

    public String getFunctionalities() {
        return functionalities;
    }

    public void setFunctionalities(String functionalities) {
        this.functionalities = functionalities;
    }

    public List<String> getMalfunctionSymptoms() {
        return malfunctionSymptoms;
    }

    public void setMalfunctionSymptoms(List<String> malfunctionSymptoms) {
        this.malfunctionSymptoms = malfunctionSymptoms;
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

    public List<String> getCarsCompatibileIds() {
        return carsCompatibileIds;
    }

    public void setCarsCompatibileIds(List<String> carsCompatibileIds) {
        this.carsCompatibileIds = carsCompatibileIds;
    }
}


