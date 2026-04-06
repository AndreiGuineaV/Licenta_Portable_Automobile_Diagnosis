package com.example.licenta_test.entities;

import com.google.firebase.firestore.DocumentId;

import java.util.List;

//CAR PARTS CLASS
public class CarPart {
    @DocumentId //The object id will be given by the id of the Firestore document
    private String id;
    private String name;
    private String category;
    private List<String> compatibleFuels;
    private List<String> malfunctionSymptoms; //a defective part can have multiple symptoms
    private String urlImage; //url for the part image
    public CarPart() {} //Firestore needs a no parameters constructor

    public CarPart(String name, String category, List<String> compatibleFuels, List<String> malfunctionSymptoms, String urlImage) {
        this.name = name;
        this.category = category;
        this.compatibleFuels = compatibleFuels;
        this.malfunctionSymptoms = malfunctionSymptoms;
        this.urlImage = urlImage;
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

    public List<String> getMalfunctionSymptoms() {
        return malfunctionSymptoms;
    }

    public void setMalfunctionSymptoms(List<String> malfunctionSymptoms) {
        this.malfunctionSymptoms = malfunctionSymptoms;
    }
    public String getUrlImage() {
        return urlImage;
    }

    public void setUrlImage(String urlImage) {
        this.urlImage = urlImage;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getCompatibleFuels() {
        return compatibleFuels;
    }

    public void setCompatibleFuels(List<String> compatibleFuels) {
        this.compatibleFuels = compatibleFuels;
    }
}


