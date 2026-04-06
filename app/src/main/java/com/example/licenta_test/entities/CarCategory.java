package com.example.licenta_test.entities;

public class CarCategory {
    private String name;
    private int imageResId;

    public CarCategory(String name, int imageResId) {
        this.name = name;
        this.imageResId = imageResId;
    }

    public String getName() {
        return name;
    }

    public int getImageResId() {
        return imageResId;
    }
}
