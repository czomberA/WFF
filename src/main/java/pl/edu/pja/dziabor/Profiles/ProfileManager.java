package pl.edu.pja.dziabor.Profiles;

import pl.edu.pja.dziabor.Models.Network;
import pl.edu.pja.dziabor.Models.Profile;

import java.util.HashMap;

public interface ProfileManager {
    Profile CreateWifiProfile(Network network);
    boolean checkIfProfileExists(Network network, HashMap<String, Profile> profiles);
    void dropProfile(Network network);
}
