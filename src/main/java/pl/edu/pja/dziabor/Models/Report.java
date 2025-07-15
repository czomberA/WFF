package pl.edu.pja.dziabor.Models;

import pl.edu.pja.dziabor.database.LocalDatabase;

import java.time.LocalDate;

public class Report {
    String bssid;
    LocalDate date;
    String reason;
    public Report(String bssid, LocalDate date, String reason) {
        this.bssid = bssid;
        this.date = date;
        this.reason = reason;
    }

    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
