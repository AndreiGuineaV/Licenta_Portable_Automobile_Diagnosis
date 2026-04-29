package com.example.licenta_test.entities;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;

public class Car implements Serializable {
    @DocumentId
    private String id;
    private String carName;
    private int km;
    private String fuel;
    private float engine;
    private int power; //Horsepower
    private int year;
    private String imgPath;
    private long itpExpiration = 0;
    private long rcaExpiration = 0;
    private long rovinietaExpiration = 0;
    private long oilChangeDate = 0;

    public Car(){} //Necessary constructor for Firestore

    public Car(String carName, int km, String fuel, float engine, int power, int year, String imgPath) {
        this.carName = carName;
        this.km = km;
        this.fuel = fuel;
        this.engine = engine;
        this.power = power;
        this.year = year;
        this.imgPath = imgPath;
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

    public String getImgPath() {
        return imgPath;
    }

    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }
    public long getItpExpiration() { return itpExpiration; }
    public void setItpExpiration(long itpExpiration) { this.itpExpiration = itpExpiration; }

    public long getRcaExpiration() { return rcaExpiration; }
    public void setRcaExpiration(long rcaExpiration) { this.rcaExpiration = rcaExpiration; }

    public long getRovinietaExpiration() { return rovinietaExpiration; }
    public void setRovinietaExpiration(long rovinietaExpiration) { this.rovinietaExpiration = rovinietaExpiration; }

    public long getOilChangeDate() { return oilChangeDate; }
    public void setOilChangeDate(long oilChangeDate) { this.oilChangeDate = oilChangeDate; }
}
