package com.example.licenta_test.Classes;

import com.google.firebase.firestore.DocumentId;

public class Car {
    @DocumentId
    private String id;
    private String Brand;
    private String model;
    private String fuel;
    private float engine;
    private int an;

    public Car(){} //Necessary constructor for Firestore

    public Car(String brand, String model, String fuel, float engine, int an) {
        Brand = brand;
        this.model = model;
        this.fuel = fuel;
        this.engine = engine;
        this.an = an;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBrand() {
        return Brand;
    }

    public void setBrand(String brand) {
        Brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getFuel() {
        return fuel;
    }

    public void setFuel(String fuel) {
        this.fuel = fuel;
    }

    public float getEngine() {
        return engine;
    }

    public void setEngine(float engine) {
        this.engine = engine;
    }

    public int getAn() {
        return an;
    }

    public void setAn(int an) {
        this.an = an;
    }
}
