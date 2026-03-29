package com.example.licenta_test.Classes;

import com.google.firebase.firestore.DocumentId;

public class Car {
    @DocumentId
    private String id;
    private String carName;
    private int km;
    private String fuel;
    private float engine;
    private int power; //Horsepower
    private int year;

    public Car(){} //Necessary constructor for Firestore

    public Car(String carName, int km, String fuel, float engine, int power, int year) {
        this.carName = carName;
        this.km = km;
        this.fuel = fuel;
        this.engine = engine;
        this.power = power;
        this.year = year;
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

    public int getKm() {
        return km;
    }

    public void setKm(int km) {
        this.km = km;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
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

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }
}
