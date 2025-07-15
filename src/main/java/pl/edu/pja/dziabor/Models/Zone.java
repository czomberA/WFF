package pl.edu.pja.dziabor.Models;

public class Zone {
    private String code;
    public Zone(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }
}
