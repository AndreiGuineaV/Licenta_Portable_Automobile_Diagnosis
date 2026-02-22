package com.example.licenta_test;

import com.google.firebase.firestore.DocumentId;

import java.util.List;

public class Martor {
    @DocumentId
    private String id;
    private String nume;
    private List<String> motiveAparitie; //un martor se poate aprinde din mai multe motive
    private String alteDetalii;
    private String urlImagine;

    public Martor(){}

    public Martor(String nume, List<String> motiveAparitie, String alteDetalii, String urlImagine) {
        this.nume = nume;
        this.motiveAparitie = motiveAparitie;
        this.alteDetalii = alteDetalii;
        this.urlImagine = urlImagine;
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

    public List<String> getMotiveAparitie() {
        return motiveAparitie;
    }

    public void setMotiveAparitie(List<String> motiveAparitie) {
        this.motiveAparitie = motiveAparitie;
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
}
