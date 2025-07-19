package pl.edu.pja.dziabor.Models;

public class Profile {
    String SSID;
    String password;
    public Profile(String SSID, String password) {
        this.SSID = SSID;
        this.password = password;
    }

    public Profile() {}

    public String getSSID() {
        return SSID;
    }

    public void setSSID(String SSID) {
        this.SSID = SSID;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "Profile{" +
                "SSID='" + SSID + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

    public boolean isValid() {
        if (SSID == null || password == null) {
            return false;
        }

        return !SSID.isEmpty() && !password.isEmpty();
    }
}
