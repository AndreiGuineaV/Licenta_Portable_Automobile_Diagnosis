package com.example.licenta_test.entities;

public enum FuelType {
    DIESEL("diesel"),
    PETROL("petrol"),
    HYBRID("hybrid"),
    ELECTRIC("electric");

    private final String fuelType;

    FuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getFuelType() {
        return fuelType;
    }

}