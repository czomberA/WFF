package pl.edu.pja.dziabor;

import pl.edu.pja.dziabor.Models.Profile;
import pl.edu.pja.dziabor.Networking.Networking;
import pl.edu.pja.dziabor.Networking.NetworkingWin;
import pl.edu.pja.dziabor.Profiles.ProfileManager;
import pl.edu.pja.dziabor.Profiles.ProfileManagerWin;

import java.util.HashMap;

public final class Device {
    private Networking networking;
    private static final Device instance = new Device();
    private ProfileManager profileManager;

    private Device() {
        setOs();
    }
    public static Device getInstance() {
        return instance;
    }
    public void setOs(){
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains(windows)) {
            this.networking = new NetworkingWin();
            this.profileManager = new ProfileManagerWin();
//        } else if (os.contains(linuxNix) || os.contains(linuxNux)) {
//            networking = new NetworkingLinux();
//        } else if (os.contains(mac)) {
//            networking = new NetworkingMac();
        } else {
            networking = new NetworkingWin();
            System.out.println("Unsupported OS.");
        }
    }

    public  Networking getNetworking() {
        return networking;
    }
    public ProfileManager getProfileManager() {
        return profileManager;
    }


    private final String windows = "win";
    private final String mac = "mac";
    private final String linuxNix = "nix";
    private final String linuxNux = "nux";

}
