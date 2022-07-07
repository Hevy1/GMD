package model;

public class Indication {

    private String medic;
    private String symptome;
    private String proba;
    private String sourceCause;

    public Indication(String medic, String symptome, String proba, String sourceCause) {
        this.medic = medic;
        this.symptome = symptome;
        this.proba = proba;
        this.sourceCause = sourceCause;
    }

    public String getMedic() {
        return medic;
    }

    public void setMedic(String medic) {
        this.medic = medic;
    }

    public String getSymptome() {
        return symptome;
    }

    public void setSymptome(String symptome) {
        this.symptome = symptome;
    }

    public String getProba() {
        return proba;
    }

    public void setProba(String proba) {
        this.proba = proba;
    }

    public String getSourceCause() {
        return sourceCause;
    }

    public void setSourceCause(String sourceCause) {
        this.sourceCause = sourceCause;
    }
}
