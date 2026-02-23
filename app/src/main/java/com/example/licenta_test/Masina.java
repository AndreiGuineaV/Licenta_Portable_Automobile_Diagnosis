package com.example.licenta_test;

import com.google.firebase.firestore.DocumentId;

public class Masina {
    @DocumentId
    private String id;
    private String marca;
    private String model;
    private String combustibil;
    private int an;

    public Masina(){} //Constructor necesar pentru Firestore

    public Masina(String marca, String model, String combustibil, int an) {
        this.marca = marca;
        this.model = model;
        this.combustibil = combustibil;
        this.an = an;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getCombustibil() {
        return combustibil;
    }

    public void setCombustibil(String combustibil) {
        this.combustibil = combustibil;
    }

    public int getAn() {
        return an;
    }

    public void setAn(int an) {
        this.an = an;
    }
}
