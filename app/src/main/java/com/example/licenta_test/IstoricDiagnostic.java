package com.example.licenta_test;

import com.google.firebase.firestore.DocumentId;

import java.util.Date;
import java.util.List;

public class IstoricDiagnostic {
    @DocumentId
    private String id;
    private String numeDiagnostic;
    private String cauze;
    private String solutii;
    private String preventii;
    private Date dataDiagnostic;

    private String idUtilizator;
    private String idMasina;
    private List<String> idsPieseRecomandate; //piesele care produc defectiunea si necesita schimbate sau reparate
    private List<String> idsMartoriAsociati; //martorii aprinsi

    public IstoricDiagnostic(){}

    public IstoricDiagnostic(String numeDiagnostic, String cauze, String solutii, String preventii, Date dataDiagnostic, String idUtilizator, String idMasina, List<String> idsPieseRecomandate, List<String> idsMartoriAsociati) {
        this.numeDiagnostic = numeDiagnostic;
        this.cauze = cauze;
        this.solutii = solutii;
        this.preventii = preventii;
        this.dataDiagnostic = dataDiagnostic;
        this.idUtilizator = idUtilizator;
        this.idMasina = idMasina;
        this.idsPieseRecomandate = idsPieseRecomandate;
        this.idsMartoriAsociati = idsMartoriAsociati;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumeDiagnostic() {
        return numeDiagnostic;
    }

    public void setNumeDiagnostic(String numeDiagnostic) {
        this.numeDiagnostic = numeDiagnostic;
    }

    public String getCauze() {
        return cauze;
    }

    public void setCauze(String cauze) {
        this.cauze = cauze;
    }

    public String getSolutii() {
        return solutii;
    }

    public void setSolutii(String solutii) {
        this.solutii = solutii;
    }

    public String getPreventii() {
        return preventii;
    }

    public void setPreventii(String preventii) {
        this.preventii = preventii;
    }

    public Date getDataDiagnostic() {
        return dataDiagnostic;
    }

    public void setDataDiagnostic(Date dataDiagnostic) {
        this.dataDiagnostic = dataDiagnostic;
    }

    public String getIdUtilizator() {
        return idUtilizator;
    }

    public void setIdUtilizator(String idUtilizator) {
        this.idUtilizator = idUtilizator;
    }

    public String getIdMasina() {
        return idMasina;
    }

    public void setIdMasina(String idMasina) {
        this.idMasina = idMasina;
    }

    public List<String> getIdsPieseRecomandate() {
        return idsPieseRecomandate;
    }

    public void setIdsPieseRecomandate(List<String> idsPieseRecomandate) {
        this.idsPieseRecomandate = idsPieseRecomandate;
    }

    public List<String> getIdsMartoriAsociati() {
        return idsMartoriAsociati;
    }

    public void setIdsMartoriAsociati(List<String> idsMartoriAsociati) {
        this.idsMartoriAsociati = idsMartoriAsociati;
    }
}
