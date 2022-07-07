package model;

public class SideEffect {

    private String idMedic;
    private String sideEffect1;
    private String sideEffect2;
    private String sourceSoignant;

    public SideEffect(String idMedic, String sideEffect1, String sideEffect2, String sourceSoignant) {
        this.idMedic = idMedic;
        this.sideEffect1 = sideEffect1;
        this.sideEffect2 = sideEffect2;
        this.sourceSoignant = sourceSoignant;
    }

    public String getIdMedic() {
        return idMedic;
    }

    public void setIdMedic(String idMedic) {
        this.idMedic = idMedic;
    }

    public String getSideEffect1() {
        return sideEffect1;
    }

    public void setSideEffect1(String sideEffect1) {
        this.sideEffect1 = sideEffect1;
    }

    public String getSideEffect2() {
        return sideEffect2;
    }

    public void setSideEffect2(String sideEffect2) {
        this.sideEffect2 = sideEffect2;
    }

    public String getSourceSoignant() {
        return sourceSoignant;
    }

    public void setSourceSoignant(String sourceSoignant) {
        this.sourceSoignant = sourceSoignant;
    }
}
