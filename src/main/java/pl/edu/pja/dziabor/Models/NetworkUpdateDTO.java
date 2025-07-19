package pl.edu.pja.dziabor.Models;

import java.time.LocalDate;

public class NetworkUpdateDTO {
    private String ssid;
    private String password;
    private String bssid;
    private String classification;
    boolean failed;
    private LocalDate lastSuccessfulConnection;

    public NetworkUpdateDTO(String ssid, String bssid, String password, String classification, LocalDate lastUse, boolean failed) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.password = password;
        this.classification = classification;
        this.lastSuccessfulConnection = lastUse;
        this.failed = failed;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public LocalDate getLastSuccessfulConnection() {
        return lastSuccessfulConnection;
    }

    public void setLastSuccessfulConnection(LocalDate lastSuccessfulConnection) {
        this.lastSuccessfulConnection = lastSuccessfulConnection;
    }

    public String getSSID() {
        return ssid;
    }
}
