package com.example.licenta_test.entities;

public enum FuelType {
    DIESEL("Diesel"),
    PETROL("Petrol"),
    HYBRID("Hybrid"),
    ELECTRIC("Electric");

    private final String fuelType;

    FuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getFuelType() {
        return fuelType;
    }

}