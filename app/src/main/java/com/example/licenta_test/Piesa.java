package com.example.licenta_test;

import com.google.firebase.firestore.DocumentId;

import java.util.List;

//CLASA PENTRU PIESELE MASINILOR
public class Piesa {
    @DocumentId //id-ul obiectului va fi dat de id-ul din documentul Firestore
    private String id;
    private String nume;
    private String functionalitati;
    private List<String> simptomeDefectiune; //o piesa defecta poate manifesta mai multe simptome
    private String alteDetalii;
    private String urlImagine; //url pentru imaginea piesei
    private List<String> masiniCompatibileIds; //id-urile masinilor compatibile cu piesa respectiva

    public Piesa() {} //Firestore are nevoie de un constructor fara parametrii

    public Piesa(String nume, String functionalitati, List<String> simptomeDefectiune, String alteDetalii, String urlImagine, List<String> masiniCompatibileIds) {
        this.nume = nume;
        this.functionalitati = functionalitati;
        this.simptomeDefectiune = simptomeDefectiune;
        this.alteDetalii = alteDetalii;
        this.urlImagine = urlImagine;
        this.masiniCompatibileIds = masiniCompatibileIds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNume() {
        return nume;
    }

    public void setNume(String nume) {
        this.nume = nume;
    }

    public String getFunctionalitati() {
        return functionalitati;
    }

    public void setFunctionalitati(String functionalitati) {
        this.functionalitati = functionalitati;
    }

    public List<String> getSimptomeDefectiune() {
        return simptomeDefectiune;
    }

    public void setSimptomeDefectiune(List<String> simptomeDefectiune) {
        this.simptomeDefectiune = simptomeDefectiune;
    }

    public String getAlteDetalii() {
        return alteDetalii;
    }

    public void setAlteDetalii(String alteDetalii) {
        this.alteDetalii = alteDetalii;
    }

    public String getUrlImagine() {
        return urlImagine;
    }

    public void setUrlImagine(String urlImagine) {
        this.urlImagine = urlImagine;
    }

    public List<String> getMasiniCompatibileIds() {
        return masiniCompatibileIds;
    }

    public void setMasiniCompatibileIds(List<String> masiniCompatibileIds) {
        this.masiniCompatibileIds = masiniCompatibileIds;
    }
}


