package pl.edu.pja.dziabor.Models;

import java.time.LocalDate;

public class Network {
    private String ssid;
    private String password;
    private String bssid;
    private int signal;
    private LocalDate lastSuccessfulConnection;

    public Network() {
    }

    public Network(String ssid, String bssid, String password, LocalDate lastSuccessfullConnection) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.password = password;
        this.lastSuccessfulConnection = lastSuccessfullConnection;
    }


    public Network(NetworkDTOcommand networkDTOcommand) {
        this.ssid = networkDTOcommand.getSsid();
        this.bssid = networkDTOcommand.getBssid();
        this.password = null;
        this.signal = networkDTOcommand.getSignal();
        this.lastSuccessfulConnection = LocalDate.MIN;
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

    public int getSignal() {
        return signal;
    }

    public void setSignal(int signal) {
        this.signal = signal;
    }

    @Override
    public String toString() {
        return "WifiConnection{" +
                "ssid='" + ssid + '\'' +
                ", password='" + password + '\'' +
                ", bssid='" + bssid + '\'' +
                ", signal=" + signal +
                ", lastSuccessfullConnection=" + lastSuccessfulConnection +
                '}';
    }



    public LocalDate getLastSuccessfulConnection() {
        return lastSuccessfulConnection;
    }

    public void setLastSuccessfulConnection(LocalDate lastSuccessfulConnection) {
        this.lastSuccessfulConnection = lastSuccessfulConnection;
    }


}
