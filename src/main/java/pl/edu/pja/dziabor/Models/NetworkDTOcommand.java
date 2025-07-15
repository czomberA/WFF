package pl.edu.pja.dziabor.Models;

import java.time.LocalDate;

public class NetworkDTOcommand {
    private String ssid;

    private String bssid;
    private int signal;

    public NetworkDTOcommand(String ssid, String bssid, int signal) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.signal = signal;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }


    public int getSignal() {
        return signal;
    }

    public void setSignal(int signal) {
        this.signal = signal;
    }

    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    @Override
    public String toString() {
        return "NetworkDTOcommand{" +
                "ssid='" + ssid + '\'' +
                ", bssid='" + bssid + '\'' +
                ", signal=" + signal +
                '}';
    }
}
